import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { AuditLog } from '../types';
import { ScrollText, RefreshCw, UserCheck, ShieldAlert, Filter } from 'lucide-react';

export const AuditExplorer: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [aggregateFilter, setAggregateFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const res = await api.getAuditLogs(aggregateFilter || undefined, page, 25);
      setLogs(res.data.content);
    } catch (err) {
      console.error('Failed to load audit logs', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [aggregateFilter, page]);

  return (
    <div className="space-y-6">
      {/* Header & Filter */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-xl border border-gray-200 shadow-xs">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-xl font-bold text-gray-900">Immutable Compliance Audit Trail</h2>
            <span className="bg-indigo-100 text-indigo-800 text-xs px-2 py-0.5 rounded-full font-bold">
              Append-Only Ledger
            </span>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Complete chronological record of all state transitions, security overrides, and integration triggers.
          </p>
        </div>

        <div className="flex items-center space-x-3 w-full sm:w-auto">
          <div className="flex items-center space-x-2 bg-gray-50 px-3 py-1.5 rounded-lg border border-gray-200 text-xs">
            <Filter className="h-3.5 w-3.5 text-gray-400" />
            <select
              value={aggregateFilter}
              onChange={(e) => {
                setAggregateFilter(e.target.value);
                setPage(0);
              }}
              className="bg-transparent border-none text-gray-700 font-medium focus:ring-0 cursor-pointer"
            >
              <option value="">All Aggregates</option>
              <option value="WORKFLOW_INSTANCE">WORKFLOW_INSTANCE</option>
              <option value="WORKFLOW_TEMPLATE">WORKFLOW_TEMPLATE</option>
              <option value="DEAD_LETTER_EVENT">DEAD_LETTER_EVENT</option>
              <option value="WEBHOOK_INGESTION">WEBHOOK_INGESTION</option>
            </select>
          </div>

          <button
            onClick={fetchLogs}
            className="p-2 text-gray-500 hover:text-blue-600 hover:bg-gray-100 rounded-lg transition"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-xs">
            <thead className="bg-gray-50 font-semibold text-gray-500 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3 text-left">Timestamp</th>
                <th className="px-6 py-3 text-left">Actor</th>
                <th className="px-6 py-3 text-left">Action</th>
                <th className="px-6 py-3 text-left">Aggregate</th>
                <th className="px-6 py-3 text-left">State Mutation</th>
                <th className="px-6 py-3 text-left">Details / Justification</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {logs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-400">
                    No compliance audit logs found.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50/80 transition">
                    <td className="px-6 py-3 text-gray-500 font-mono">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                    <td className="px-6 py-3">
                      <div className="flex items-center space-x-1.5 font-semibold text-gray-900">
                        <UserCheck className="h-3.5 w-3.5 text-blue-600" />
                        <span>{log.actorUsername}</span>
                      </div>
                      <span className="text-[10px] text-gray-400 font-mono">IP: {log.clientIp}</span>
                    </td>
                    <td className="px-6 py-3">
                      <span className="bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded font-mono font-bold">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-6 py-3">
                      <span className="text-gray-800 font-semibold">{log.aggregateType}</span>
                      <span className="text-gray-400 ml-1">#{log.aggregateId}</span>
                    </td>
                    <td className="px-6 py-3">
                      <div className="font-mono text-[11px] text-gray-600">
                        <span className="text-gray-400">{log.previousState || 'NONE'}</span>
                        <span className="mx-1 text-gray-400">→</span>
                        <span className="font-bold text-gray-900">{log.newState || 'NONE'}</span>
                      </div>
                    </td>
                    <td className="px-6 py-3 text-gray-600 max-w-xs truncate">
                      {log.detailsJson ? (
                        <span className="italic bg-gray-50 px-2 py-1 rounded border border-gray-100">
                          {log.detailsJson}
                        </span>
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
