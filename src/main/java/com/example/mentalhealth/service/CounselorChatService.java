package com.example.mentalhealth.service;

import com.example.mentalhealth.model.CounselorChat;
import com.example.mentalhealth.model.CounselorMessage;
import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.CounselorChatRepository;
import com.example.mentalhealth.repository.CounselorMessageRepository;
import com.example.mentalhealth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CounselorChatService {
    
    @Autowired
    private CounselorChatRepository counselorChatRepository;
    
    @Autowired
    private CounselorMessageRepository counselorMessageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getActiveCounselors() {
        return userRepository.findByRole(Role.COUNSELOR);
    }
    
    @Transactional
    public CounselorChat createOrGetChat(Long studentId, Long counselorId) {
        Optional<CounselorChat> existingChat = counselorChatRepository
            .findByStudentIdAndCounselorIdAndStatus(studentId, counselorId, CounselorChat.ChatStatus.ACTIVE);
        
        if (existingChat.isPresent()) {
            return existingChat.get();
        }
        
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        User counselor = userRepository.findById(counselorId)
            .orElseThrow(() -> new RuntimeException("Counselor not found"));
        
        CounselorChat chat = new CounselorChat();
        chat.setStudent(student);
        chat.setCounselor(counselor);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setStatus(CounselorChat.ChatStatus.ACTIVE);
        
        return counselorChatRepository.save(chat);
    }
    
    public List<CounselorChat> getUserChats(Long userId) {
        return counselorChatRepository.findByStudentIdOrCounselorIdOrderByLastMessageAtDesc(userId, userId);
    }
    
    public List<CounselorMessage> getChatMessages(Long chatId) {
        return counselorMessageRepository.findByCounselorChatIdOrderBySentAtAsc(chatId);
    }
    
    @Transactional
    public CounselorMessage sendMessage(Long chatId, Long senderId, String content) {
        CounselorChat chat = counselorChatRepository.findById(chatId)
            .orElseThrow(() -> new RuntimeException("Chat not found"));
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        CounselorMessage message = new CounselorMessage();
        message.setCounselorChat(chat);
        message.setSender(sender);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);
        
        CounselorMessage savedMessage = counselorMessageRepository.save(message);
        
        chat.setLastMessageAt(LocalDateTime.now());
        counselorChatRepository.save(chat);
        
        return savedMessage;
    }
    
    public Long getUnreadCount(Long chatId, Long userId) {
        return counselorMessageRepository.countByCounselorChatIdAndSenderIdNotAndIsReadFalse(chatId, userId);
    }
    
    public Optional<CounselorChat> getChatById(Long chatId) {
        return counselorChatRepository.findById(chatId);
    }
}