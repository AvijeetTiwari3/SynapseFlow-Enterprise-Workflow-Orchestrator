export type Role = 'ROLE_ADMIN' | 'ROLE_APPROVER' | 'ROLE_OPERATOR' | 'ROLE_AUDITOR' | 'ROLE_INTEGRATION_CLIENT';

export type WorkflowStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'IN_PROGRESS'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'
  | 'ESCALATED'
  | 'COMPLETED';

export type StepStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SKIPPED' | 'TIMED_OUT' | 'ESCALATED';

export type OutboxStatus = 'PENDING' | 'DISPATCHING' | 'SENT' | 'FAILED' | 'DEAD_LETTERED';

export type DeadLetterStatus = 'QUARANTINED' | 'REPLAYING' | 'REPLAYED' | 'DISCARDED';

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  fullName: string;
  department: string;
  roles: Role[];
  enabled: boolean;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  username: string;
  email: string;
  fullName: string;
  department: string;
  roles: string[];
  expiresIn: number;
}

export interface WorkflowTemplate {
  id: number;
  templateKey: string;
  name: string;
  description: string;
  category: string;
  stepsDefinitionJson: string;
  version: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StepExecution {
  id: number;
  stepKey: string;
  stepName: string;
  stepOrder: number;
  requiredRole: Role;
  status: StepStatus;
  completedByUsername: string | null;
  comments: string | null;
  slaDeadline: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface WorkflowInstance {
  id: number;
  templateId: number;
  templateKey: string;
  templateName: string;
  category: string;
  title: string;
  initiatorUsername: string;
  status: WorkflowStatus;
  currentState: string;
  contextPayloadJson: string;
  rejectionReason: string | null;
  version: number;
  steps: StepExecution[];
  createdAt: string;
  updatedAt: string;
}

export interface DeadLetterEvent {
  id: number;
  originalOutboxId: number;
  aggregateType: string;
  aggregateId: number;
  eventType: string;
  targetEndpoint: string;
  payloadJson: string;
  failureReason: string;
  stackTrace: string;
  retryAttempts: number;
  status: DeadLetterStatus;
  replayedAt: string | null;
  replayedByUsername: string | null;
  createdAt: string;
}

export interface AuditLog {
  id: number;
  aggregateType: string;
  aggregateId: number;
  actorUsername: string;
  action: string;
  previousState: string | null;
  newState: string | null;
  detailsJson: string | null;
  clientIp: string;
  timestamp: string;
}

export interface DashboardMetrics {
  totalWorkflows: number;
  activeWorkflows: number;
  approvedWorkflows: number;
  rejectedWorkflows: number;
  pendingApprovals: number;
  outboxPending: number;
  outboxSent: number;
  dlqQuarantined: number;
  auditLogCount: number;
  statusDistribution: Record<string, number>;
  systemSuccessRate: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
