package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounsellorNote;
import com.example.mentalhealth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CounsellorNoteRepository extends JpaRepository<CounsellorNote, Long> {
    List<CounsellorNote> findByStudentOrderByCreatedAtDesc(User student);
}
