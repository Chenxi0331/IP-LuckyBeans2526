package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.ModuleQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleQuizRepository extends JpaRepository<ModuleQuiz, Integer> {
    List<ModuleQuiz> findByModuleIdAndStatusOrderByQuestionOrderAsc(Integer moduleId, String status);
    List<ModuleQuiz> findByModuleIdOrderByQuestionOrderAsc(Integer moduleId);
    List<ModuleQuiz> findByStatusOrderByCreatedAtDesc(String status);
    Long countByModuleIdAndStatus(Integer moduleId, String status);
}