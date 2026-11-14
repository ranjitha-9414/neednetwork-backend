
package com.example.neednetwork.repository;

import com.example.neednetwork.entity.ResourceRequest;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceRequestRepository
        extends JpaRepository<ResourceRequest, Long> {

    // Get requests submitted by a particular user
    List<ResourceRequest> findByRequesterEmailIgnoreCase(String email);

    // Get requests for resources belonging to a particular owner
    List<ResourceRequest> findByResource_OwnerEmailIgnoreCase(String email);

    // Check whether a resource has any borrowing-request history
    boolean existsByResource_Id(Long resourceId);

    // Check whether a resource has a request with a particular status
    boolean existsByResource_IdAndStatus(
            Long resourceId,
            String status
    );

    // Retrieve all requests according to urgency priority
    @Query("""
        SELECT r FROM ResourceRequest r
        ORDER BY
        CASE UPPER(r.urgencyLevel)
            WHEN 'EMERGENCY' THEN 1
            WHEN 'URGENT' THEN 2
            WHEN 'NORMAL' THEN 3
            WHEN 'PLANNED' THEN 4
            ELSE 5
        END ASC,
        r.createdAt ASC
        """)
    List<ResourceRequest> findAllByPriority();

    // Lock a borrowing request during approval or rejection
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ResourceRequest r WHERE r.id = :id")
    Optional<ResourceRequest> findByIdForUpdate(@Param("id") Long id);

    // NEW: Count distinct resources successfully shared
    @Query("""
        SELECT COUNT(DISTINCT r.resource.id)
        FROM ResourceRequest r
        WHERE UPPER(r.status) IN ('APPROVED', 'RETURNED')
        """)
    long countDistinctSharedResources();

    // NEW: Count distinct people successfully supported
    @Query("""
        SELECT COUNT(DISTINCT LOWER(r.requesterEmail))
        FROM ResourceRequest r
        WHERE UPPER(r.status) IN ('APPROVED', 'RETURNED')
        """)
    long countDistinctSupportedPeople();
}
