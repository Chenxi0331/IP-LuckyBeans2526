package com.example.mentalhealth.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "user_assessment")
public class UserAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Integer assessmentId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "assessment_result")
    private String assessmentResult;

    @Column(name = "severity_level", length = 50)
    private String severityLevel;

    @Column(name = "assessment_date")
    private LocalDateTime assessmentDate;

    @Column(name = "assessment_type_id")
    private Integer assessmentTypeId;

    @Transient
    private String assessmentTypeName;

    @Transient
    private String recommendations;

    @Transient
    private Integer maxScore;

    @Transient
private String userName;

@Transient
private String userEmail;


public String getUserName() {
    return userName;
}

public void setUserName(String userName) {
    this.userName = userName;
}

public String getUserEmail() {
    return userEmail;
}

public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
}

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }
    
    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getAssessmentResult() {
        return assessmentResult;
    }

    public void setAssessmentResult(String assessmentResult) {
        this.assessmentResult = assessmentResult;
    }

    public String getSeverityLevel() {
        return severityLevel;
    }

    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }

    public LocalDateTime getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(LocalDateTime assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    public Integer getAssessmentTypeId() {
        return assessmentTypeId;
    }

    public void setAssessmentTypeId(Integer assessmentTypeId) {
        this.assessmentTypeId = assessmentTypeId;
    }

    public String getAssessmentTypeName() {
        return assessmentTypeName;
    }

    public void setAssessmentTypeName(String assessmentTypeName) {
        this.assessmentTypeName = assessmentTypeName;
    }
}