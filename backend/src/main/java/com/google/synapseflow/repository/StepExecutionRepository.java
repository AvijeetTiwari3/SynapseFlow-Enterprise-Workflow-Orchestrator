package com.google.synapseflow.repository;

import com.google.synapseflow.entity.Role;
import com.google.synapseflow.entity.StepExecution;
import com.google.synapseflow.entity.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StepExecutionRepository extends JpaRepository<StepExecution, Long> {

    List<StepExecution> findByWorkflowInstanceIdOrderByStepOrderAsc(Long workflowInstanceId);

    List<StepExecution> findByStatusAndRequiredRole(StepStatus status, Role role);

    @Query("SELECT s FROM StepExecution s WHERE s.status = 'PENDING' AND s.slaDeadline IS NOT NULL AND s.slaDeadline < :now")
    List<StepExecution> findBreachedSlaSteps(@Param("now") Instant now);
}
