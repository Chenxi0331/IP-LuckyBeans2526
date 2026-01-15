package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.ModuleEditRequestQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleEditRequestQuizRepository extends JpaRepository<ModuleEditRequestQuiz, Integer> {
    
    List<ModuleEditRequestQuiz> findByRequestIdOrderByQuestionOrderAsc(Integer requestId);
    
    void deleteByRequestId(Integer requestId);
    
    Long countByRequestId(Integer requestId);
}