package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounselorRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CounselorRecommendationRepository extends JpaRepository<CounselorRecommendation, Long> {
    
    // Find active recommendations for a student
    List<CounselorRecommendation> findByStudentIdAndIsActiveTrueOrderByCreatedAtDesc(Long studentId);
    
    // Find all recommendations by counselor
    List<CounselorRecommendation> findByCounselorIdOrderByCreatedAtDesc(Long counselorId);
    
    // Check if recommendation already exists
    boolean existsByStudentIdAndModuleIdAndIsActiveTrue(Long studentId, Integer moduleId);
}
