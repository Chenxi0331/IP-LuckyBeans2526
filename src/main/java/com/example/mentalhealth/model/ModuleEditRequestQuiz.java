package com.example.mentalhealth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "module_edit_request_quiz")
public class ModuleEditRequestQuiz {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "temp_quiz_id")
    private Integer tempQuizId;
    
    @Column(name = "request_id", nullable = false)
    private Integer requestId;
    
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;
    
    @Column(name = "option_a", length = 500, nullable = false)
    private String optionA;
    
    @Column(name = "option_b", length = 500, nullable = false)
    private String optionB;
    
    @Column(name = "option_c", length = 500, nullable = false)
    private String optionC;
    
    @Column(name = "option_d", length = 500, nullable = false)
    private String optionD;
    
    @Column(name = "correct_answer", length = 1)
    private String correctAnswer;
    
    @Column(name = "question_order")
    private Integer questionOrder = 1;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getTempQuizId() {
        return tempQuizId;
    }

    public void setTempQuizId(Integer tempQuizId) {
        this.tempQuizId = tempQuizId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}