package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.UserAssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserAssessmentAnswerRepository extends JpaRepository<UserAssessmentAnswer, Integer> {
    List<UserAssessmentAnswer> findByAssessmentId(Integer assessmentId);
}