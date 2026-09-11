import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { WorkflowTemplate } from '../types';
import { X, Send } from 'lucide-react';

interface CreateWorkflowModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (id: number) => void;
}

export const CreateWorkflowModal: React.FC<CreateWorkflowModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [templates, setTemplates] = useState<WorkflowTemplate[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const [title, setTitle] = useState('');
  const [payloadJson, setPayloadJson] = useState('{\n  "justification": "Production deployment access",\n  "urgency": "HIGH"\n}');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen) {
      api.getTemplates().then((res) => {
        setTemplates(res.data);
        if (res.data.length > 0) {
          setSelectedTemplateId(res.data[0].id);
        }
      });
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTemplateId || !title.trim()) return;

    try {
      setLoading(true);
      const res = await api.createInstance({
        templateId: selectedTemplateId,
        title,
        contextPayloadJson: payloadJson,
      });
      onSuccess(res.data.id);
      onClose();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to create workflow');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-gray-200">
        <div className="flex justify-between items-center border-b border-gray-100 pb-3">
          <h3 className="text-lg font-bold text-gray-900">Launch New Enterprise Workflow</h3>
          <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded-lg text-gray-400">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 mt-4 text-xs">
          <div>
            <label className="block font-semibold text-gray-700 mb-1">Workflow Template</label>
            <select
              value={selectedTemplateId || ''}
              onChange={(e) => setSelectedTemplateId(Number(e.target.value))}
              className="w-full border border-gray-300 rounded-lg p-2.5 bg-white text-gray-800 focus:ring-2 focus:ring-blue-500"
            >
              {templates.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.category})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-semibold text-gray-700 mb-1">Workflow Title / Request Summary</label>
            <input
              type="text"
              required
              placeholder="e.g. Elevate BigQuery Access for EMEA Growth Pipeline"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-2.5 text-gray-900 focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block font-semibold text-gray-700 mb-1">Context Payload (JSON)</label>
            <textarea
              rows={4}
              value={payloadJson}
              onChange={(e) => setPayloadJson(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-2.5 font-mono text-xs text-gray-900 bg-gray-50 focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2 border-t border-gray-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg font-medium"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex items-center space-x-1.5 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg font-semibold shadow-xs disabled:opacity-50"
            >
              <Send className="h-3.5 w-3.5" />
              <span>{loading ? 'Launching...' : 'Instantiate FSM'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
