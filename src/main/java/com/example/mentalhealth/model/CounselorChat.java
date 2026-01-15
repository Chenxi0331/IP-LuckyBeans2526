package com.example.mentalhealth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "counselor_chats")
public class CounselorChat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    @ManyToOne
    @JoinColumn(name = "counselor_id", nullable = false)
    private User counselor;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ChatStatus status = ChatStatus.ACTIVE;
    
    @OneToMany(mappedBy = "counselorChat", cascade = CascadeType.ALL)
    private List<CounselorMessage> messages = new ArrayList<>();
    
    public enum ChatStatus {
        ACTIVE, CLOSED
    }
    
    // Constructors
    public CounselorChat() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getStudent() {
        return student;
    }
    
    public void setStudent(User student) {
        this.student = student;
    }
    
    public User getCounselor() {
        return counselor;
    }
    
    public void setCounselor(User counselor) {
        this.counselor = counselor;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
    
    public ChatStatus getStatus() {
        return status;
    }
    
    public void setStatus(ChatStatus status) {
        this.status = status;
    }
    
    public List<CounselorMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<CounselorMessage> messages) {
        this.messages = messages;
    }
}