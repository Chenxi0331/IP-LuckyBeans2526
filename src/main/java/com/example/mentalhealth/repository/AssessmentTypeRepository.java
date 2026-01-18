package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.AssessmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssessmentTypeRepository extends JpaRepository<AssessmentType, Integer> {
    
    List<AssessmentType> findByIsActiveTrueAndStatusOrderByTypeNameAsc(String status);
    
    AssessmentType findByTypeCode(String typeCode);
    
    List<AssessmentType> findByIsEditableTrueAndStatusOrderByTypeNameAsc(String status);
    
    long countByStatus(String status);
    
    List<AssessmentType> findByIsSystemDefaultFalseOrderByTypeNameAsc();
}