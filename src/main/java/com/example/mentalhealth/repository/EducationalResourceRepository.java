package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.EducationalResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EducationalResourceRepository extends JpaRepository<EducationalResource, Long> {

    // Finds resources by category (e.g., "Anxiety")
    List<EducationalResource> findByCategory(String category);

    // Searches for keyword in title OR description, ignoring case (A == a)
    List<EducationalResource> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title,
            String description);
}