package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounselorChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorChatRepository extends JpaRepository<CounselorChat, Long> {
    
    // Find all chats where user is either student or counselor, ordered by most recent
    List<CounselorChat> findByStudentIdOrCounselorIdOrderByLastMessageAtDesc(Long studentId, Long counselorId);
    
    // Find all chats for a specific student
    List<CounselorChat> findByStudentIdOrderByLastMessageAtDesc(Long studentId);
    
    // Find all chats for a specific counselor
    List<CounselorChat> findByCounselorIdOrderByLastMessageAtDesc(Long counselorId);
    
    // Find active chat between a specific student and counselor
    Optional<CounselorChat> findByStudentIdAndCounselorIdAndStatus(Long studentId, Long counselorId, CounselorChat.ChatStatus status);
}