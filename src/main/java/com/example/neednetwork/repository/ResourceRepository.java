
package com.example.neednetwork.repository;

import com.example.neednetwork.entity.Resource;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository
        extends JpaRepository<Resource, Long> {

    // Find resources belonging to a particular user
    List<Resource> findByOwnerEmailIgnoreCase(String ownerEmail);

    // Lock resource during borrowing operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}
