package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounsellingSession;
import com.example.mentalhealth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CounsellingSessionRepository extends JpaRepository<CounsellingSession, Long> {

    List<CounsellingSession> findByStudent(User student);

    List<CounsellingSession> findByCounsellor(User counsellor);
    List<CounsellingSession> findByStatus(String status);
    List<CounsellingSession> findByCounsellorAndStatus(User counsellor, String status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT s.counsellor FROM CounsellingSession s GROUP BY s.counsellor ORDER BY COUNT(s) DESC LIMIT 1")
    User findMostRequestedCounselor();
}
