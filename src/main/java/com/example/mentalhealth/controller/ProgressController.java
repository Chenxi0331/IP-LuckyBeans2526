package com.example.mentalhealth.controller;

import com.example.mentalhealth.model.MoodEntry;
import com.example.mentalhealth.model.ProgressInsight;
import com.example.mentalhealth.model.CounsellorNote;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.service.MoodEntryService;
import com.example.mentalhealth.service.ProgressInsightService;
import com.example.mentalhealth.service.CounsellorNoteService;
import com.example.mentalhealth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/progress")
public class ProgressController {

    @Autowired
    private MoodEntryService moodEntryService;

    @Autowired
    private ProgressInsightService progressInsightService;

    @Autowired
    private CounsellorNoteService counsellorNoteService;

    @Autowired
    private UserRepository userRepository;

    // UC018: Complete Mood Tracker (Student) - View Data
    @GetMapping("/mood")
    public String moodTracker(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null)
            return "error/404";

        List<MoodEntry> allEntries = moodEntryService.getEntriesForUser(user);

        List<MoodEntry> latestThree = allEntries.stream()
                .sorted(Comparator.comparing(MoodEntry::getDate).reversed())
                .limit(3)
                .toList();

        model.addAttribute("latestEntries", latestThree);
        model.addAttribute("entries", allEntries);
        model.addAttribute("user", user); // Pass user to check consent flag in view

        // Prepare 7-day data for the charts
        model.addAttribute("moodChartData", getChartData(user, 7));

