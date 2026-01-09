package com.example.mentalhealth.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class AwarenessCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String status = "PENDING";
    private String category;
    private LocalDate startDate;
    private LocalDate endDate;
    private String time;

    // Detailed Content
    @Column(length = 2000)
    private String objective;
    private String campaignType; // Talk, Workshop, etc.

    // Location Details
    private String locationType; // Physical or Online
    private String physicalBuilding;
    private String physicalRoom;
    private String physicalCampus;
    private String onlinePlatform;
    private String onlineLink;

    // Host Info
    private String organizer;
    private String contactInfo;
    private String registrationInfo;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getCampaignType() {
        return campaignType;
    }

    public void setCampaignType(String campaignType) {
        this.campaignType = campaignType;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getPhysicalBuilding() {
        return physicalBuilding;
    }

    public void setPhysicalBuilding(String physicalBuilding) {
        this.physicalBuilding = physicalBuilding;
    }

    public String getPhysicalRoom() {
        return physicalRoom;
    }

    public void setPhysicalRoom(String physicalRoom) {
        this.physicalRoom = physicalRoom;
    }

    public String getPhysicalCampus() {
        return physicalCampus;
    }

    public void setPhysicalCampus(String physicalCampus) {
        this.physicalCampus = physicalCampus;
    }

    public String getOnlinePlatform() {
        return onlinePlatform;
    }

    public void setOnlinePlatform(String onlinePlatform) {
        this.onlinePlatform = onlinePlatform;
    }

    public String getOnlineLink() {
        return onlineLink;
    }

    public void setOnlineLink(String onlineLink) {
        this.onlineLink = onlineLink;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getRegistrationInfo() {
        return registrationInfo;
    }

    public void setRegistrationInfo(String registrationInfo) {
        this.registrationInfo = registrationInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // New field for UC020
    private int participantCount = 0;

    @ManyToMany
    @JoinTable(name = "campaign_participants", joinColumns = @JoinColumn(name = "campaign_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private java.util.Set<User> participants = new java.util.HashSet<>();

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public java.util.Set<User> getParticipants() {
        return participants;
    }

    public void setParticipants(java.util.Set<User> participants) {
        this.participants = participants;
    }
}
