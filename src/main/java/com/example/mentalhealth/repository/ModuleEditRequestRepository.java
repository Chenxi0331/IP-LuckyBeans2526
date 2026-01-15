package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.ModuleEditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModuleEditRequestRepository extends JpaRepository<ModuleEditRequest, Integer> {

    List<ModuleEditRequest> findByRequestedByOrderByRequestedAtDesc(Long requestedBy);

    List<ModuleEditRequest> findByStatusOrderByRequestedAtDesc(String status);

    List<ModuleEditRequest> findByModuleIdOrderByRequestedAtDesc(Integer moduleId);
}