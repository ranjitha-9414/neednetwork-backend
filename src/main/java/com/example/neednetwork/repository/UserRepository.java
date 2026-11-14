
package com.example.neednetwork.repository;

import com.example.neednetwork.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user using email during login and verification
    Optional<User> findByEmailIgnoreCase(String email);

    // Check whether an email is already registered
    boolean existsByEmailIgnoreCase(String email);

    // Search users by name or email
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName,
            String email
    );

    // Count active users by role (VOLUNTEER or NGO)
    long countByRoleAndActiveTrue(User.Role role);
}
