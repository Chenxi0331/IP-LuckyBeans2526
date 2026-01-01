package com.example.mentalhealth.service;

import com.example.mentalhealth.model.CounsellingSession;
import com.example.mentalhealth.model.User;
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

    public List<CounsellingSession> getSessionsForStudent(User student) {
        return repo.findByStudent(student);
    }

    public List<CounsellingSession> getSessionsForCounsellor(User counsellor) {
        return repo.findByCounsellor(counsellor);
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

    public List<CounsellingSession> getHistoryForUser(User user) {
        return repo.findByStudent(user); 
    }

    public List<CounsellingSession> getAllSessions() {
        return repo.findAll();
    }

    public CounsellingSession getSessionById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public void addNotesAndComplete(Long id, String notes) {
        repo.findById(id).ifPresentOrElse(s -> {
            System.out.println("Updating session " + id + " to COMPLETED");
            s.setNotes(notes);
            s.setStatus("COMPLETED");
            repo.save(s);
        }, () -> System.out.println("Session " + id + " not found for update"));
    }
}
