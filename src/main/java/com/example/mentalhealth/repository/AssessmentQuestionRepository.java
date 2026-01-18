package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Integer> {
    
    List<AssessmentQuestion> findAllByOrderByQuestionOrderAsc();
    
    List<AssessmentQuestion> findByAssessmentTypeIdAndStatusOrderByQuestionOrderAsc(
        Integer assessmentTypeId, String status);
    
    List<AssessmentQuestion> findByAssessmentTypeIdOrderByQuestionOrderAsc(Integer assessmentTypeId);
    
    List<AssessmentQuestion> findByStatusOrderByQuestionOrderAsc(String status);
    
    List<AssessmentQuestion> findByStatusInOrderByCreatedAtDesc(List<String> statuses);
   
    List<AssessmentQuestion> findByStatusInAndCreatedByOrderByCreatedAtDesc(
        List<String> statuses, Long createdBy);
    
    long countByStatus(String status);
    
    long countByStatusAndIsEditableTrue(String status);
    
    List<AssessmentQuestion> findByIsEditableTrueAndStatusOrderByQuestionOrderAsc(String status);
}