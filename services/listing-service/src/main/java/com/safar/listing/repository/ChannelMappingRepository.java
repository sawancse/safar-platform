package com.safar.listing.repository;

import com.safar.listing.entity.ChannelMapping;
import com.safar.listing.entity.enums.ChannelName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChannelMappingRepository extends JpaRepository<ChannelMapping, UUID> {

    List<ChannelMapping> findByChannelManagerPropertyId(UUID propertyId);

    List<ChannelMapping> findByChannelManagerPropertyIdAndChannelName(UUID propertyId, ChannelName channelName);
}
