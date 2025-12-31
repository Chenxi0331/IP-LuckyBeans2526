package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.AwarenessCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface AwarenessCampaignRepository extends JpaRepository<AwarenessCampaign, Long> {

    // 1. Find campaigns by a specific category (e.g., "Anxiety", "Depression")
    List<AwarenessCampaign> findByCategory(String category);

    // 2. Find campaigns that are currently active based on dates
    List<AwarenessCampaign> findByStartDateBeforeAndEndDateAfter(LocalDate today, LocalDate todayRepeat);

    // 3. Search for campaigns with a specific keyword in the title
    List<AwarenessCampaign> findByTitleContainingIgnoreCase(String keyword);

    // 4. Find the most recent campaigns
    List<AwarenessCampaign> findTop3ByOrderByStartDateDesc();
}