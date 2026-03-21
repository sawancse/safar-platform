package com.safar.listing.repository;

import com.safar.listing.entity.WebhookDelivery;
import com.safar.listing.entity.enums.WebhookDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, UUID> {

    List<WebhookDelivery> findByStatus(WebhookDeliveryStatus status);

    List<WebhookDelivery> findByAppId(UUID appId);
}
