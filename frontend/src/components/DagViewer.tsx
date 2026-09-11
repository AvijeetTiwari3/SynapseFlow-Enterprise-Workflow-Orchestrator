import React from 'react';
import { StepExecution, WorkflowStatus } from '../types';
import { CheckCircle2, Clock, XCircle, AlertTriangle, ArrowRight, UserCheck } from 'lucide-react';

interface DagViewerProps {
  steps: StepExecution[];
  currentStatus: WorkflowStatus;
  currentState: string;
}

export const DagViewer: React.FC<DagViewerProps> = ({ steps, currentStatus, currentState }) => {
  return (
    <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h4 className="text-sm font-bold text-gray-900 uppercase tracking-wide">FSM State Machine & DAG Execution Pipeline</h4>
          <p className="text-xs text-gray-500">Live multi-tier approval progression with optimistic concurrency & SLA enforcement</p>
        </div>
        <span
          className={`px-3 py-1 rounded-full text-xs font-bold ${
            currentStatus === 'APPROVED' || currentStatus === 'COMPLETED'
              ? 'bg-green-100 text-green-800 border border-green-200'
              : currentStatus === 'REJECTED'
              ? 'bg-red-100 text-red-800 border border-red-200'
              : currentStatus === 'ESCALATED'
              ? 'bg-amber-100 text-amber-800 border border-amber-200'
              : 'bg-blue-100 text-blue-800 border border-blue-200'
          }`}
        >
          {currentStatus}
        </span>
      </div>

      <div className="flex flex-col md:flex-row items-center justify-between gap-4 py-4 overflow-x-auto">
        {/* START / DRAFT Node */}
        <div className="flex items-center">
          <div className="flex flex-col items-center">
            <div className="h-10 w-10 rounded-full bg-gray-100 border-2 border-gray-400 flex items-center justify-center font-bold text-xs text-gray-700">
              INIT
            </div>
            <span className="text-xs font-medium text-gray-600 mt-1">Submitted</span>
          </div>
          <ArrowRight className="h-4 w-4 text-gray-400 mx-2 hidden md:block" />
        </div>

        {/* Steps Nodes */}
        {steps.map((step, idx) => {
          const isApproved = step.status === 'APPROVED';
          const isRejected = step.status === 'REJECTED';
          const isEscalated = step.status === 'ESCALATED';
          const isPending = step.status === 'PENDING';

          return (
            <React.Fragment key={step.id}>
              <div className="flex-1 min-w-[200px]">
                <div
                  className={`p-4 rounded-xl border transition-all ${
                    isApproved
                      ? 'bg-green-50/50 border-green-300'
                      : isRejected
                      ? 'bg-red-50/50 border-red-300'
                      : isEscalated
                      ? 'bg-amber-50/50 border-amber-300 animate-pulse'
                      : 'bg-white border-blue-200 shadow-xs'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-bold text-gray-500">Tier {step.stepOrder}</span>
                    {isApproved && <CheckCircle2 className="h-4 w-4 text-green-600" />}
                    {isRejected && <XCircle className="h-4 w-4 text-red-600" />}
                    {isEscalated && <AlertTriangle className="h-4 w-4 text-amber-600" />}
                    {isPending && <Clock className="h-4 w-4 text-blue-600 animate-spin" />}
                  </div>

                  <div className="font-semibold text-xs text-gray-900 line-clamp-1">{step.stepName}</div>
                  
                  <div className="mt-2 text-[11px] text-gray-600 space-y-1">
                    <div className="flex items-center space-x-1">
                      <span className="text-gray-400">Required:</span>
                      <span className="bg-gray-100 text-gray-700 px-1.5 py-0.5 rounded font-mono text-[10px]">
                        {step.requiredRole.replace('ROLE_', '')}
                      </span>
                    </div>

                    {step.completedByUsername && (
                      <div className="flex items-center space-x-1 text-green-700">
                        <UserCheck className="h-3 w-3" />
                        <span className="font-medium">{step.completedByUsername}</span>
                      </div>
                    )}

                    {step.comments && (
                      <div className="italic text-gray-500 bg-white/70 p-1.5 rounded border border-gray-100 text-[10px]">
                        "{step.comments}"
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {idx < steps.length - 1 && (
                <ArrowRight className="h-4 w-4 text-gray-400 mx-1 hidden md:block shrink-0" />
              )}
            </React.Fragment>
          );
        })}

        {/* TERMINAL / OUTCOME Node */}
        <div className="flex items-center">
          <ArrowRight className="h-4 w-4 text-gray-400 mx-2 hidden md:block" />
          <div className="flex flex-col items-center">
            <div
              className={`h-10 w-10 rounded-full border-2 flex items-center justify-center font-bold text-xs ${
                currentStatus === 'APPROVED' || currentStatus === 'COMPLETED'
                  ? 'bg-green-600 border-green-600 text-white'
                  : currentStatus === 'REJECTED'
                  ? 'bg-red-600 border-red-600 text-white'
                  : 'bg-gray-100 border-gray-300 text-gray-400'
              }`}
            >
              {currentStatus === 'APPROVED' ? 'DONE' : currentStatus === 'REJECTED' ? 'REJ' : 'END'}
            </div>
            <span className="text-xs font-medium text-gray-600 mt-1">Outbox Dispatch</span>
          </div>
        </div>
      </div>
    </div>
  );
};
