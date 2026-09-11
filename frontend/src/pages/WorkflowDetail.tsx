import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { WorkflowInstance, AuditLog } from '../types';
import { DagViewer } from '../components/DagViewer';
import { 
  ArrowLeft, 
  CheckCircle2, 
  XCircle, 
  Send, 
  AlertTriangle, 
  ScrollText, 
  Code, 
  UserCheck, 
  RefreshCw 
} from 'lucide-react';

interface WorkflowDetailProps {
  workflowId: number;
  onBack: () => void;
}

export const WorkflowDetail: React.FC<WorkflowDetailProps> = ({ workflowId, onBack }) => {
  const [instance, setInstance] = useState<WorkflowInstance | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionNote, setActionNote] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchDetails = async () => {
    try {
      setLoading(true);
      const [instRes, auditRes] = await Promise.all([
        api.getInstanceById(workflowId),
        api.getAuditTimeline('WORKFLOW_INSTANCE', workflowId)
      ]);
      setInstance(instRes.data);
      setAuditLogs(auditRes.data);
    } catch (err) {
      console.error('Failed to load workflow details', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetails();
  }, [workflowId]);

  const handleTransition = async (event: string, stepExecutionId?: number) => {
    try {
      setIsSubmitting(true);
      await api.transitionWorkflow(workflowId, {
        event,
        note: actionNote || undefined,
        stepExecutionId
      });
      setActionNote('');
      await fetchDetails();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Transition failed');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading && !instance) {
    return (
      <div className="flex items-center justify-center h-64 text-blue-600 font-medium">
        <RefreshCw className="h-5 w-5 animate-spin mr-2" />
        Loading Workflow DAG & Compliance History...
      </div>
    );
  }

  if (!instance) {
    return <div>Workflow not found</div>;
  }

  const isCompleted = ['APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED'].includes(instance.status);
  const pendingStep = instance.steps.find((s) => s.status === 'PENDING');

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between bg-white p-6 rounded-xl border border-gray-200 shadow-xs">
        <div className="flex items-center space-x-4">
          <button
            onClick={onBack}
            className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 transition"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-mono text-xs bg-gray-100 px-2 py-0.5 rounded text-gray-700 font-bold">
                WF-{instance.id}
              </span>
              <span className="text-xs bg-blue-100 text-blue-800 px-2 py-0.5 rounded font-medium">
                {instance.category}
              </span>
            </div>
            <h2 className="text-xl font-bold text-gray-900 mt-1">{instance.title}</h2>
          </div>
        </div>

        <button
          onClick={fetchDetails}
          className="p-2 text-gray-400 hover:text-blue-600 hover:bg-gray-50 rounded-lg transition"
        >
          <RefreshCw className="h-4 w-4" />
        </button>
      </div>

      {/* Visual DAG Pipeline */}
      <DagViewer
        steps={instance.steps}
        currentStatus={instance.status}
        currentState={instance.currentState}
      />

      {/* Action Bar (if active) */}
      {!isCompleted && (
        <div className="bg-white p-6 rounded-xl border border-blue-200 bg-blue-50/30 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="font-bold text-sm text-gray-900">Execute FSM Transition & Governance Action</h4>
              <p className="text-xs text-gray-500">
                Current Pending Step: <strong className="text-blue-700">{pendingStep ? pendingStep.stepName : 'Ready to Submit'}</strong>
              </p>
            </div>
            <div className="text-xs text-gray-400 font-mono">
              Optimistic Lock: v{instance.version}
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-3">
            <input
              type="text"
              placeholder="Add compliance notes, justification, or DDL review confirmation..."
              value={actionNote}
              onChange={(e) => setActionNote(e.target.value)}
              className="flex-1 text-xs border border-gray-300 rounded-lg px-3 py-2 bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />

            <div className="flex space-x-2">
              {instance.status === 'DRAFT' ? (
                <button
                  disabled={isSubmitting}
                  onClick={() => handleTransition('SUBMIT')}
                  className="flex items-center space-x-1.5 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-xs disabled:opacity-50"
                >
                  <Send className="h-3.5 w-3.5" />
                  <span>Submit Workflow</span>
                </button>
              ) : (
                <>
                  <button
                    disabled={isSubmitting || !pendingStep}
                    onClick={() => handleTransition('APPROVE', pendingStep?.id)}
                    className="flex items-center space-x-1.5 bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-xs disabled:opacity-50"
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    <span>Approve Step</span>
                  </button>

                  <button
                    disabled={isSubmitting || !pendingStep}
                    onClick={() => handleTransition('REJECT', pendingStep?.id)}
                    className="flex items-center space-x-1.5 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-xs disabled:opacity-50"
                  >
                    <XCircle className="h-3.5 w-3.5" />
                    <span>Reject</span>
                  </button>

                  <button
                    disabled={isSubmitting}
                    onClick={() => handleTransition('ESCALATE', pendingStep?.id)}
                    className="flex items-center space-x-1.5 bg-amber-500 hover:bg-amber-600 text-white px-3 py-2 rounded-lg text-xs font-semibold shadow-xs disabled:opacity-50"
                  >
                    <AlertTriangle className="h-3.5 w-3.5" />
                    <span>Escalate</span>
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Grid: Context Payload JSON & Audit Timeline */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Context Payload */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-3">
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <h4 className="font-bold text-sm text-gray-900 flex items-center space-x-2">
              <Code className="h-4 w-4 text-blue-600" />
              <span>Workflow Context Payload (JSON)</span>
            </h4>
            <span className="text-[11px] font-mono text-gray-400">PostgreSQL JSONB</span>
          </div>

          <pre className="bg-gray-900 text-green-400 p-4 rounded-lg font-mono text-xs overflow-x-auto max-h-72">
            {JSON.stringify(JSON.parse(instance.contextPayloadJson || '{}'), null, 2)}
          </pre>
        </div>

        {/* Audit Timeline */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-3">
          <div className="flex items-center justify-between border-b border-gray-100 pb-3">
            <h4 className="font-bold text-sm text-gray-900 flex items-center space-x-2">
              <ScrollText className="h-4 w-4 text-indigo-600" />
              <span>Compliance Audit Trail</span>
            </h4>
            <span className="text-[11px] bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded font-medium">
              Immutable
            </span>
          </div>

          <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
            {auditLogs.length === 0 ? (
              <p className="text-xs text-gray-400">No audit events recorded yet.</p>
            ) : (
              auditLogs.map((log) => (
                <div key={log.id} className="p-3 bg-gray-50 rounded-lg text-xs space-y-1 border border-gray-100">
                  <div className="flex justify-between items-center">
                    <span className="font-semibold text-gray-900 font-mono">{log.action}</span>
                    <span className="text-gray-400 text-[10px]">
                      {new Date(log.timestamp).toLocaleTimeString()}
                    </span>
                  </div>
                  <div className="flex items-center space-x-2 text-gray-600 text-[11px]">
                    <UserCheck className="h-3 w-3 text-blue-600" />
                    <span>Actor: <strong>{log.actorUsername}</strong></span>
                    <span>• State: {log.previousState} → {log.newState}</span>
                  </div>
                  {log.detailsJson && (
                    <div className="text-gray-500 italic text-[10px] bg-white p-1.5 rounded border border-gray-100">
                      {log.detailsJson}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
