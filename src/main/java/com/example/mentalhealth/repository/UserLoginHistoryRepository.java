package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounselorVerification;
import com.example.mentalhealth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLoginHistoryRepository extends JpaRepository<com.example.mentalhealth.model.UserLoginHistory, Long> {
    List<com.example.mentalhealth.model.UserLoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId);
    
    @Query("SELECT ulh FROM UserLoginHistory ulh WHERE ulh.user.id = ?1 ORDER BY ulh.loginTime DESC LIMIT 10")
    List<com.example.mentalhealth.model.UserLoginHistory> findLatestLoginsByUserId(Long userId);
}