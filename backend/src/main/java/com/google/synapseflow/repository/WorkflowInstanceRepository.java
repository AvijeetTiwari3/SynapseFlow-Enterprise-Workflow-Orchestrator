package com.google.synapseflow.repository;

import com.google.synapseflow.entity.WorkflowInstance;
import com.google.synapseflow.entity.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    Page<WorkflowInstance> findByStatus(WorkflowStatus status, Pageable pageable);

    Page<WorkflowInstance> findByInitiatorUsername(String initiatorUsername, Pageable pageable);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.status = :status AND w.updatedAt < :cutoff")
    List<WorkflowInstance> findStaleInstances(@Param("status") WorkflowStatus status, @Param("cutoff") Instant cutoff);

    long countByStatus(WorkflowStatus status);

    @Query("SELECT w.status, COUNT(w) FROM WorkflowInstance w GROUP BY w.status")
    List<Object[]> countGroupedByStatus();
}
