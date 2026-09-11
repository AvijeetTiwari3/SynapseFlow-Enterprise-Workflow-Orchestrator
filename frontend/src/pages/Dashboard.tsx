import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { DashboardMetrics, WorkflowInstance } from '../types';
import { MetricCard } from '../components/MetricCard';
import { 
  GitMerge, 
  CheckCircle, 
  XCircle, 
  Clock, 
  Send, 
  AlertOctagon, 
  ScrollText, 
  ShieldCheck, 
  RefreshCw,
  TrendingUp,
  ArrowUpRight
} from 'lucide-react';

interface DashboardProps {
  onSelectWorkflow: (id: number) => void;
  onNavigateToTab: (tab: string) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({ onSelectWorkflow, onNavigateToTab }) => {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [recentWorkflows, setRecentWorkflows] = useState<WorkflowInstance[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const [metricsRes, instancesRes] = await Promise.all([
        api.getMetrics(),
        api.getInstances(undefined, 0, 5)
      ]);
      setMetrics(metricsRes.data);
      setRecentWorkflows(instancesRes.data.content);
    } catch (err) {
      console.error('Failed to load dashboard metrics', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 8000);
    return () => clearInterval(interval);
  }, []);

  if (loading && !metrics) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="flex items-center space-x-2 text-blue-600 font-medium">
          <RefreshCw className="h-5 w-5 animate-spin" />
          <span>Polling Enterprise Telemetry...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex justify-between items-center bg-gradient-to-r from-blue-900 to-indigo-900 text-white p-6 rounded-2xl shadow-sm">
        <div>
          <span className="bg-blue-800 text-blue-200 text-xs font-semibold px-2.5 py-1 rounded-full uppercase tracking-wider">
            Google CorpEng Operations Telemetry
          </span>
          <h2 className="text-2xl font-bold mt-2">Enterprise Workflow & Integration Gateway</h2>
          <p className="text-blue-200 text-sm mt-1">
            Real-time Finite State Machine transitions, Transactional Outbox queues, and Resilience4j circuit health.
          </p>
        </div>
        <button
          onClick={fetchDashboardData}
          className="flex items-center space-x-2 bg-white/10 hover:bg-white/20 px-4 py-2 rounded-xl text-sm font-medium backdrop-blur-sm transition"
        >
          <RefreshCw className="h-4 w-4" />
          <span>Refresh Metrics</span>
        </button>
      </div>

