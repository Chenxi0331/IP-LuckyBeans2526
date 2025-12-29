package com.example.mentalhealth.model;

import jakarta.persistence.*;
import jakarta.persistence.*;

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
}
