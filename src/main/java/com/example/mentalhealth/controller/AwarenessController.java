package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.EducationalResource;
import com.example.mentalhealth.model.AwarenessCampaign;
import com.example.mentalhealth.service.EducationalResourceService;
import com.example.mentalhealth.service.AwarenessCampaignService;
import org.springframework.beans.factory.annotation.Autowired;
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

    // UC019 & UC020: Unified View for Resources and Campaigns
    @GetMapping("/library")
    public String awarenessLibrary(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            Model model) {

        // 1. Fetch Resources with Filtering logic
        List<EducationalResource> resources;
        if (search != null && !search.isEmpty()) {
            resources = educationalResourceService.searchResources(search);
        } else if (category != null && !category.isEmpty() && !category.equals("All")) {
            resources = educationalResourceService.getResourcesByCategory(category);
        } else {
            resources = educationalResourceService.getAllResources();
        }

        // 2. Fetch Campaigns for the second tab
        List<AwarenessCampaign> campaigns = awarenessCampaignService.getAllCampaigns();

        // 3. Add to Model
        model.addAttribute("resources", resources);
        model.addAttribute("campaigns", campaigns);
        model.addAttribute("selectedCategory", (category == null) ? "All" : category);

        return "awareness/resources"; // This matches your HTML file location
    }

    // UC020: Handle joining a campaign (POST)
    @PostMapping("/campaigns/join/{id}")
    public String joinCampaign(@PathVariable Long id) {
        awarenessCampaignService.registerUserForCampaign(id);
        return "redirect:/awareness/library?tab=campaigns&success=joined";
    }

    // UC023: Update Educational Resources (Admin) - List Resources
    @GetMapping("/manage")
    public String manageResources(Model model) {
        // Fetch all resources for the admin table
        List<EducationalResource> resources = educationalResourceService.getAllResources();
        model.addAttribute("resources", resources);
        return "awareness/manage-resources";
    }

    // UC023: Show Create Form
    @GetMapping("/resource/new")
    public String showCreateResourceForm(Model model) {
        model.addAttribute("resource", new EducationalResource());
        return "awareness/resource-form";
    }

    // UC023: Save/Update Resource
    @PostMapping("/resource/save")
    public String saveResource(@ModelAttribute EducationalResource resource) {
        educationalResourceService.saveResource(resource);
        return "redirect:/awareness/manage?success=saved";
    }

    // UC023: Show Edit Form
    @GetMapping("/resource/edit/{id}")
    public String showEditResourceForm(@PathVariable Long id, Model model) {
        EducationalResource resource = educationalResourceService.getResourceById(id);
        if (resource == null) {
            return "redirect:/awareness/manage?error=notfound";
        }
        model.addAttribute("resource", resource);
        return "awareness/resource-form";
    }

    // UC023: Delete Resource
    @PostMapping("/resource/delete/{id}")
    public String deleteResource(@PathVariable Long id) {
        educationalResourceService.deleteResource(id);
        return "redirect:/awareness/manage?success=deleted";
    }
}