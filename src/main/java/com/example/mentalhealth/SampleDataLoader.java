package com.example.mentalhealth;

import com.example.mentalhealth.model.*;
import com.example.mentalhealth.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class SampleDataLoader {
    @Bean
    CommandLineRunner loadSampleData(UserRepository userRepo,
            MoodEntryRepository moodRepo,
            ProgressInsightRepository insightRepo,
            EducationalResourceRepository resourceRepo,
            AwarenessCampaignRepository campaignRepo) {
        return args -> {
            // Sample user
            User user = userRepo.findByEmail("testuser@test.com").orElse(null);
            if (user == null)
                return;

            // Sample Mood Entries - Only add if empty
            if (moodRepo.count() == 0) {
                MoodEntry mood1 = new MoodEntry();
                mood1.setUser(user);
                mood1.setDate(LocalDate.now().minusDays(2));
                mood1.setMood("Happy 😊");
                mood1.setNote("Had a good day");
                moodRepo.save(mood1);

                MoodEntry mood2 = new MoodEntry();
                mood2.setUser(user);
                mood2.setDate(LocalDate.now().minusDays(1));
                mood2.setMood("Sad 😢");
                mood2.setNote("Tough exam");
                moodRepo.save(mood2);

                MoodEntry mood3 = new MoodEntry();
                mood3.setUser(user);
                mood3.setDate(LocalDate.now());
                mood3.setMood("Excited 🤩");
                mood3.setNote("Looking forward to holidays");
                moodRepo.save(mood3);
            }

            // Sample Progress Insights - Only add if empty
            if (insightRepo.count() == 0) {
                ProgressInsight insight1 = new ProgressInsight();
                insight1.setUser(user);
                insight1.setSummary("Improvement");
                insight1.setDetails("Mood has improved over the week.");
                insightRepo.save(insight1);

                ProgressInsight insight2 = new ProgressInsight();
                insight2.setUser(user);
                insight2.setSummary("Needs Attention");
                insight2.setDetails("Low mood detected on exam day.");
                insightRepo.save(insight2);
            }

            // Sample Educational Resources - Only add if empty
            if (resourceRepo.count() == 0) {
                EducationalResource resource1 = new EducationalResource();
                resource1.setTitle("Coping with Stress");
                resource1.setDescription("Tips for managing stress.");
                resource1.setUrl("https://example.com/stress");
                resource1.setType("article");
                resource1.setCategory("Stress");
                resourceRepo.save(resource1);

                EducationalResource resource2 = new EducationalResource();
                resource2.setTitle("Mindfulness Basics");
                resource2.setDescription("Introduction to mindfulness.");
                resource2.setUrl("https://example.com/mindfulness");
                resource2.setType("guide");
                resource2.setCategory("Mindfulness");
                resourceRepo.save(resource2);

                EducationalResource resource3 = new EducationalResource();
                resource3.setTitle("Mental Health Podcast");
                resource3.setDescription("Listen to experts discuss mental health topics.");
                resource3.setUrl("https://example.com/podcast");
                resource3.setType("podcast");
                resource3.setCategory("Anxiety");
                resourceRepo.save(resource3);

                EducationalResource resource4 = new EducationalResource();
                resource4.setTitle("Guided Meditation Video");
                resource4.setDescription("Watch and follow a guided meditation session.");
                resource4.setUrl("https://example.com/meditation-video");
                resource4.setType("video");
                resource4.setCategory("Mindfulness");
                resourceRepo.save(resource4);

                EducationalResource resource5 = new EducationalResource();
                resource5.setTitle("Self-care Guide");
                resource5.setDescription("Downloadable guide for daily self-care routines.");
                resource5.setUrl("https://example.com/selfcare-guide");
                resource5.setType("guide");
                resource5.setCategory("Depression");
                resourceRepo.save(resource5);
            }

            // Sample Awareness Campaigns - Only add if empty
            if (campaignRepo.count() == 0) {
                AwarenessCampaign campaign1 = new AwarenessCampaign();
                campaign1.setTitle("Mental Health Week");
                campaign1.setDescription("Join our campaign to raise awareness.");
                campaign1.setStatus("Active");
                campaign1.setCategory("General");
                campaign1.setStartDate(LocalDate.now());
                campaign1.setEndDate(LocalDate.now().plusDays(7));
                campaignRepo.save(campaign1);

                AwarenessCampaign campaign2 = new AwarenessCampaign();
                campaign2.setTitle("Stress Awareness Month");
                campaign2.setDescription("Participate in events all month.");
                campaign2.setStatus("Upcoming");
                campaign2.setCategory("Stress");
                campaign2.setStartDate(LocalDate.now().plusDays(10));
                campaign2.setEndDate(LocalDate.now().plusDays(40));
                campaignRepo.save(campaign2);

                AwarenessCampaign campaign3 = new AwarenessCampaign();
                campaign3.setTitle("World Suicide Prevention Day");
                campaign3.setDescription("Events and resources to support suicide prevention.");
                campaign3.setStatus("Ended");
                campaign3.setCategory("Prevention");
                campaign3.setStartDate(LocalDate.now().minusDays(10));
                campaign3.setEndDate(LocalDate.now().minusDays(1));
                campaignRepo.save(campaign3);

                AwarenessCampaign campaign4 = new AwarenessCampaign();
                campaign4.setTitle("Youth Mental Wellness Drive");
                campaign4.setDescription("Special activities for youth mental wellness.");
                campaign4.setStatus("Active");
                campaign4.setCategory("Youth");
                campaign4.setStartDate(LocalDate.now());
                campaign4.setEndDate(LocalDate.now().plusDays(14));
                campaignRepo.save(campaign4);
            }
        };
    }
}
