package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.AssessmentOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;

@Repository
public interface AssessmentOptionRepository extends JpaRepository<AssessmentOption, Integer> {
    List<AssessmentOption> findByQuestionId(Integer questionId);
    
    Optional<AssessmentOption> findById(Integer optionId);
}
