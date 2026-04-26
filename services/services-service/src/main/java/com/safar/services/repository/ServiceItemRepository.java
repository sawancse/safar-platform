package com.safar.services.repository;

import com.safar.services.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {

    List<ServiceItem> findByServiceListingIdAndStatusOrderByDisplayOrderAsc(UUID serviceListingId, String status);

    List<ServiceItem> findByServiceListingId(UUID serviceListingId);
}
