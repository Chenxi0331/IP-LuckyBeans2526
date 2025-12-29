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

    public EducationalResource saveResource(EducationalResource resource) {
        return educationalResourceRepository.save(resource);
    }

    // NEW: Logic for Category Pills
    public List<EducationalResource> getResourcesByCategory(String category) {
        return educationalResourceRepository.findByCategory(category);
    }

    // NEW: Logic for Search Bar
    public List<EducationalResource> searchResources(String keyword) {
        return educationalResourceRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword,
                keyword);
    }

    public EducationalResource getResourceById(Long id) {
        return educationalResourceRepository.findById(id).orElse(null);
    }

    public void deleteResource(Long id) {
        educationalResourceRepository.deleteById(id);
    }
}
