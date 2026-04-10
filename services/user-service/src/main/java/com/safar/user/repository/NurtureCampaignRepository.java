package com.safar.user.repository;

import com.safar.user.entity.NurtureCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NurtureCampaignRepository extends JpaRepository<NurtureCampaign, UUID> {
    List<NurtureCampaign> findByCampaignTypeAndActiveTrue(String campaignType);
    List<NurtureCampaign> findByActiveTrue();
}
