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

    public void deleteSession(Long id, User user) {
        repo.findById(id).ifPresent(s -> {
            if (s.getStudent().getId().equals(user.getId()) && "PENDING".equals(s.getStatus())) {
                repo.delete(s);
            } else {
                throw new IllegalStateException("Not authorized to delete this session or session is not pending");
            }
        });
    }

    public List<CounsellingSession> findPendingForCounselor(User counselor) {
        return repo.findByCounsellorAndStatus(counselor, "PENDING");
    }

    public void approveSession(Long id, String meetingLink) {
        repo.findById(id).ifPresent(s -> {
            s.setStatus("APPROVED");
            s.setMeetingLink(meetingLink);
            repo.save(s);
        });
    }

    public void updateStatus(Long id, String status) {
        repo.findById(id).ifPresent(s -> {
            s.setStatus(status);
            repo.save(s);
        });
    }

    public java.util.Map<String, Object> getAnalytics() {
        java.util.Map<String, Object> analytics = new java.util.HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);

        analytics.put("totalBookings", repo.countByCreatedAtBetween(start, end));
        analytics.put("approvedCount", repo.findByStatus("APPROVED").size()); // broad count, maybe should be monthly? keeping simple for now
        analytics.put("rejectedCount", repo.findByStatus("REJECTED").size());
        
        User topCounselor = repo.findMostRequestedCounselor();
        analytics.put("topCounselor", topCounselor != null ? topCounselor.getFullName() : "N/A");
        
        return analytics;
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
