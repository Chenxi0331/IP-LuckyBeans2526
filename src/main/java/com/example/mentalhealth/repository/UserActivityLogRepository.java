package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounselorVerification;
import com.example.mentalhealth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserActivityLogRepository extends JpaRepository<com.example.mentalhealth.model.UserActivityLog, Long> {
    List<com.example.mentalhealth.model.UserActivityLog> findByUserIdOrderByTimestampDesc(Long userId);
    
    @Query("SELECT uac FROM UserActivityLog uac WHERE uac.user.id = ?1 ORDER BY uac.timestamp DESC LIMIT 50")
    List<com.example.mentalhealth.model.UserActivityLog> findLatestActivityByUserId(Long userId);
}