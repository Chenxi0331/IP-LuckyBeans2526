package com.example.mentalhealth.model;

public class Recommendation {

    private String icon;
    private String title;
    private String description;
    private String basedOn;
    private String level;
    private Integer duration;
    private Integer progress;
    private String buttonText;
    private Integer moduleId;
    private Boolean isAI;
    private String aiBadge;
    private Boolean isLocked;
    private String contentType;
    private String recommendedBy;    
    private String recommenderType;    
    private Long recommenderId; 

    // Getters and Setters
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBasedOn() {
        return basedOn;
    }

    public void setBasedOn(String basedOn) {
        this.basedOn = basedOn;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Boolean getIsAI() {
        return isAI;
    }

    public void setIsAI(Boolean isAI) {
        this.isAI = isAI;
    }

    public String getAiBadge() {
        return aiBadge;
    }

    public void setAiBadge(String aiBadge) {
        this.aiBadge = aiBadge;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRecommendedBy() {
        return recommendedBy;
    }

    public void setRecommendedBy(String recommendedBy) {
        this.recommendedBy = recommendedBy;
    }

    public String getRecommenderType() {
        return recommenderType;
    }

    public void setRecommenderType(String recommenderType) {
        this.recommenderType = recommenderType;
    }

    public Long getRecommenderId() {
        return recommenderId;
    }

    public void setRecommenderId(Long recommenderId) {
        this.recommenderId = recommenderId;
    }
}