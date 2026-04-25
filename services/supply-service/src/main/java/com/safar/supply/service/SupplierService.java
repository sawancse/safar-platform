package com.safar.supply.service;

import com.safar.supply.dto.CatalogItemRequest;
import com.safar.supply.dto.SupplierRequest;
import com.safar.supply.entity.Supplier;
import com.safar.supply.entity.SupplierCatalogItem;
import com.safar.supply.repository.SupplierCatalogItemRepository;
import com.safar.supply.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService {

    private final SupplierRepository supplierRepo;
    private final SupplierCatalogItemRepository catalogRepo;

    // ── Suppliers ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Supplier> list(boolean activeOnly) {
        return activeOnly
                ? supplierRepo.findByActiveTrueOrderByBusinessNameAsc()
                : supplierRepo.findByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Supplier get(UUID id) {
        return supplierRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
    }

    @Transactional
    public Supplier create(SupplierRequest req) {
        if (req.businessName() == null || req.businessName().isBlank())
            throw new IllegalArgumentException("businessName required");
        if (req.phone() == null || req.phone().isBlank())
            throw new IllegalArgumentException("phone required");

        Supplier s = Supplier.builder()
                .businessName(req.businessName())
                .ownerName(req.ownerName())
                .phone(req.phone())
                .email(req.email())
                .whatsapp(req.whatsapp())
                .gst(req.gst())
                .pan(req.pan())
                .bankAccount(req.bankAccount())
                .bankIfsc(req.bankIfsc())
                .bankHolder(req.bankHolder())
                .address(req.address())
                .categories(toUpperArray(req.categories()))
                .serviceCities(toLowerArray(req.serviceCities()))
                .leadTimeDays(req.leadTimeDays() == null ? 2 : req.leadTimeDays())
                .paymentTerms(req.paymentTerms() == null ? "NET_15" : req.paymentTerms())
                .creditLimitPaise(req.creditLimitPaise() == null ? 0L : req.creditLimitPaise())
                .kycStatus(req.kycStatus() == null ? "PENDING" : req.kycStatus())
                .kycNotes(req.kycNotes())
                .notes(req.notes())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .build();
        return supplierRepo.save(s);
    }

    @Transactional
    public Supplier update(UUID id, SupplierRequest req) {
        Supplier s = get(id);
        if (req.businessName() != null)    s.setBusinessName(req.businessName());
        if (req.ownerName() != null)       s.setOwnerName(req.ownerName());
        if (req.phone() != null)           s.setPhone(req.phone());
        if (req.email() != null)           s.setEmail(req.email());
        if (req.whatsapp() != null)        s.setWhatsapp(req.whatsapp());
        if (req.gst() != null)             s.setGst(req.gst());
        if (req.pan() != null)             s.setPan(req.pan());
        if (req.bankAccount() != null)     s.setBankAccount(req.bankAccount());
        if (req.bankIfsc() != null)        s.setBankIfsc(req.bankIfsc());
        if (req.bankHolder() != null)      s.setBankHolder(req.bankHolder());
        if (req.address() != null)         s.setAddress(req.address());
        if (req.categories() != null)      s.setCategories(toUpperArray(req.categories()));
        if (req.serviceCities() != null)   s.setServiceCities(toLowerArray(req.serviceCities()));
        if (req.leadTimeDays() != null)    s.setLeadTimeDays(req.leadTimeDays());
        if (req.paymentTerms() != null)    s.setPaymentTerms(req.paymentTerms());
        if (req.creditLimitPaise() != null) s.setCreditLimitPaise(req.creditLimitPaise());
        if (req.kycStatus() != null)       s.setKycStatus(req.kycStatus());
        if (req.kycNotes() != null)        s.setKycNotes(req.kycNotes());
        if (req.notes() != null)           s.setNotes(req.notes());
        if (req.active() != null)          s.setActive(req.active());
        return supplierRepo.save(s);
    }

    @Transactional
    public Supplier setActive(UUID id, boolean active) {
        Supplier s = get(id);
        s.setActive(active);
        return supplierRepo.save(s);
    }

    @Transactional
    public Supplier verifyKyc(UUID id, boolean verified, String notes) {
        Supplier s = get(id);
        s.setKycStatus(verified ? "VERIFIED" : "REJECTED");
        if (notes != null) s.setKycNotes(notes);
        return supplierRepo.save(s);
    }

    // ── Catalog ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SupplierCatalogItem> listCatalog(UUID supplierId, boolean activeOnly) {
        return activeOnly
                ? catalogRepo.findBySupplierIdAndActiveTrueOrderByItemLabelAsc(supplierId)
                : catalogRepo.findBySupplierIdOrderByItemLabelAsc(supplierId);
    }

    @Transactional
    public SupplierCatalogItem addCatalogItem(UUID supplierId, CatalogItemRequest req) {
        get(supplierId); // existence check
        if (req.itemKey() == null || req.itemKey().isBlank())
            throw new IllegalArgumentException("itemKey required");
        if (catalogRepo.findBySupplierIdAndItemKey(supplierId, req.itemKey()).isPresent())
            throw new IllegalStateException("Supplier already has item " + req.itemKey());

        SupplierCatalogItem c = SupplierCatalogItem.builder()
                .supplierId(supplierId)
                .itemKey(req.itemKey())
                .itemLabel(req.itemLabel())
                .category(req.category())
                .unit(req.unit())
                .pricePaise(req.pricePaise())
                .moqQty(req.moqQty())
                .packSize(req.packSize())
                .leadTimeDays(req.leadTimeDays())
                .notes(req.notes())
                .active(req.active() == null ? Boolean.TRUE : req.active())
                .build();
        return catalogRepo.save(c);
    }

    @Transactional
    public SupplierCatalogItem updateCatalogItem(UUID supplierId, UUID itemId, CatalogItemRequest req) {
        SupplierCatalogItem c = catalogRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + itemId));
        if (!supplierId.equals(c.getSupplierId()))
            throw new IllegalArgumentException("Item does not belong to supplier");

        if (req.itemLabel() != null)    c.setItemLabel(req.itemLabel());
        if (req.category() != null)     c.setCategory(req.category());
        if (req.unit() != null)         c.setUnit(req.unit());
        if (req.pricePaise() != null)   c.setPricePaise(req.pricePaise());
        if (req.moqQty() != null)       c.setMoqQty(req.moqQty());
        if (req.packSize() != null)     c.setPackSize(req.packSize());
        if (req.leadTimeDays() != null) c.setLeadTimeDays(req.leadTimeDays());
        if (req.notes() != null)        c.setNotes(req.notes());
        if (req.active() != null)       c.setActive(req.active());
        return catalogRepo.save(c);
    }

    @Transactional
    public void softDeleteCatalogItem(UUID supplierId, UUID itemId) {
        SupplierCatalogItem c = catalogRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + itemId));
        if (!supplierId.equals(c.getSupplierId()))
            throw new IllegalArgumentException("Item does not belong to supplier");
        c.setActive(false);
        catalogRepo.save(c);
    }

    @Transactional
    public void incrementPosCompleted(UUID supplierId) {
        Supplier s = get(supplierId);
        s.setPosCompleted((s.getPosCompleted() == null ? 0 : s.getPosCompleted()) + 1);
        supplierRepo.save(s);
    }

    private String[] toUpperArray(List<String> values) {
        if (values == null) return new String[0];
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toUpperCase())
                .distinct()
                .toArray(String[]::new);
    }

    private String[] toLowerArray(List<String> values) {
        if (values == null) return new String[0];
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase())
                .distinct()
                .toArray(String[]::new);
    }
}
