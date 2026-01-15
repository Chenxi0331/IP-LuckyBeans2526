package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.EducationalResource;
import com.example.mentalhealth.model.AwarenessCampaign;
import com.example.mentalhealth.service.EducationalResourceService;
import com.example.mentalhealth.service.AwarenessCampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/awareness")
public class AwarenessController {

    @Autowired
    private EducationalResourceService educationalResourceService;

    @Autowired
    private AwarenessCampaignService awarenessCampaignService;

    // UC019 & UC020: Unified View for Resources and Campaigns (Public)
    @GetMapping("/library")
    public String awarenessLibrary(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Model model,
            java.security.Principal principal) {

        List<EducationalResource> resources;
        if (search != null && !search.isEmpty()) {
            resources = educationalResourceService.searchResources(search);
        } else if (category != null && !category.isEmpty() && !category.equals("All")) {
            resources = educationalResourceService.getResourcesByCategory(category);
        } else {
            resources = educationalResourceService.getApprovedResources();
        }

        List<AwarenessCampaign> campaigns = awarenessCampaignService.getApprovedCampaigns();

        // Calculate joined campaigns for the current user
        java.util.Set<Long> joinedCampaignIds = new java.util.HashSet<>();
        if (principal != null) {
            String username = principal.getName();
            System.out.println("DEBUG: Current User: " + username); // DEBUG
            for (AwarenessCampaign campaign : campaigns) {
                // Check if participants list contains the user
                // Note: We need to be careful with lazy loading if participants is lazy,
                // but for now assuming it works or is eager/open-session-in-view.
                // A more efficient way would be a service method, but we can do this in-memory
                // for now.
                boolean isJoined = campaign.getParticipants().stream()
                        .anyMatch(u -> (u.getUsername() != null && u.getUsername().equals(username))
                                || (u.getEmail() != null && u.getEmail().equals(username)));
                System.out.println("DEBUG: Campaign ID: " + campaign.getId() + ", Participants: "
                        + campaign.getParticipants().size() + ", Is Joined: " + isJoined); // DEBUG
                if (isJoined) {
                    joinedCampaignIds.add(campaign.getId());
                }
            }
        } else {
            System.out.println("DEBUG: Principal is null"); // DEBUG
        }

        model.addAttribute("resources", resources);
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("selectedCategory", (category == null) ? "All" : category);
        model.addAttribute("joinedCampaignIds", joinedCampaignIds);

        return "awareness/resources";
    }

    // UC020: Handle joining a campaign (POST)
    @PostMapping("/campaigns/join/{id}")
    public String joinCampaign(@PathVariable Long id, java.security.Principal principal) {
        if (principal != null) {
            System.out.println("DEBUG: Joining Campaign " + id + " as " + principal.getName()); // DEBUG
            awarenessCampaignService.registerUserForCampaign(id, principal.getName());
        }
        return "redirect:/awareness/campaign/view/" + id + "?success=joined";
    }

    // --- Management for Counsellors and Admins ---

    @GetMapping("/manage")
    @PreAuthorize("hasAnyAuthority('ROLE_COUNSELOR', 'ROLE_ADMIN')")
    public String manageContent(Model model) {
        model.addAttribute("resources", educationalResourceService.getAllResources());
        model.addAttribute("campaigns", awarenessCampaignService.getAllCampaigns());
        return "awareness/manage-resources";
    }

    // Resource Management
    @GetMapping("/resource/new")
    @PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
    public String showCreateResourceForm(Model model) {
        model.addAttribute("resource", new EducationalResource());
        return "awareness/resource-form";
    }

    @PostMapping("/resource/save")
    @PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
    public String saveResource(@ModelAttribute EducationalResource resource) {
        // When saved by counsellor, it stays in PENDING (default) or REJECTED becomes
        // PENDING again
        resource.setStatus("PENDING");
        educationalResourceService.saveResource(resource);
        return "redirect:/awareness/manage?success=saved";
    }

    @GetMapping("/resource/edit/{id}")
    @PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
    public String showEditResourceForm(@PathVariable Long id, Model model) {
        EducationalResource resource = educationalResourceService.getResourceById(id);
        if (resource == null)
            return "redirect:/awareness/manage?error=notfound";
        model.addAttribute("resource", resource);
        return "awareness/resource-form";
    }

    @PostMapping("/resource/archive/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String archiveResource(@PathVariable Long id) {
        educationalResourceService.archiveResource(id);
        return "redirect:/awareness/manage?success=archived";
    }

    @PostMapping("/resource/approve/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String approveResource(@PathVariable Long id) {
        educationalResourceService.approveResource(id);
        return "redirect:/awareness/manage?success=approved";
    }

    @PostMapping("/resource/reject/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String rejectResource(@PathVariable Long id) {
        educationalResourceService.rejectResource(id);
        return "redirect:/awareness/manage?success=rejected";
    }

    @GetMapping("/resource/view/{id}")
    public String showResourceDetails(@PathVariable Long id, Model model) {
        EducationalResource resource = educationalResourceService.getResourceById(id);
        if (resource == null)
            return "redirect:/awareness/library?error=notfound";
        model.addAttribute("resource", resource);
        return "awareness/resource-view";
    }

    // Campaign Management
    @GetMapping("/campaign/new")
    @PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
    public String showCreateCampaignForm(Model model) {
        model.addAttribute("campaign", new AwarenessCampaign());
        return "awareness/campaign-form"; // Need to create this
    }

    @PostMapping("/campaign/save")
    @PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
    public String saveCampaign(@ModelAttribute AwarenessCampaign campaign) {
        campaign.setStatus("PENDING");
        awarenessCampaignService.saveCampaign(campaign);
        return "redirect:/awareness/manage?success=saved";
    }

    @GetMapping("/campaign/edit/{id}")
    @PreAuthorize("hasAuthority('ROLE_COUNSELOR')")
    public String showEditCampaignForm(@PathVariable Long id, Model model) {
        AwarenessCampaign campaign = awarenessCampaignService.getCampaignById(id);
        if (campaign == null)
            return "redirect:/awareness/manage?error=notfound";
        model.addAttribute("campaign", campaign);
        return "awareness/campaign-form";
    }

    @PostMapping("/campaign/archive/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String archiveCampaign(@PathVariable Long id) {
        awarenessCampaignService.archiveCampaign(id);
        return "redirect:/awareness/manage?success=archived";
    }

    @PostMapping("/campaign/approve/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String approveCampaign(@PathVariable Long id) {
        awarenessCampaignService.approveCampaign(id);
        return "redirect:/awareness/manage?success=approved";
    }

    @PostMapping("/campaign/reject/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String rejectCampaign(@PathVariable Long id) {
        awarenessCampaignService.rejectCampaign(id);
        return "redirect:/awareness/manage?success=rejected";
    }

    @GetMapping("/campaign/view/{id}")
    public String showCampaignDetails(@PathVariable Long id, Model model, java.security.Principal principal) {
        AwarenessCampaign campaign = awarenessCampaignService.getCampaignById(id);
        if (campaign == null)
            return "redirect:/awareness/library?error=notfound";
        model.addAttribute("campaign", campaign);

        boolean isJoined = false;
        if (principal != null) {
            isJoined = awarenessCampaignService.isUserRegistered(id, principal.getName());
        }
        model.addAttribute("isJoined", isJoined);

        return "awareness/campaign-view";
    }
}