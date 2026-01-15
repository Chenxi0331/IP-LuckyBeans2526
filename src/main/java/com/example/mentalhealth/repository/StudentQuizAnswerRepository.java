package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.StudentQuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentQuizAnswerRepository extends JpaRepository<StudentQuizAnswer, Long> {
    List<StudentQuizAnswer> findByUserIdAndModuleId(Long userId, Integer moduleId);
    Optional<StudentQuizAnswer> findByUserIdAndQuizId(Long userId, Integer quizId);
    Long countByUserIdAndModuleIdAndIsCorrect(Long userId, Integer moduleId, Boolean isCorrect);
    Long countByUserIdAndModuleId(Long userId, Integer moduleId);
    boolean existsByUserIdAndQuizId(Long userId, Integer quizId);
}