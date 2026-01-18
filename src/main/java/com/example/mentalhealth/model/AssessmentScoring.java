package com.example.mentalhealth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_scoring")
public class AssessmentScoring {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer scoringId;
    
    @Column(nullable = false)
    private Integer assessmentTypeId;
    
    @Column(nullable = false, length = 50)
    private String severityLevel;
    
    @Column(nullable = false)
    private Integer minScore;
    
    @Column(nullable = false)
    private Integer maxScore;
    
    @Column(columnDefinition = "TEXT")
    private String interpretation;
    
    @Column(columnDefinition = "TEXT")
    private String recommendations;
    
    // Getters and Setters
    public Integer getScoringId() { return scoringId; }
    public void setScoringId(Integer scoringId) { this.scoringId = scoringId; }
    
    public Integer getAssessmentTypeId() { return assessmentTypeId; }
    public void setAssessmentTypeId(Integer assessmentTypeId) { this.assessmentTypeId = assessmentTypeId; }
    
    public String getSeverityLevel() { return severityLevel; }
    public void setSeverityLevel(String severityLevel) { this.severityLevel = severityLevel; }
    
    public Integer getMinScore() { return minScore; }
    public void setMinScore(Integer minScore) { this.minScore = minScore; }
    
    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }
    
    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
}