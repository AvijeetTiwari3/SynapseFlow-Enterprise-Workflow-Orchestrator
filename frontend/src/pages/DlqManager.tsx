import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { DeadLetterEvent, DeadLetterStatus } from '../types';
import { 
  AlertOctagon, 
  RotateCw, 
  Trash2, 
  Eye, 
  CheckCircle, 
  RefreshCw, 
  X, 
  Zap, 
  FileCode 
} from 'lucide-react';

export const DlqManager: React.FC = () => {
  const [events, setEvents] = useState<DeadLetterEvent[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<DeadLetterEvent | null>(null);
  const [statusFilter, setStatusFilter] = useState<DeadLetterStatus | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [replayingId, setReplayingId] = useState<number | null>(null);
  const [bulkReplaying, setBulkReplaying] = useState(false);

  const fetchDlqEvents = async () => {
    try {
      setLoading(true);
      const res = await api.getDlqEvents(statusFilter);
      setEvents(res.data.content);
    } catch (err) {
      console.error('Failed to load DLQ events', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDlqEvents();
  }, [statusFilter]);

  const handleReplay = async (id: number) => {
    try {
      setReplayingId(id);
      await api.replayDlqEvent(id);
      await fetchDlqEvents();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Replay failed');
    } finally {
      setReplayingId(null);
    }
  };

  const handleBulkReplay = async () => {
    try {
      setBulkReplaying(true);
      await api.bulkReplayDlq();
      await fetchDlqEvents();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Bulk replay failed');
    } finally {
      setBulkReplaying(false);
    }
  };

  const handleDiscard = async (id: number) => {
    if (!confirm('Are you sure you want to discard this DLQ event without replaying?')) return;
    try {
      await api.discardDlqEvent(id);
      await fetchDlqEvents();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Discard failed');
    }
  };

  const quarantinedCount = events.filter((e) => e.status === 'QUARANTINED').length;

  return (
    <div className="space-y-6">
      {/* Top Banner & Bulk Actions */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-xl border border-gray-200 shadow-xs">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-xl font-bold text-gray-900">Dead-Letter Queue (DLQ) Inspector</h2>
            <span className="bg-red-100 text-red-800 text-xs px-2 py-0.5 rounded-full font-bold">
              Resilience4j Quarantined
            </span>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Quarantine area for failed 3rd-party SaaS webhook dispatches after exponential backoff exhaustion.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={fetchDlqEvents}
            className="p-2 text-gray-500 hover:text-blue-600 hover:bg-gray-100 rounded-lg transition"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            disabled={quarantinedCount === 0 || bulkReplaying}
            onClick={handleBulkReplay}
            className="flex items-center space-x-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg text-xs font-semibold shadow-xs disabled:opacity-50 transition"
          >
            <Zap className="h-4 w-4" />
            <span>{bulkReplaying ? 'Replaying...' : `Bulk Replay All (${quarantinedCount})`}</span>
          </button>
        </div>
      </div>

      {/* Events Table */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50 text-xs font-semibold text-gray-500 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-3 text-left">DLQ ID & Target Endpoint</th>
                <th className="px-6 py-3 text-left">Event Type</th>
                <th className="px-6 py-3 text-left">Failure Reason</th>
                <th className="px-6 py-3 text-left">Attempts</th>
                <th className="px-6 py-3 text-left">Status</th>
                <th className="px-6 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white text-xs">
              {events.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-400">
                    <CheckCircle className="h-8 w-8 text-green-500 mx-auto mb-2" />
                    No quarantined dead-letter events. All 3rd-party outbox dispatches healthy!
                  </td>
                </tr>
              ) : (
                events.map((dlq) => (
                  <tr key={dlq.id} className="hover:bg-gray-50/80 transition">
                    <td className="px-6 py-4">
                      <div className="font-mono font-bold text-gray-900">DLQ-{dlq.id}</div>
                      <div className="text-[11px] text-gray-500 truncate max-w-xs font-mono">{dlq.targetEndpoint}</div>
                    </td>
                    <td className="px-6 py-4 font-mono font-semibold text-gray-800">{dlq.eventType}</td>
                    <td className="px-6 py-4">
                      <span className="text-red-700 bg-red-50 p-1.5 rounded border border-red-100 line-clamp-1">
                        {dlq.failureReason}
                      </span>
                    </td>
                    <td className="px-6 py-4 font-mono font-bold text-gray-600">{dlq.retryAttempts} max retries</td>
                    <td className="px-6 py-4">
                      <span
                        className={`px-2 py-0.5 rounded-full font-bold text-[10px] ${
                          dlq.status === 'QUARANTINED'
                            ? 'bg-red-100 text-red-800 animate-pulse'
                            : dlq.status === 'REPLAYED'
                            ? 'bg-green-100 text-green-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {dlq.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <button
                        onClick={() => setSelectedEvent(dlq)}
                        className="p-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-md"
                        title="View Payload & Stack Trace"
                      >
                        <Eye className="h-3.5 w-3.5" />
                      </button>

                      {dlq.status === 'QUARANTINED' && (
                        <>
                          <button
                            disabled={replayingId === dlq.id}
                            onClick={() => handleReplay(dlq.id)}
                            className="p-1.5 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded-md font-semibold"
                            title="1-Click Replay"
                          >
                            <RotateCw className={`h-3.5 w-3.5 ${replayingId === dlq.id ? 'animate-spin' : ''}`} />
                          </button>
                          <button
                            onClick={() => handleDiscard(dlq.id)}
                            className="p-1.5 bg-gray-100 hover:bg-red-50 text-gray-500 hover:text-red-600 rounded-md"
                            title="Discard"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Payload & StackTrace Modal */}
      {selectedEvent && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-gray-200 space-y-4">
            <div className="flex justify-between items-center border-b border-gray-100 pb-3">
              <h3 className="font-bold text-gray-900 flex items-center space-x-2">
                <AlertOctagon className="h-5 w-5 text-red-600" />
                <span>DLQ-{selectedEvent.id} Diagnostic Inspector</span>
              </h3>
              <button onClick={() => setSelectedEvent(null)} className="p-1 text-gray-400 hover:bg-gray-100 rounded-lg">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-gray-700 mb-1">Target Endpoint</label>
                <div className="bg-gray-50 p-2 rounded border border-gray-200 font-mono text-gray-800">
                  {selectedEvent.targetEndpoint}
                </div>
              </div>

              <div>
                <label className="block font-semibold text-gray-700 mb-1">Payload (JSON)</label>
                <pre className="bg-gray-900 text-green-400 p-3 rounded-lg font-mono text-[11px] overflow-x-auto max-h-40">
                  {JSON.stringify(JSON.parse(selectedEvent.payloadJson || '{}'), null, 2)}
                </pre>
              </div>

              <div>
                <label className="block font-semibold text-gray-700 mb-1">Exception Stack Trace</label>
                <pre className="bg-red-950/80 text-red-200 p-3 rounded-lg font-mono text-[10px] overflow-x-auto max-h-40">
                  {selectedEvent.stackTrace || selectedEvent.failureReason}
                </pre>
              </div>
            </div>

            <div className="flex justify-end space-x-2 pt-2 border-t border-gray-100">
              <button
                onClick={() => setSelectedEvent(null)}
                className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg font-medium text-xs"
              >
                Close
              </button>
              {selectedEvent.status === 'QUARANTINED' && (
                <button
                  onClick={() => {
                    handleReplay(selectedEvent.id);
                    setSelectedEvent(null);
                  }}
                  className="flex items-center space-x-1.5 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg font-semibold text-xs shadow-xs"
                >
                  <RotateCw className="h-3.5 w-3.5" />
                  <span>Replay Now</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
