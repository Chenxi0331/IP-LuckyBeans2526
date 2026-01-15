package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.CounselorMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CounselorMessageRepository extends JpaRepository<CounselorMessage, Long> {
    
    // Find all messages in a chat, ordered by sent time (oldest first)
    List<CounselorMessage> findByCounselorChatIdOrderBySentAtAsc(Long chatId);
    
    // Count unread messages in a chat that were NOT sent by the specified user
    Long countByCounselorChatIdAndSenderIdNotAndIsReadFalse(Long chatId, Long senderId);
    
    // Find all unread messages in a chat
    List<CounselorMessage> findByCounselorChatIdAndIsReadFalse(Long chatId);
}