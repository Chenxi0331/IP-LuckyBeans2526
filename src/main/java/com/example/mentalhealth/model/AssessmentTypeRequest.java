package com.example.mentalhealth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment_type_request")
public class AssessmentTypeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer requestId;
    
    // 新增：关联的评估类型ID（用于UPDATE类型）
    @Column(name = "assessment_type_id")
    private Integer assessmentTypeId;
    
    @Column(nullable = false, length = 20)
    private String typeCode;
    
    @Column(nullable = false, length = 100)
    private String typeName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer totalQuestions;
    
    @Column(nullable = false)
    private Integer maxScore;
    
    @Column(columnDefinition = "TEXT")
    private String instructions;
    
    @Column(columnDefinition = "TEXT")
    private String scoringCriteria;
    
    // 新增：请求类型 (CREATE 或 UPDATE)
    @Column(length = 20)
    private String requestType = "CREATE";
    
    @Column(length = 20)
    private String status = "PENDING";
    
    @Column(nullable = false)
    private Long requestedBy;
    
    @Column(updatable = false)
    private LocalDateTime requestedAt;
    
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    
    @Column(columnDefinition = "TEXT")
    private String reviewNotes;
    
    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    
    public Integer getAssessmentTypeId() { return assessmentTypeId; }
    public void setAssessmentTypeId(Integer assessmentTypeId) { this.assessmentTypeId = assessmentTypeId; }
    
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }
    
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    
    public String getScoringCriteria() { return scoringCriteria; }
    public void setScoringCriteria(String scoringCriteria) { this.scoringCriteria = scoringCriteria; }
    
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}