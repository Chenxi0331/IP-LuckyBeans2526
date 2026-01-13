package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounselorComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CounselorCommentRepository extends JpaRepository<CounselorComment, Long> {
    List<CounselorComment> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<CounselorComment> findByStudentIdAndIsVisibleToStudentTrueOrderByCreatedAtDesc(Long studentId);
    List<CounselorComment> findByAssessmentIdOrderByCreatedAtDesc(Integer assessmentId);
}