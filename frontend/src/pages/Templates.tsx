import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { WorkflowTemplate } from '../types';
import { Layers, RefreshCw, Plus, Clock, ShieldCheck, Code } from 'lucide-react';

export const Templates: React.FC = () => {
  const [templates, setTemplates] = useState<WorkflowTemplate[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchTemplates = async () => {
    try {
      setLoading(true);
      const res = await api.getTemplates();
      setTemplates(res.data);
    } catch (err) {
      console.error('Failed to load templates', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center bg-white p-6 rounded-xl border border-gray-200 shadow-xs">
        <div>
          <h2 className="text-xl font-bold text-gray-900">Enterprise Workflow Templates</h2>
          <p className="text-xs text-gray-500 mt-1">
            Pre-configured declarative Finite State Machine templates with multi-role approval gates.
          </p>
        </div>
        <button
          onClick={fetchTemplates}
          className="p-2 text-gray-500 hover:text-blue-600 hover:bg-gray-100 rounded-lg transition"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {templates.map((tpl) => {
          let steps: any[] = [];
          try {
            steps = JSON.parse(tpl.stepsDefinitionJson || '[]');
          } catch (e) {
            steps = [];
          }

          return (
            <div key={tpl.id} className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-4 hover:border-blue-300 transition">
              <div className="flex justify-between items-start">
                <span className="bg-blue-50 text-blue-700 text-xs px-2.5 py-1 rounded font-semibold border border-blue-100">
                  {tpl.category}
                </span>
                <span className="text-[11px] font-mono text-gray-400">v{tpl.version}</span>
              </div>

              <div>
                <h3 className="font-bold text-base text-gray-900">{tpl.name}</h3>
                <p className="text-xs text-gray-500 mt-1 line-clamp-2">{tpl.description}</p>
              </div>

              <div className="space-y-2 pt-2 border-t border-gray-100 text-xs">
                <div className="font-semibold text-gray-700">Approval Steps Pipeline ({steps.length}):</div>
                <div className="space-y-1.5">
                  {steps.map((step, idx) => (
                    <div key={idx} className="flex items-center justify-between p-2 bg-gray-50 rounded-lg text-[11px]">
                      <span className="font-medium text-gray-800">{idx + 1}. {step.name}</span>
                      <span className="bg-white px-2 py-0.5 rounded border border-gray-200 font-mono text-[10px] text-gray-600">
                        {step.requiredRole?.replace('ROLE_', '')}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
