package com.safar.user.service;

import com.safar.user.dto.BucketListItemDto;
import com.safar.user.entity.BucketListItem;
import com.safar.user.repository.BucketListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BucketListService {

    private final BucketListRepository bucketListRepository;
    private final RestTemplate restTemplate;

    @Value("${services.listing.url}")
    private String listingServiceUrl;

    @Transactional
    public BucketListItemDto add(UUID guestId, UUID listingId, String notes) {
        if (bucketListRepository.existsByGuestIdAndListingId(guestId, listingId)) {
            throw new IllegalStateException(
                    "Listing " + listingId + " is already in your bucket list");
        }
        BucketListItem item = BucketListItem.builder()
                .guestId(guestId)
                .listingId(listingId)
                .notes(notes)
                .build();
        return toDto(bucketListRepository.save(item));
    }

    @Transactional
    public void remove(UUID guestId, UUID listingId) {
        bucketListRepository.deleteByGuestIdAndListingId(guestId, listingId);
    }

    public Page<BucketListItemDto> list(UUID guestId, Pageable pageable) {
        return bucketListRepository.findByGuestId(guestId, pageable).map(this::toDto);
    }

    private BucketListItemDto toDto(BucketListItem item) {
        String title = null;
        String city = null;
        String imageUrl = null;

        try {
            String url = listingServiceUrl + "/api/v1/listings/" + item.getListingId();
            @SuppressWarnings("unchecked")
            Map<String, Object> listing = restTemplate.getForObject(url, Map.class);
            if (listing != null) {
                title = (String) listing.get("title");
                city = (String) listing.get("city");
                imageUrl = (String) listing.get("primaryPhotoUrl");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch listing {} details: {}", item.getListingId(), e.getMessage());
        }

        return new BucketListItemDto(
                item.getId(),
                item.getGuestId(),
                item.getListingId(),
                item.getAddedAt(),
                item.getNotes(),
                title,
                city,
                imageUrl
        );
    }
}
