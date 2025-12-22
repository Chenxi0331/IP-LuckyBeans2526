package com.example.mentalhealth.service;

import com.example.mentalhealth.model.CounsellingSession;
import com.example.mentalhealth.repository.CounsellingSessionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CounsellingService {
    
    @Autowired
    private CounsellingSessionRepository repo;

    public CounsellingService(CounsellingSessionRepository repo) {
        this.repo = repo;
    }

        public CounsellingSession createSession(CounsellingSession session) {
        return repo.save(session);
    }

    public List<CounsellingSession> getSessionsForStudent(Long studentId) {
        return repo.findByStudentId(studentId);
    }

    public List<CounsellingSession> getSessionsForCounsellor(Long counsellorId) {
        return repo.findByCounsellorId(counsellorId);
    }

    public void save(CounsellingSession session) {
        repo.save(session);
    }

    public List<CounsellingSession> findPending() {
        return repo.findByStatus("PENDING");
    }

    public void updateStatus(Long id, String string) {
        repo.findById(id).ifPresent(s -> {
            s.setStatus(string);
            repo.save(s);
        });
    }

    public long countMonthlyStatus(String status) {
        return repo.findByStatus(status).stream()
                .filter(s -> s.getSessionDate().getMonth() == LocalDateTime.now().getMonth())
                .count();
    }

    // public long countApprovedSessionsThisMonth() {
    // return repo.findByStatus("APPROVED").stream()
    //            .filter(s -> s.getSessionDate().getMonth() == LocalDate.now().getMonth())
    //            .count();
    // }

    public List<CounsellingSession> getHistoryForUser(Long userId) {
        return repo.findByStudentId(userId); 
    }
}
