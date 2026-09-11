import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { WorkflowInstance, WorkflowStatus } from '../types';
import { Plus, RefreshCw, Filter, ArrowRight, Clock, CheckCircle2, XCircle, AlertTriangle } from 'lucide-react';

interface WorkflowsProps {
  onSelectWorkflow: (id: number) => void;
  onOpenCreateModal: () => void;
}

export const Workflows: React.FC<WorkflowsProps> = ({ onSelectWorkflow, onOpenCreateModal }) => {
  const [workflows, setWorkflows] = useState<WorkflowInstance[]>([]);
  const [statusFilter, setStatusFilter] = useState<WorkflowStatus | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchWorkflows = async () => {
    try {
      setLoading(true);
      const res = await api.getInstances(statusFilter, page, 10);
      setWorkflows(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      console.error('Failed to load workflows', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWorkflows();
  }, [statusFilter, page]);

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-xl border border-gray-200 shadow-xs">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Enterprise Workflow Instances</h2>
          <p className="text-xs text-gray-500 mt-1">
            Manage multi-tier approvals, state progression, and SLA compliance.
          </p>
        </div>

        <div className="flex items-center space-x-3 w-full sm:w-auto">
          {/* Status Filter */}
          <div className="flex items-center space-x-2 bg-gray-50 px-3 py-1.5 rounded-lg border border-gray-200 text-xs">
            <Filter className="h-3.5 w-3.5 text-gray-400" />
            <select
              value={statusFilter || ''}
              onChange={(e) => {
                setStatusFilter(e.target.value ? (e.target.value as WorkflowStatus) : undefined);
                setPage(0);
              }}
              className="bg-transparent border-none text-gray-700 font-medium focus:ring-0 cursor-pointer"
            >
              <option value="">All Statuses</option>
              <option value="DRAFT">DRAFT</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="PENDING_APPROVAL">PENDING_APPROVAL</option>
              <option value="APPROVED">APPROVED</option>
              <option value="REJECTED">REJECTED</option>
              <option value="ESCALATED">ESCALATED</option>
            </select>
          </div>

          <button
            onClick={fetchWorkflows}
            className="p-2 text-gray-500 hover:text-blue-600 hover:bg-gray-100 rounded-lg transition"
            title="Refresh"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={onOpenCreateModal}
            className="flex items-center space-x-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-xs transition"
          >
            <Plus className="h-4 w-4" />
            <span>Launch Workflow</span>
          </button>
        </div>
      </div>

      {/* Workflows Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-xs font-semibold text-gray-500 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3 text-left">Workflow ID & Title</th>
                <th className="px-6 py-3 text-left">Category / Template</th>
                <th className="px-6 py-3 text-left">Initiator</th>
                <th className="px-6 py-3 text-left">Approval Progress</th>
                <th className="px-6 py-3 text-left">Status</th>
                <th className="px-6 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {workflows.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-400 text-sm">
                    No workflow instances found matching current filter.
                  </td>
                </tr>
              ) : (
                workflows.map((inst) => {
                  const completedSteps = inst.steps.filter((s) => s.status === 'APPROVED').length;
                  const totalSteps = inst.steps.length;

                  return (
                    <tr key={inst.id} className="hover:bg-gray-50/80 transition">
                      <td className="px-6 py-4">
                        <div className="font-semibold text-gray-900">{inst.title}</div>
                        <div className="text-xs text-gray-400 font-mono mt-0.5">
                          ID: WF-{inst.id} • Optimistic Lock: v{inst.version}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-xs font-semibold text-gray-800">{inst.category}</div>
                        <span className="text-[11px] text-gray-500 font-mono">{inst.templateKey}</span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="text-xs font-medium text-gray-800">{inst.initiatorUsername}</div>
                        <div className="text-[11px] text-gray-400">{new Date(inst.createdAt).toLocaleDateString()}</div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="w-36">
                          <div className="flex justify-between text-xs text-gray-500 mb-1">
                            <span>Steps</span>
                            <span className="font-semibold">{completedSteps} / {totalSteps}</span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                            <div
                              className={`h-1.5 rounded-full ${
                                inst.status === 'APPROVED' ? 'bg-green-600' : 'bg-blue-600'
                              }`}
                              style={{ width: `${totalSteps > 0 ? (completedSteps / totalSteps) * 100 : 0}%` }}
                            ></div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-xs font-bold ${
                            inst.status === 'APPROVED'
                              ? 'bg-green-100 text-green-800'
                              : inst.status === 'REJECTED'
                              ? 'bg-red-100 text-red-800'
                              : inst.status === 'ESCALATED'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-blue-100 text-blue-800'
                          }`}
                        >
                          {inst.status === 'APPROVED' && <CheckCircle2 className="h-3 w-3 mr-1" />}
                          {inst.status === 'REJECTED' && <XCircle className="h-3 w-3 mr-1" />}
                          {inst.status === 'ESCALATED' && <AlertTriangle className="h-3 w-3 mr-1" />}
                          {inst.status === 'IN_PROGRESS' && <Clock className="h-3 w-3 mr-1" />}
                          <span>{inst.status}</span>
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          onClick={() => onSelectWorkflow(inst.id)}
                          className="inline-flex items-center space-x-1 text-xs bg-blue-50 hover:bg-blue-100 text-blue-700 font-semibold px-3 py-1.5 rounded-lg transition"
                        >
                          <span>Manage DAG</span>
                          <ArrowRight className="h-3.5 w-3.5" />
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-6 py-3 bg-gray-50 border-t border-gray-200 flex justify-between items-center text-xs text-gray-500">
            <span>Page {page + 1} of {totalPages}</span>
            <div className="flex space-x-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1 bg-white border border-gray-300 rounded font-medium disabled:opacity-50"
              >
                Previous
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1 bg-white border border-gray-300 rounded font-medium disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
