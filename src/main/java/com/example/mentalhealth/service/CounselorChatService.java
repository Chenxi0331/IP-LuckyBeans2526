package com.example.mentalhealth.service;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CounselorChatService {

    @Autowired
    private CounselorChatRepository counselorChatRepository;

    @Autowired
    private CounselorMessageRepository counselorMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounselorVerificationRepository verificationRepository;

    /**
     * Updated: Now filters for counselors who are both ACTIVE and APPROVED.
     */
    public List<User> getActiveCounselors() {
        List<User> allCounselors = userRepository.findByRole(Role.COUNSELOR);
        
        return allCounselors.stream()
                .filter(counselor -> counselor.isActive()) // Assuming isActive() exists in User model
                .filter(counselor -> {
                    Optional<CounselorVerification> verification = 
                        verificationRepository.findByUserId(counselor.getId());
                    return verification.isPresent() && 
                           verification.get().getStatus() == CounselorVerification.VerificationStatus.APPROVED;
                })
                .collect(Collectors.toList());
    }

    /**
     * Updated: Added security check to prevent chats with unapproved counselors.
     */
    @Transactional
    public CounselorChat createOrGetChat(Long studentId, Long counselorId) {
        // 1. Verify counselor status first
        User counselor = userRepository.findById(counselorId)
                .orElseThrow(() -> new RuntimeException("Counselor not found"));
        
        Optional<CounselorVerification> verification = verificationRepository.findByUserId(counselorId);
        if (verification.isEmpty() || 
            verification.get().getStatus() != CounselorVerification.VerificationStatus.APPROVED) {
            throw new RuntimeException("Counselor is not approved for chatting");
        }

        // 2. Check for existing active chat
        Optional<CounselorChat> existingChat = counselorChatRepository
                .findByStudentIdAndCounselorIdAndStatus(studentId, counselorId, CounselorChat.ChatStatus.ACTIVE);
        
        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        // 3. Create new chat
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        CounselorChat chat = new CounselorChat();
        chat.setStudent(student);
        chat.setCounselor(counselor);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setStatus(CounselorChat.ChatStatus.ACTIVE);

        return counselorChatRepository.save(chat);
    }

    @Transactional
    public CounselorMessage sendMessage(Long chatId, Long senderId, String content) {
        CounselorChat chat = counselorChatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        CounselorMessage message = new CounselorMessage();
        message.setCounselorChat(chat); // Fixed to match your original field name
        message.setSender(sender);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false); // Fixed to match your original field name

        // Update chat metadata
        chat.setLastMessageAt(LocalDateTime.now());
        counselorChatRepository.save(chat);

        return counselorMessageRepository.save(message);
    }

    public List<CounselorChat> getUserChats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Optimized logic to return relevant chats based on role
        if (user.getRole() == Role.STUDENT) {
            return counselorChatRepository.findByStudentIdOrderByLastMessageAtDesc(userId);
        } else {
            return counselorChatRepository.findByCounselorIdOrderByLastMessageAtDesc(userId);
        }
    }

    public List<CounselorMessage> getChatMessages(Long chatId) {
        return counselorMessageRepository.findByCounselorChatIdOrderBySentAtAsc(chatId);
    }

    public Long getUnreadCount(Long chatId, Long userId) {
        return counselorMessageRepository.countByCounselorChatIdAndSenderIdNotAndIsReadFalse(chatId, userId);
    }

    @Transactional
    public void markMessageAsRead(Long messageId) {
        counselorMessageRepository.findById(messageId).ifPresent(msg -> {
            msg.setIsRead(true);
            counselorMessageRepository.save(msg);
        });
    }

    @Transactional
    public void closeChat(Long chatId) {
        counselorChatRepository.findById(chatId).ifPresent(chat -> {
            chat.setStatus(CounselorChat.ChatStatus.CLOSED);
            counselorChatRepository.save(chat);
        });
    }
        public Optional<CounselorChat> getChatById(Long chatId) {
    return counselorChatRepository.findById(chatId);
}
}