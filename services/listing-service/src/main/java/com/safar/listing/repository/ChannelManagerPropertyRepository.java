package com.safar.listing.repository;

import com.safar.listing.entity.ChannelManagerProperty;
import com.safar.listing.entity.enums.ChannelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelManagerPropertyRepository extends JpaRepository<ChannelManagerProperty, UUID> {

    Optional<ChannelManagerProperty> findByListingId(UUID listingId);

    List<ChannelManagerProperty> findByStatus(ChannelStatus status);

    boolean existsByListingId(UUID listingId);
}
