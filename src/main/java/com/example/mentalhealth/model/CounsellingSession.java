package com.example.mentalhealth.model;

import jakarta.persistence.*;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class CounsellingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Long counsellorId;

    private LocalDateTime sessionDate;
    private String status = "PENDING"; // PENDING / APPROVED / COMPLETED

    private String sessionType;
    private String notes;      
    private LocalDateTime createdAt = LocalDateTime.now();
    public void setStatus(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStatus'");
    }
    public LocalDate getSessionDate() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSessionDate'");
    }
    public void setStudentId(Long id2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setStudentId'");
    }
    public void setSessionDate(LocalDateTime dt) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSessionDate'");
    }
    public void setCounsellorId(Long counsellorId2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCounsellorId'");
    }
}
