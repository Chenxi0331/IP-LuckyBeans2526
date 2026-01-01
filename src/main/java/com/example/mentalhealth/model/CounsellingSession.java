package com.example.mentalhealth.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
public class CounsellingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "counsellor_id")
    private User counsellor;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime sessionDate;
    private String status = "PENDING"; // PENDING / APPROVED / COMPLETED / REJECTED

    private String sessionType;
    private String notes; // Counsellor notes
    
    @Column(columnDefinition = "TEXT")
    private String studentNotes; // Notes from student during booking

    private LocalDateTime createdAt = LocalDateTime.now();
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

    public User getCounsellor() {
        return counsellor;
    }

    public void setCounsellor(User counsellor) {
        this.counsellor = counsellor;
    }

    public LocalDateTime getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDateTime sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStudentNotes() {
        return studentNotes;
    }

    public void setStudentNotes(String studentNotes) {
        this.studentNotes = studentNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
