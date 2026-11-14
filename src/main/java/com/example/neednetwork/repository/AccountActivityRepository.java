
package com.example.neednetwork.repository;

import com.example.neednetwork.entity.AccountActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountActivityRepository
        extends JpaRepository<AccountActivity, Long> {

    // Get activity history for one user
    List<AccountActivity> findByTargetUserIdOrderByCreatedAtDesc(
        Long targetUserId
    );

    // Get activity history for all users
    List<AccountActivity> findAllByOrderByCreatedAtDesc();
}