        return "progress/mood-tracker";
    }

    // UC018: Complete Mood Tracker (Student) - Save Data
    @PostMapping("/mood")
    public String submitMood(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String mood,
            @RequestParam(required = false) String activity,
            @RequestParam(required = false) String note) {

        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null)
            return "error/404";

        MoodEntry entry = new MoodEntry();
        entry.setUser(user);
        entry.setDate(LocalDate.now());
        entry.setMood(mood);
        entry.setActivity(activity);
        entry.setNote(note);

        moodEntryService.saveEntry(entry);

        return "redirect:/progress/mood?success=true";
    }

    // New: Toggle Consent
    @PostMapping("/mood/consent")
    public String toggleConsent(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false, defaultValue = "false") boolean consent) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user != null) {
            user.setConsentMoodSharing(consent);
            userRepository.save(user);
        }
        return "redirect:/progress/mood?consentSaved=true";
    }

    // New: List students for Counsellor
    @GetMapping("/search")
    public String searchStudents(Model model) {
        try {
            List<User> students = userRepository.findByRole(Role.STUDENT);
            model.addAttribute("students", students != null ? students : new ArrayList<>());
        } catch (Exception e) {
            model.addAttribute("students", new ArrayList<>());
        }
        return "progress/search";
    }

    @GetMapping("/students/{id}")
    @SuppressWarnings("null")
    public String monitorStudentProgress(@PathVariable Long id, Model model) {
        try {
            System.out.println("DEBUG: Entering monitorStudentProgress for ID: " + id);
            User student = userRepository.findById(id).orElse(null);
            if (student == null) {
                System.out.println("DEBUG: Student not found with ID: " + id);
                return "redirect:/progress/search"; // Safer than error/404 if missing
            }

            model.addAttribute("student", student);
            boolean noConsent = !student.isConsentMoodSharing();
            model.addAttribute("noConsent", noConsent);
            System.out.println("DEBUG: Found student: " + student.getUsername());
            System.out.println("DEBUG: Consent status: " + student.isConsentMoodSharing());

            if (noConsent) {
                return "progress/student-progress";
            }

            // Summary Data Only
            Map<String, Long> summary = moodEntryService.getMoodSummary(student);
            System.out.println("DEBUG: Mood Summary size: " + (summary != null ? summary.size() : "null"));
            model.addAttribute("moodSummary", summary);

            // Trend Data (Charts)
            List<Map<String, Object>> chartData = getChartData(student, 14);
            System.out.println("DEBUG: Chart Data size: " + (chartData != null ? chartData.size() : "null"));
            model.addAttribute("moodChartData", chartData);

            // Counsellor Private Notes
            model.addAttribute("counsellorNotes", counsellorNoteService.getNotesForStudent(student));

            return "progress/student-progress";
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in monitorStudentProgress: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/progress/search?error=details_fail";
        }
    }

    // Helper for chart data
    private List<Map<String, Object>> getChartData(User user, int days) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> moodChartData = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            List<MoodEntry> dayEntries = moodEntryService.getEntriesForUserByDate(user, date);

            String moodStr = (dayEntries.isEmpty() || dayEntries.get(0).getMood() == null) ? "-"
                    : dayEntries.get(0).getMood();

            int moodValue = -1;
            if (!"-".equals(moodStr)) {
                moodValue = switch (moodStr) {
                    case "Sad 😢" -> 0;
                    case "Neutral 😐" -> 1;
                    case "Happy 😊" -> 2;
                    case "Angry 😠" -> 3;
                    case "Excited 🤩" -> 4;
                    default -> -1;
                };
            }

            Map<String, Object> chartEntry = new HashMap<>();
            chartEntry.put("date", date.toString());
            chartEntry.put("moodValue", moodValue);
            moodChartData.add(chartEntry);
        }
        return moodChartData;
    }

    // UC021: Add Private Counsellor Note
    @PostMapping("/students/{id}/notes")
    public String saveCounsellorNote(@PathVariable Long id, @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails, Model model) {
        try {
            User student = userRepository.findById(id).orElse(null);
            if (student == null)
                return "redirect:/progress/search";

            CounsellorNote note = new CounsellorNote();
            note.setStudent(student);
            note.setCounsellorEmail(userDetails.getUsername());
            note.setContent(content);

            counsellorNoteService.saveNote(note);

            return "redirect:/progress/students/" + id + "?noteSaved=true";
        } catch (Exception e) {
            return "redirect:/progress/students/" + id + "?error=note_fail";
        }
    }

    @Autowired
    private com.example.mentalhealth.repository.UserAssessmentRepository userAssessmentRepository;

    // UC020: View Progress Insights Dashboard
    @GetMapping("/dashboard")
    public String progressDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null)
            return "redirect:/login";

        model.addAttribute("user", user);

        // 1. Mood Chart Data (Last 7 Days)
        model.addAttribute("dashboardMoodChartData", getChartData(user, 7));

        // 2. Weekly Goal Percent (Days with at least one entry in the last 7 days)
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6); // inclusive of today = 7 days
        List<MoodEntry> recentEntries = moodEntryService.getEntriesForUser(user).stream()
                .filter(e -> !e.getDate().isBefore(sevenDaysAgo))
                .toList();

        long uniqueDays = recentEntries.stream()
                .map(MoodEntry::getDate)
                .distinct()
                .count();
        
        int goalPercent = (int) ((uniqueDays / 7.0) * 100);
        model.addAttribute("weeklyGoalPercent", goalPercent);

        // 3. Achievements (Dynamic)
        List<String> achievements = new ArrayList<>();
        if (goalPercent >= 100) achievements.add("7-Day Streak! 🔥");
        if (moodEntryService.getEntriesForUser(user).size() >= 1) achievements.add("First Step Taken 🚀");
        if (recentEntries.stream().anyMatch(e -> "Happy 😊".equals(e.getMood()))) achievements.add("Found Joy 😊");
        
        model.addAttribute("achievements", achievements);

        // 4. Activity Distribution Data
        Map<String, Long> activityCounts = new HashMap<>();
        // Initialize common activities to 0 to ensure chart has labels
        activityCounts.put("Study", 0L);
        activityCounts.put("Exercise", 0L);
        activityCounts.put("Social", 0L);
        activityCounts.put("Rest", 0L);
        activityCounts.put("Other", 0L);

        recentEntries.stream()
            .map(MoodEntry::getActivity)
            .filter(Objects::nonNull)
            .forEach(activity -> activityCounts.put(activity, activityCounts.getOrDefault(activity, 0L) + 1));

        model.addAttribute("activityLabels", activityCounts.keySet());
        model.addAttribute("activityData", activityCounts.values());

        // 5. Assessment History Data
        List<com.example.mentalhealth.model.UserAssessment> assessments = userAssessmentRepository.findByUserIdOrderByAssessmentDateDesc(user.getId().intValue());
        
        // Take latest 5
        List<com.example.mentalhealth.model.UserAssessment> latestAssessments = assessments.stream()
            .limit(5)
            .sorted(Comparator.comparing(com.example.mentalhealth.model.UserAssessment::getAssessmentDate)) // Sort back to ascending for chart (Left to Right)
            .toList();

        List<String> assessmentLabels = new ArrayList<>();
        List<Integer> assessmentScores = new ArrayList<>();
        
        for (com.example.mentalhealth.model.UserAssessment ua : latestAssessments) {
            assessmentLabels.add(ua.getAssessmentDate().toLocalDate().toString()); 
            assessmentScores.add(ua.getTotalScore());
        }

        model.addAttribute("assessmentLabels", assessmentLabels);
        model.addAttribute("assessmentData", assessmentScores);

        List<ProgressInsight> insights = progressInsightService.getInsightsForUser(user);
        model.addAttribute("insights", insights);

        return "progress/dashboard";
    }
}