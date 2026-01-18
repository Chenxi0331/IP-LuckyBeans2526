package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.AssessmentScoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssessmentScoringRepository extends JpaRepository<AssessmentScoring, Integer> {
    List<AssessmentScoring> findByAssessmentTypeIdOrderByMinScoreAsc(Integer assessmentTypeId);
    
    void deleteByAssessmentTypeId(Integer assessmentTypeId);
}