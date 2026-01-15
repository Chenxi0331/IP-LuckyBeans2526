package com.example.mentalhealth.service;

import com.example.mentalhealth.model.AwarenessCampaign;
import com.example.mentalhealth.repository.AwarenessCampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AwarenessCampaignService {
    @Autowired
    private AwarenessCampaignRepository awarenessCampaignRepository;

    @Autowired
    private com.example.mentalhealth.repository.UserRepository userRepository;

    public List<AwarenessCampaign> getAllCampaigns() {
        return awarenessCampaignRepository.findAll();
    }

    public List<AwarenessCampaign> getApprovedCampaigns() {
        return awarenessCampaignRepository.findByStatus("APPROVED");
    }

    public List<AwarenessCampaign> getPendingCampaigns() {
        return awarenessCampaignRepository.findByStatus("PENDING");
    }

    public AwarenessCampaign saveCampaign(AwarenessCampaign campaign) {
        return awarenessCampaignRepository.save(campaign);
    }

    @SuppressWarnings("null")
    public AwarenessCampaign getCampaignById(Long id) {
        return awarenessCampaignRepository.findById(id).orElse(null);
    }

    public void approveCampaign(Long id) {
        AwarenessCampaign campaign = getCampaignById(id);
        if (campaign != null) {
            campaign.setStatus("APPROVED");
            awarenessCampaignRepository.save(campaign);
        }
    }

    public void rejectCampaign(Long id) {
        AwarenessCampaign campaign = getCampaignById(id);
        if (campaign != null) {
            campaign.setStatus("REJECTED");
            awarenessCampaignRepository.save(campaign);
        }
    }

    // Logic for UC020: Participate in Campaign
    @SuppressWarnings("null")
    public void registerUserForCampaign(Long campaignId, String username) {
        System.out.println("DEBUG: SERVICE - registerUserForCampaign called for " + campaignId + " / " + username);
        AwarenessCampaign campaign = awarenessCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        com.example.mentalhealth.model.User user = userRepository.findByUsername(username);
        if (user == null) {
            System.out.println("DEBUG: SERVICE - User not found by username. Trying email...");
            user = userRepository.findByEmail(username).orElse(null);
        }

        if (user != null) {
            System.out.println("DEBUG: SERVICE - User found: " + user.getId());
            boolean added = campaign.getParticipants().add(user);
            System.out.println("DEBUG: SERVICE - Added to set? " + added);
            campaign.setParticipantCount(campaign.getParticipants().size());

            // Explicitly flush to force DB insert
            awarenessCampaignRepository.saveAndFlush(campaign);
            System.out.println(
                    "DEBUG: SERVICE - saveAndFlush called. Participants size: " + campaign.getParticipants().size());
        } else {
            System.out.println("DEBUG: SERVICE - User NOT found by username OR email!");
        }
    }

    public boolean isUserRegistered(Long campaignId, String username) {
        AwarenessCampaign campaign = awarenessCampaignRepository.findById(campaignId).orElse(null);
        if (campaign == null)
            return false;

        boolean registered = campaign.getParticipants().stream()
                .anyMatch(u -> (u.getUsername() != null && u.getUsername().equals(username))
                        || (u.getEmail() != null && u.getEmail().equals(username)));
        System.out.println("DEBUG: SERVICE - isUserRegistered(" + campaignId + ", " + username + ") = " + registered);
        return registered;
    }

    public void archiveCampaign(Long id) {
        AwarenessCampaign campaign = getCampaignById(id);
        if (campaign != null) {
            campaign.setStatus("ARCHIVED");
            awarenessCampaignRepository.save(campaign);
        }
    }
}
