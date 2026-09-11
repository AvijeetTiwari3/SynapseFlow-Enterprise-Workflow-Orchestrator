package com.google.synapseflow.repository;

import com.google.synapseflow.entity.OutboxEvent;
import com.google.synapseflow.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEventsToDispatch(@Param("now") Instant now, Pageable pageable);

    long countByStatus(OutboxStatus status);
}
