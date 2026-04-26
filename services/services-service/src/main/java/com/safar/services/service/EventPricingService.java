package com.safar.services.service;

import com.safar.services.dto.CreateEventPricingDefaultRequest;
import com.safar.services.dto.EventPricingItemResponse;
import com.safar.services.dto.UpdateEventPricingRequest;
import com.safar.services.entity.ChefEventPricing;
import com.safar.services.entity.EventPricingDefault;
import com.safar.services.repository.ChefEventPricingRepository;
import com.safar.services.repository.EventPricingDefaultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPricingService {

    private final EventPricingDefaultRepository defaultRepo;
    private final ChefEventPricingRepository chefPricingRepo;

    // ── Public: get resolved pricing ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventPricingItemResponse> getPricing(UUID chefId) {
        List<EventPricingDefault> defaults = defaultRepo.findByActiveTrueOrderByCategoryAscSortOrderAsc();

        Map<String, ChefEventPricing> chefOverrides = Collections.emptyMap();
        if (chefId != null) {
            chefOverrides = chefPricingRepo.findByChefId(chefId).stream()
                    .collect(Collectors.toMap(ChefEventPricing::getItemKey, Function.identity()));
        }

        List<EventPricingItemResponse> result = new ArrayList<>();
        for (EventPricingDefault d : defaults) {
            ChefEventPricing override = chefOverrides.get(d.getItemKey());

            Long price = d.getDefaultPricePaise();
            boolean isCustom = false;
            boolean available = true;

            if (override != null) {
                price = clamp(override.getCustomPricePaise(), d.getMinPricePaise(), d.getMaxPricePaise());
                isCustom = true;
                available = override.getAvailable();
            }

            result.add(new EventPricingItemResponse(
                    d.getCategory(), d.getItemKey(), d.getLabel(), d.getDescription(),
                    d.getIcon(), price, d.getPriceType(),
                    d.getMinPricePaise(), d.getMaxPricePaise(),
                    d.getSortOrder(), isCustom, available
            ));
        }
        return result;
    }

    // ── Chef: manage custom pricing ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventPricingItemResponse> getChefPricing(UUID chefId) {
        return getPricing(chefId);
    }

    @Transactional
    public ChefEventPricing setChefPrice(UUID chefId, UpdateEventPricingRequest req) {
        EventPricingDefault def = defaultRepo.findByItemKey(req.itemKey())
                .orElseThrow(() -> new IllegalArgumentException("Unknown pricing item: " + req.itemKey()));

        Long price = clamp(req.pricePaise(), def.getMinPricePaise(), def.getMaxPricePaise());

        ChefEventPricing pricing = chefPricingRepo.findByChefIdAndItemKey(chefId, req.itemKey())
                .orElse(ChefEventPricing.builder()
                        .chefId(chefId)
                        .itemKey(req.itemKey())
                        .build());

        pricing.setCustomPricePaise(price);
        pricing.setAvailable(req.available() != null ? req.available() : true);
        log.info("Chef {} set price for {}: {} paise", chefId, req.itemKey(), price);
        return chefPricingRepo.save(pricing);
    }

    @Transactional
    public List<ChefEventPricing> setChefPricingBulk(UUID chefId, List<UpdateEventPricingRequest> items) {
        return items.stream()
                .map(req -> setChefPrice(chefId, req))
                .collect(Collectors.toList());
    }

    @Transactional
    public void resetChefPrice(UUID chefId, String itemKey) {
        chefPricingRepo.deleteByChefIdAndItemKey(chefId, itemKey);
        log.info("Chef {} reset price for {} to platform default", chefId, itemKey);
    }

    // ── Admin: CRUD defaults ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventPricingDefault> getAllDefaults() {
        return defaultRepo.findByActiveTrueOrderByCategoryAscSortOrderAsc();
    }

    @Transactional
    public EventPricingDefault createDefault(CreateEventPricingDefaultRequest req) {
        defaultRepo.findByItemKey(req.itemKey()).ifPresent(existing -> {
            throw new IllegalArgumentException("Pricing item already exists: " + req.itemKey());
        });

        EventPricingDefault item = EventPricingDefault.builder()
                .category(req.category())
                .itemKey(req.itemKey())
                .label(req.label())
                .description(req.description())
                .icon(req.icon())
                .defaultPricePaise(req.defaultPricePaise())
                .priceType(req.priceType())
                .minPricePaise(req.minPricePaise())
                .maxPricePaise(req.maxPricePaise())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .active(true)
                .build();

        log.info("Admin created pricing item: {}", req.itemKey());
        return defaultRepo.save(item);
    }

    @Transactional
    public EventPricingDefault updateDefault(String itemKey, CreateEventPricingDefaultRequest req) {
        EventPricingDefault item = defaultRepo.findByItemKey(itemKey)
                .orElseThrow(() -> new IllegalArgumentException("Pricing item not found: " + itemKey));

        if (req.label() != null) item.setLabel(req.label());
        if (req.description() != null) item.setDescription(req.description());
        if (req.icon() != null) item.setIcon(req.icon());
        if (req.defaultPricePaise() != null) item.setDefaultPricePaise(req.defaultPricePaise());
        if (req.priceType() != null) item.setPriceType(req.priceType());
        if (req.minPricePaise() != null) item.setMinPricePaise(req.minPricePaise());
        if (req.maxPricePaise() != null) item.setMaxPricePaise(req.maxPricePaise());
        if (req.sortOrder() != null) item.setSortOrder(req.sortOrder());

        log.info("Admin updated pricing item: {}", itemKey);
        return defaultRepo.save(item);
    }

    @Transactional
    public void deactivateDefault(String itemKey) {
        EventPricingDefault item = defaultRepo.findByItemKey(itemKey)
                .orElseThrow(() -> new IllegalArgumentException("Pricing item not found: " + itemKey));
        item.setActive(false);
        defaultRepo.save(item);
        log.info("Admin deactivated pricing item: {}", itemKey);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Long clamp(Long value, Long min, Long max) {
        if (value == null) return null;
        if (min != null && value < min) return min;
        if (max != null && value > max) return max;
        return value;
    }
}
