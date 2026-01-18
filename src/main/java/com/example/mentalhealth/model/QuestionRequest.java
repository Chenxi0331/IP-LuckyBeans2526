package com.example.mentalhealth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "question_request")
public class QuestionRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer questionRequestId;
    
    @Column(nullable = false)
    private Integer typeRequestId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String questionText;
    
    @Column(nullable = false)
    private Integer questionOrder;
    
    @Column(columnDefinition = "TEXT")
    private String options; 
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
 
    public Integer getQuestionRequestId() { return questionRequestId; }
    public void setQuestionRequestId(Integer questionRequestId) { this.questionRequestId = questionRequestId; }
    
    public Integer getTypeRequestId() { return typeRequestId; }
    public void setTypeRequestId(Integer typeRequestId) { this.typeRequestId = typeRequestId; }
    
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}