package com.google.synapseflow.config;

import com.google.synapseflow.dto.WorkflowDto;
import com.google.synapseflow.entity.*;
import com.google.synapseflow.integration.DeadLetterQueueService;
import com.google.synapseflow.repository.*;
import com.google.synapseflow.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final OutboxEventRepository outboxRepository;
    private final DeadLetterQueueService dlqService;
    private final WorkflowService workflowService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            WorkflowTemplateRepository templateRepository,
            OutboxEventRepository outboxRepository,
            DeadLetterQueueService dlqService,
            WorkflowService workflowService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
        this.outboxRepository = outboxRepository;
        this.dlqService = dlqService;
        this.workflowService = workflowService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Initializing SynapseFlow database with default enterprise roles, users, and workflow templates...");

        // 1. Seed Users
        User admin = new User(
                "admin",
                passwordEncoder.encode("admin123"),
                "admin@google.corp",
                "Google Corporate Admin",
                "Corporate Engineering",
                Set.of(Role.ROLE_ADMIN, Role.ROLE_OPERATOR, Role.ROLE_AUDITOR, Role.ROLE_APPROVER)
        );
        userRepository.save(admin);

        User managerJane = new User(
                "manager_jane",
                passwordEncoder.encode("password123"),
                "jane.doe@google.corp",
                "Jane Doe (Engineering Manager)",
                "Search Infrastructure",
                Set.of(Role.ROLE_APPROVER)
        );
        userRepository.save(managerJane);

        User secopsAlex = new User(
                "secops_alex",
                passwordEncoder.encode("password123"),
                "alex.k@google.corp",
                "Alex K (Security Operations Lead)",
                "Security & Privacy",
                Set.of(Role.ROLE_APPROVER, Role.ROLE_OPERATOR)
        );
        userRepository.save(secopsAlex);

        User auditorBob = new User(
                "auditor_bob",
                passwordEncoder.encode("password123"),
                "bob.m@google.corp",
                "Bob Miller (Compliance Auditor)",
                "Enterprise Compliance",
                Set.of(Role.ROLE_AUDITOR)
        );
        userRepository.save(auditorBob);

        // 2. Seed Workflow Templates
        String gcpStepsJson = """
        [
          {"key": "PEER_REVIEW", "name": "L6+ Peer Technical Review", "requiredRole": "ROLE_APPROVER", "slaHours": 12},
          {"key": "SECOPS_SIGN_OFF", "name": "SecOps Risk & IAM Assessment", "requiredRole": "ROLE_OPERATOR", "slaHours": 24},
          {"key": "VP_AUTHORIZATION", "name": "Director / VP Production Approval", "requiredRole": "ROLE_ADMIN", "slaHours": 48}
        ]
        """;
        WorkflowTemplate gcpTemplate = new WorkflowTemplate(
                "GCP_PROD_ACCESS_V1",
                "GCP Production Cluster Elevated Access Request",
                "Multi-tiered approval workflow for elevated IAM privileges and production Kubernetes/Spanner access.",
                "Security & Infrastructure",
                gcpStepsJson
        );
        templateRepository.save(gcpTemplate);

        String vendorStepsJson = """
        [
          {"key": "LEGAL_REVIEW", "name": "Corporate Legal & DPA Review", "requiredRole": "ROLE_AUDITOR", "slaHours": 48},
          {"key": "INFOSEC_AUDIT", "name": "SOC2 & FedRAMP Infosec Audit", "requiredRole": "ROLE_OPERATOR", "slaHours": 72},
          {"key": "FINANCE_SIGNOFF", "name": "FinOps Budget Allocation", "requiredRole": "ROLE_ADMIN", "slaHours": 24}
        ]
        """;
        WorkflowTemplate vendorTemplate = new WorkflowTemplate(
                "VENDOR_CONTRACT_APPROVAL_V1",
                "Third-Party SaaS Vendor Onboarding & Security Risk Assessment",
                "Enterprise compliance verification and SLA negotiation pipeline for external SaaS vendor integration.",
                "Enterprise Operations",
                vendorStepsJson
        );
        templateRepository.save(vendorTemplate);

        String quotaStepsJson = """
        [
          {"key": "CAPACITY_PLANNING", "name": "Compute Engine Core Capacity Validation", "requiredRole": "ROLE_APPROVER", "slaHours": 24},
          {"key": "FINOPS_CHARGEBACK", "name": "Departmental Chargeback Confirmation", "requiredRole": "ROLE_ADMIN", "slaHours": 24}
        ]
        """;
        WorkflowTemplate quotaTemplate = new WorkflowTemplate(
                "CLOUD_QUOTA_EXPANSION_V1",
                "Cloud Infrastructure Quota & GPU Cluster Expansion",
                "Automated validation and resource reservation for large-scale GPU/TPU training clusters.",
                "Cloud Compute",
                quotaStepsJson
        );
        templateRepository.save(quotaTemplate);

        // 3. Seed Sample Workflow Instances
        WorkflowDto.WorkflowInstanceResponse instance1 = workflowService.createInstance(
                new WorkflowDto.CreateInstanceRequest(
                        gcpTemplate.getId(),
                        "Urgent: Production Spanner DB Schema Migration Access for Ads Core",
                        "{\"cluster\": \"europe-west1-prod-04\", \"role\": \"roles/spanner.databaseAdmin\", \"ticketId\": \"SO-9824\"}"
                ),
                "manager_jane"
        );
        workflowService.transitionWorkflow(
                instance1.id(),
                new WorkflowDto.TransitionRequest("SUBMIT", "Submitted for technical peer validation", null),
                "manager_jane"
        );
        workflowService.transitionWorkflow(
                instance1.id(),
                new WorkflowDto.TransitionRequest("APPROVE", "L6 Review Completed - Schema DDL checked and backward compatible", null),
                "manager_jane"
        );

        WorkflowDto.WorkflowInstanceResponse instance2 = workflowService.createInstance(
                new WorkflowDto.CreateInstanceRequest(
                        vendorTemplate.getId(),
                        "Datadog Enterprise APM SaaS Integration for Corporate IT Systems",
                        "{\"vendor\": \"Datadog Inc\", \"tier\": \"Tier-1 SaaS\", \"annualSpend\": \"$180,000\", \"dataClass\": \"Confidential\"}"
                ),
                "admin"
        );
        workflowService.transitionWorkflow(
                instance2.id(),
                new WorkflowDto.TransitionRequest("SUBMIT", "Submitted to legal and infosec queues", null),
                "admin"
        );

        // 4. Seed a Sample Quarantined DLQ Event to demonstrate Replay Mechanism
        OutboxEvent failedOutbox = new OutboxEvent(
                "WORKFLOW_INSTANCE",
                999L,
                "VENDOR.INTEGRATION.FAILED",
                "https://api.vendor-mock-unreachable.corp/webhooks/v1",
                "{\"event\":\"VENDOR_ONBOARDED\",\"vendorId\":\"DATADOG-982\",\"status\":\"SYNC_ERROR\"}"
        );
        failedOutbox.setStatus(OutboxStatus.DEAD_LETTERED);
        failedOutbox.setRetryCount(5);
        failedOutbox.setLastError("Connection timed out after 5000ms: ConnectException to remote SaaS host");
        outboxRepository.save(failedOutbox);

        dlqService.quarantineEvent(
                failedOutbox,
                "HTTP 504 Gateway Timeout: Vendor endpoint unreachable after 5 exponential backoff retries",
                "java.net.ConnectException: Connection refused to api.vendor-mock-unreachable.corp:443\n\tat com.google.synapseflow.integration.OutboxDispatcher.sendHttpRequest(OutboxDispatcher.java:82)"
        );

        log.info("SynapseFlow database initialization complete. Ready for enterprise traffic.");
    }
}
