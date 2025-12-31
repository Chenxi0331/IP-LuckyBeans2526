package com.example.mentalhealth.service;

import com.example.mentalhealth.model.AwarenessCampaign;
import com.example.mentalhealth.repository.AwarenessCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AwarenessCampaignService {
    @Autowired
    private AwarenessCampaignRepository awarenessCampaignRepository;

    public List<AwarenessCampaign> getAllCampaigns() {
        return awarenessCampaignRepository.findAll();
    }

    public AwarenessCampaign saveCampaign(AwarenessCampaign campaign) {
        return awarenessCampaignRepository.save(campaign);
    }
    // Logic for UC020: Participate in Campaign
    public void registerUserForCampaign(Long campaignId) {
        AwarenessCampaign campaign = awarenessCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        
        // Logic: Increment participant count or add to a ManyToMany list
        // For now, let's assume you have a 'participantCount' field
        campaign.setParticipantCount(campaign.getParticipantCount() + 1);
        awarenessCampaignRepository.save(campaign);
    }
}
