package com.google.synapseflow.repository;

import com.google.synapseflow.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByAggregateType(String aggregateType, Pageable pageable);

    List<AuditLog> findByAggregateTypeAndAggregateIdOrderByTimestampDesc(String aggregateType, Long aggregateId);

    Page<AuditLog> findByActorUsername(String actorUsername, Pageable pageable);
}
