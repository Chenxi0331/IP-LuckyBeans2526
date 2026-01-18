package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.AssessmentTypeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssessmentTypeRequestRepository extends JpaRepository<AssessmentTypeRequest, Integer> {

    List<AssessmentTypeRequest> findByStatusOrderByRequestedAtDesc(String status);
    
    List<AssessmentTypeRequest> findByRequestedByOrderByRequestedAtDesc(Long requestedBy);
    
    long countByStatus(String status);

    List<AssessmentTypeRequest> findAllByOrderByRequestedAtDesc();
}