package com.google.synapseflow.repository;

import com.google.synapseflow.entity.DeadLetterEvent;
import com.google.synapseflow.entity.DeadLetterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {

    Page<DeadLetterEvent> findByStatus(DeadLetterStatus status, Pageable pageable);

    long countByStatus(DeadLetterStatus status);
}
