package com.google.synapseflow.repository;

import com.google.synapseflow.entity.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {
    Optional<WorkflowTemplate> findByTemplateKey(String templateKey);
    List<WorkflowTemplate> findByActiveTrue();
    List<WorkflowTemplate> findByCategory(String category);
}
