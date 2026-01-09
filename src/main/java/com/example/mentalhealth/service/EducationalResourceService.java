package com.example.mentalhealth.service;

import com.example.mentalhealth.model.EducationalResource;
import com.example.mentalhealth.repository.EducationalResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EducationalResourceService {
    @Autowired
    private EducationalResourceRepository educationalResourceRepository;

    public List<EducationalResource> getAllResources() {
        return educationalResourceRepository.findAll();
    }

    public List<EducationalResource> getApprovedResources() {
        return educationalResourceRepository.findByStatus("APPROVED");
    }

    public List<EducationalResource> getPendingResources() {
        return educationalResourceRepository.findByStatus("PENDING");
    }

    public EducationalResource saveResource(EducationalResource resource) {
        return educationalResourceRepository.save(resource);
    }

    public List<EducationalResource> getResourcesByCategory(String category) {
        return educationalResourceRepository.findByCategoryAndStatus(category, "APPROVED");
    }

    public List<EducationalResource> searchResources(String keyword) {
        return educationalResourceRepository
                .findByStatusAndTitleContainingIgnoreCaseOrStatusAndDescriptionContainingIgnoreCase(
                        "APPROVED", keyword, "APPROVED", keyword);
    }

    public EducationalResource getResourceById(Long id) {
        return educationalResourceRepository.findById(id).orElse(null);
    }

    public void approveResource(Long id) {
        EducationalResource resource = getResourceById(id);
        if (resource != null) {
            resource.setStatus("APPROVED");
            educationalResourceRepository.save(resource);
        }
    }

    public void rejectResource(Long id) {
        EducationalResource resource = getResourceById(id);
        if (resource != null) {
            resource.setStatus("REJECTED");
            educationalResourceRepository.save(resource);
        }
    }

    public void deleteResource(Long id) {
        educationalResourceRepository.deleteById(id);
    }
}
