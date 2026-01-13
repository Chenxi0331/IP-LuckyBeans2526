package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.UserModuleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserModuleProgressRepository extends JpaRepository<UserModuleProgress, Integer> {

    Optional<UserModuleProgress> findByUserIdAndModuleId(Integer userId, Integer moduleId);

    @Query("SELECT COUNT(p) FROM UserModuleProgress p WHERE p.userId = :userId AND p.status = 'completed'")
    Long countCompletedModules(@Param("userId") Integer userId);

    @Query("SELECT AVG(p.progressPercentage) FROM UserModuleProgress p WHERE p.userId = :userId")
    Double getAverageProgress(@Param("userId") Integer userId);

    List<UserModuleProgress> findByUserIdAndStatus(Integer userId, String status);
    
    List<UserModuleProgress> findByUserId(Integer userId);
}