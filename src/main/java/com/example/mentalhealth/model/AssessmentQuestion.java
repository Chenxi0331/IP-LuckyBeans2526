package com.example.mentalhealth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer questionId;
    
    @Column(columnDefinition = "TEXT")
    private String questionText;
    
    private Integer questionOrder;
    
    @Column(name = "assessment_type_id")
    private Integer assessmentTypeId;
    
    @Column(name = "is_editable")
    private Boolean isEditable = true;
    
    private String status; // PENDING, APPROVED, REJECTED
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;

    @OneToMany
    @JoinColumn(name = "questionId")
    private List<AssessmentOption> options;

    // Getters and Setters
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    
    public Integer getAssessmentTypeId() { return assessmentTypeId; }
    public void setAssessmentTypeId(Integer assessmentTypeId) { this.assessmentTypeId = assessmentTypeId; }
    
    public Boolean getIsEditable() { return isEditable; }
    public void setIsEditable(Boolean isEditable) { this.isEditable = isEditable; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public List<AssessmentOption> getOptions() { return options; }
    public void setOptions(List<AssessmentOption> options) { this.options = options; }
}