      {/* High-Level Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          title="Total Workflows"
          value={metrics?.totalWorkflows || 0}
          subtitle="All orchestrated instances"
          icon={GitMerge}
          color="blue"
        />
        <MetricCard
          title="Active In-Flight"
          value={metrics?.activeWorkflows || 0}
          subtitle="Awaiting approvals/SLA"
          icon={Clock}
          color="yellow"
        />
        <MetricCard
          title="System SLA Success"
          value={`${metrics?.systemSuccessRate || 100}%`}
          subtitle="FSM convergence rate"
          icon={TrendingUp}
          color="green"
        />
        <MetricCard
          title="DLQ Quarantined"
          value={metrics?.dlqQuarantined || 0}
          subtitle="Failed SaaS dispatches"
          icon={AlertOctagon}
          color="red"
        />
      </div>

      {/* Integration Queue Health Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Outbox & DLQ Telemetry */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <h3 className="font-bold text-gray-900 text-sm flex items-center space-x-2">
              <Send className="h-4 w-4 text-blue-600" />
              <span>Transactional Outbox Health</span>
            </h3>
            <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded-full font-medium">
              Virtual Thread Worker
            </span>
          </div>

          <div className="space-y-3 text-sm">
            <div className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">Pending Outbox Dispatches</span>
              <span className="font-bold text-gray-900">{metrics?.outboxPending || 0}</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">Dispatched Webhook Events</span>
              <span className="font-bold text-green-600">{metrics?.outboxSent || 0}</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">Dead-Letter Queue Events</span>
              <span className={`font-bold ${(metrics?.dlqQuarantined || 0) > 0 ? 'text-red-600 font-bold' : 'text-gray-900'}`}>
                {metrics?.dlqQuarantined || 0}
              </span>
            </div>
          </div>

          <button
            onClick={() => onNavigateToTab('dlq')}
            className="w-full flex items-center justify-center space-x-2 text-xs text-blue-600 hover:text-blue-700 font-semibold pt-1"
          >
            <span>Open Dead-Letter Queue Inspector</span>
            <ArrowUpRight className="h-3.5 w-3.5" />
          </button>
        </div>

        {/* Audit Log Volume */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <h3 className="font-bold text-gray-900 text-sm flex items-center space-x-2">
              <ScrollText className="h-4 w-4 text-indigo-600" />
              <span>Compliance & Audit Stream</span>
            </h3>
            <span className="text-xs bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded-full font-medium">
              Immutable Ledger
            </span>
          </div>

          <div className="space-y-3 text-sm">
            <div className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">Total Audit Events Recorded</span>
              <span className="font-bold text-gray-900">{metrics?.auditLogCount || 0}</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">Security Access Controls</span>
              <span className="font-bold text-green-600">RBAC Enforced</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
              <span className="text-gray-600">Optimistic Locking Guard</span>
              <span className="font-bold text-blue-600">Active (@Version)</span>
            </div>
          </div>

          <button
            onClick={() => onNavigateToTab('audit')}
            className="w-full flex items-center justify-center space-x-2 text-xs text-indigo-600 hover:text-indigo-700 font-semibold pt-1"
          >
            <span>Explore Full Audit Timeline</span>
            <ArrowUpRight className="h-3.5 w-3.5" />
          </button>
        </div>

        {/* System Architecture Blueprint */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <h3 className="font-bold text-gray-900 text-sm flex items-center space-x-2">
              <ShieldCheck className="h-4 w-4 text-green-600" />
              <span>Resilience & Fault-Tolerance</span>
            </h3>
          </div>

          <div className="space-y-2.5 text-xs text-gray-600">
            <div className="flex items-center space-x-2">
              <span className="h-2 w-2 rounded-full bg-green-500"></span>
              <span><strong>Resilience4j:</strong> Exponential backoff retry policy</span>
            </div>
            <div className="flex items-center space-x-2">
              <span className="h-2 w-2 rounded-full bg-green-500"></span>
              <span><strong>HMAC-SHA256:</strong> Cryptographic webhook validation</span>
            </div>
            <div className="flex items-center space-x-2">
              <span className="h-2 w-2 rounded-full bg-green-500"></span>
              <span><strong>Circuit Breaker:</strong> Auto-trip on 50% failure rate</span>
            </div>
            <div className="flex items-center space-x-2">
              <span className="h-2 w-2 rounded-full bg-green-500"></span>
              <span><strong>SLA Watchdog:</strong> 60s background escalation loop</span>
            </div>
          </div>

          <button
            onClick={() => onNavigateToTab('simulator')}
            className="w-full bg-blue-50 hover:bg-blue-100 text-blue-700 py-2 rounded-lg font-semibold text-xs transition"
          >
            Launch Webhook Simulator
          </button>
        </div>
      </div>

      {/* Recent Workflow Activity Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
          <h3 className="font-bold text-gray-900 text-sm">Recent Enterprise Workflows</h3>
          <button
            onClick={() => onNavigateToTab('workflows')}
            className="text-xs text-blue-600 hover:text-blue-800 font-semibold"
          >
            View All Workflows →
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-xs font-semibold text-gray-500 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3 text-left">ID & Title</th>
                <th className="px-6 py-3 text-left">Template</th>
                <th className="px-6 py-3 text-left">Initiator</th>
                <th className="px-6 py-3 text-left">Status</th>
                <th className="px-6 py-3 text-left">Current State</th>
                <th className="px-6 py-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {recentWorkflows.map((inst) => (
                <tr key={inst.id} className="hover:bg-gray-50/80 transition">
                  <td className="px-6 py-4">
                    <div className="font-semibold text-gray-900">{inst.title}</div>
                    <div className="text-xs text-gray-400">WF-{inst.id} • v{inst.version}</div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="bg-gray-100 text-gray-800 text-xs px-2 py-0.5 rounded font-mono">
                      {inst.templateKey}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-gray-600 font-medium">{inst.initiatorUsername}</td>
                  <td className="px-6 py-4">
                    <span
                      className={`px-2.5 py-1 rounded-full text-xs font-bold ${
                        inst.status === 'APPROVED'
                          ? 'bg-green-100 text-green-800'
                          : inst.status === 'REJECTED'
                          ? 'bg-red-100 text-red-800'
                          : inst.status === 'IN_PROGRESS'
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {inst.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-mono text-xs text-gray-700">{inst.currentState}</td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => onSelectWorkflow(inst.id)}
                      className="text-xs bg-gray-100 hover:bg-gray-200 text-gray-800 font-semibold px-3 py-1.5 rounded-lg transition"
                    >
                      Inspect DAG
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
