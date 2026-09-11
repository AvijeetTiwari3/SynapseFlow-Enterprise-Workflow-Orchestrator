import axios from 'axios';
import {
  ApiResponse,
  AuthResponse,
  DashboardMetrics,
  DeadLetterEvent,
  AuditLog,
  Page,
  WorkflowInstance,
  WorkflowTemplate,
  DeadLetterStatus,
  WorkflowStatus
} from '../types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach JWT Bearer Token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('synapse_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: Handle Unauthorized / Token Expiry
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('synapse_token');
      localStorage.removeItem('synapse_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// API Endpoints
export const api = {
  // Auth
  login: async (username: string, password: string): Promise<ApiResponse<AuthResponse>> => {
    const res = await apiClient.post('/auth/login', { username, password });
    return res.data;
  },
  getMe: async (): Promise<ApiResponse<any>> => {
    const res = await apiClient.get('/auth/me');
    return res.data;
  },

  // Metrics
  getMetrics: async (): Promise<ApiResponse<DashboardMetrics>> => {
    const res = await apiClient.get('/metrics/summary');
    return res.data;
  },

  // Workflows
  getTemplates: async (): Promise<ApiResponse<WorkflowTemplate[]>> => {
    const res = await apiClient.get('/workflows/templates');
    return res.data;
  },
  createTemplate: async (data: any): Promise<ApiResponse<WorkflowTemplate>> => {
    const res = await apiClient.post('/workflows/templates', data);
    return res.data;
  },
  getInstances: async (status?: WorkflowStatus, page = 0, size = 20): Promise<ApiResponse<Page<WorkflowInstance>>> => {
    const params: any = { page, size };
    if (status) params.status = status;
    const res = await apiClient.get('/workflows/instances', { params });
    return res.data;
  },
  getInstanceById: async (id: number): Promise<ApiResponse<WorkflowInstance>> => {
    const res = await apiClient.get(`/workflows/instances/${id}`);
    return res.data;
  },
  createInstance: async (data: { templateId: number; title: string; contextPayloadJson?: string }): Promise<ApiResponse<WorkflowInstance>> => {
    const res = await apiClient.post('/workflows/instances', data);
    return res.data;
  },
  transitionWorkflow: async (
    id: number,
    data: { event: string; note?: string; stepExecutionId?: number }
  ): Promise<ApiResponse<WorkflowInstance>> => {
    const res = await apiClient.post(`/workflows/instances/${id}/transition`, data);
    return res.data;
  },

  // Dead Letter Queue
  getDlqEvents: async (status?: DeadLetterStatus, page = 0, size = 20): Promise<ApiResponse<Page<DeadLetterEvent>>> => {
    const params: any = { page, size };
    if (status) params.status = status;
    const res = await apiClient.get('/dlq/events', { params });
    return res.data;
  },
  replayDlqEvent: async (id: number): Promise<ApiResponse<any>> => {
    const res = await apiClient.post(`/dlq/events/${id}/replay`);
    return res.data;
  },
  bulkReplayDlq: async (): Promise<ApiResponse<any>> => {
    const res = await apiClient.post('/dlq/events/bulk-replay');
    return res.data;
  },
  discardDlqEvent: async (id: number): Promise<ApiResponse<any>> => {
    const res = await apiClient.post(`/dlq/events/${id}/discard`);
    return res.data;
  },

  // Audit Logs
  getAuditLogs: async (aggregateType?: string, page = 0, size = 25): Promise<ApiResponse<Page<AuditLog>>> => {
    const params: any = { page, size };
    if (aggregateType) params.aggregateType = aggregateType;
    const res = await apiClient.get('/audit/logs', { params });
    return res.data;
  },
  getAuditTimeline: async (aggregateType: string, aggregateId: number): Promise<ApiResponse<AuditLog[]>> => {
    const res = await apiClient.get(`/audit/timeline/${aggregateType}/${aggregateId}`);
    return res.data;
  },

  // Webhook Simulator
  simulateWebhook: async (source: string, payload: any, signature?: string): Promise<ApiResponse<WorkflowInstance>> => {
    const headers: Record<string, string> = {};
    if (signature) headers['X-Hub-Signature-256'] = signature;
    const res = await apiClient.post(`/integrations/webhooks/${source}`, payload, { headers });
    return res.data;
  }
};
