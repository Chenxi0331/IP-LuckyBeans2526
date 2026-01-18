package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.QuestionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRequestRepository extends JpaRepository<QuestionRequest, Integer> {
    List<QuestionRequest> findByTypeRequestIdOrderByQuestionOrder(Integer typeRequestId);
 
    void deleteByTypeRequestId(Integer typeRequestId);
}