package com.example.mentalhealth.service;

import com.example.mentalhealth.model.MoodEntry;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.MoodEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MoodEntryService {
    @Autowired
    private MoodEntryRepository moodEntryRepository;

    public List<MoodEntry> getEntriesForUser(User user) {
        return moodEntryRepository.findByUser(user);
    }

    public List<MoodEntry> getEntriesForUserByDate(User user, LocalDate date) {
        return moodEntryRepository.findByUserAndDate(user, date);
    }

    public MoodEntry saveEntry(MoodEntry entry) {
        return moodEntryRepository.save(entry);
    }

    public Map<String, Long> getMoodSummary(User user) {
        List<MoodEntry> entries = getEntriesForUser(user);
        if (entries == null)
            return Collections.emptyMap();
        return entries.stream()
                .filter(e -> e.getMood() != null)
                .collect(Collectors.groupingBy(MoodEntry::getMood, Collectors.counting()));
    }
}
