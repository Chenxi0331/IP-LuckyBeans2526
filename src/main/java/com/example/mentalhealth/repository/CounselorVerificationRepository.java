package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounselorVerification;
import com.example.mentalhealth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// CounselorVerificationRepository
@Repository
public interface CounselorVerificationRepository extends JpaRepository<CounselorVerification, Long> {
    Optional<CounselorVerification> findByUserId(Long userId);
    
    List<CounselorVerification> findByStatus(CounselorVerification.VerificationStatus status);
    
    @Query("SELECT cv FROM CounselorVerification cv WHERE cv.status = 'PENDING' ORDER BY cv.requestedAt ASC")
    List<CounselorVerification> findPendingVerifications();
}

