package com.example.mentalhealth.repository;

import com.example.mentalhealth.model.Role;
import com.example.mentalhealth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email); // ADDED

    User findByUsername(String username);

    boolean existsByEmail(String email); // ADDED

    List<User> findByRole(Role role);
}