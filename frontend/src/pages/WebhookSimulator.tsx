import React, { useState } from 'react';
import { api } from '../api/client';
import { Radio, Send, CheckCircle2, ShieldCheck, RefreshCw } from 'lucide-react';

interface WebhookSimulatorProps {
  onWorkflowTriggered: (id: number) => void;
}

export const WebhookSimulator: React.FC<WebhookSimulatorProps> = ({ onWorkflowTriggered }) => {
  const [source, setSource] = useState('workday');
  const [eventType, setEventType] = useState('EMPLOYEE_ONBOARDING');
  const [payloadText, setPayloadText] = useState(
    JSON.stringify(
      {
        employeeId: 'EMP-9824',
        name: 'Sarah Chen',
        title: 'Senior Software Engineer (L5)',
        department: 'Search Ranking Infrastructure',
        startDate: '2026-10-01',
      },
      null,
      2
    )
  );
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<any | null>(null);

  const presets: Record<string, { eventType: string; payload: any }> = {
    workday: {
      eventType: 'EMPLOYEE_HIRE',
      payload: {
        employeeId: 'EMP-9824',
        name: 'Sarah Chen',
        title: 'Senior Software Engineer (L5)',
        department: 'Search Ranking Infrastructure',
        startDate: '2026-10-01',
      },
    },
    salesforce: {
      eventType: 'CONTRACT_SIGNED',
      payload: {
        opportunityId: 'OPP-5421',
        accountName: 'Snowflake Computing',
        annualValue: '$250,000',
        tier: 'Enterprise SaaS',
        legalReviewed: true,
      },
    },
    jira: {
      eventType: 'SEV1_INCIDENT_ELEVATION',
      payload: {
        incidentKey: 'INC-8891',
        service: 'Cloud Spanner Ads Shard 02',
        urgency: 'HIGH',
        requestedIAMRole: 'roles/spanner.databaseAdmin',
      },
    },
  };

  const handleSelectPreset = (key: string) => {
    setSource(key);
    setEventType(presets[key].eventType);
    setPayloadText(JSON.stringify(presets[key].payload, null, 2));
  };

  const handleSendWebhook = async () => {
    try {
      setLoading(true);
      setResult(null);
      const parsedData = JSON.parse(payloadText);
      const response = await api.simulateWebhook(source, {
        source,
        eventType,
        data: parsedData,
      });

      setResult(response.data);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Webhook simulation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs">
        <div className="flex items-center space-x-2">
          <Radio className="h-5 w-5 text-blue-600 animate-pulse" />
          <h2 className="text-xl font-bold text-gray-900">Third-Party SaaS Webhook Ingestion Simulator</h2>
        </div>
        <p className="text-xs text-gray-500 mt-1">
          Test real-time event ingestion from external SaaS vendors (Workday, Salesforce, Jira, ServiceNow). Verifies HMAC-SHA256 signatures, applies rate limiting, and auto-launches FSM approval workflows.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Preset Selector */}
        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-3">
          <h3 className="font-bold text-xs text-gray-700 uppercase tracking-wider">Select Vendor SaaS Scenario</h3>
          
          <button
            onClick={() => handleSelectPreset('workday')}
            className={`w-full text-left p-3 rounded-xl border transition ${
              source === 'workday' ? 'bg-blue-50 border-blue-300 ring-2 ring-blue-500/20' : 'hover:bg-gray-50'
            }`}
          >
            <div className="font-bold text-xs text-gray-900">Workday HRMS</div>
            <div className="text-[11px] text-gray-500">New Employee Hire → Triggers Onboarding Workflow</div>
          </button>

          <button
            onClick={() => handleSelectPreset('salesforce')}
            className={`w-full text-left p-3 rounded-xl border transition ${
              source === 'salesforce' ? 'bg-blue-50 border-blue-300 ring-2 ring-blue-500/20' : 'hover:bg-gray-50'
            }`}
          >
            <div className="font-bold text-xs text-gray-900">Salesforce CRM</div>
            <div className="text-[11px] text-gray-500">Contract Signed → Triggers Vendor Risk Signoff</div>
          </button>

          <button
            onClick={() => handleSelectPreset('jira')}
            className={`w-full text-left p-3 rounded-xl border transition ${
              source === 'jira' ? 'bg-blue-50 border-blue-300 ring-2 ring-blue-500/20' : 'hover:bg-gray-50'
            }`}
          >
            <div className="font-bold text-xs text-gray-900">Jira / ServiceNow</div>
            <div className="text-[11px] text-gray-500">SEV-1 Incident → Triggers GCP Elevated IAM Access</div>
          </button>
        </div>

        {/* Payload Editor & Dispatch */}
        <div className="lg:col-span-2 bg-white p-6 rounded-xl border border-gray-200 shadow-xs space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="font-bold text-sm text-gray-900">Webhook Request Payload</h3>
            <div className="flex items-center space-x-1.5 text-xs text-green-700 bg-green-50 px-2 py-0.5 rounded border border-green-200 font-mono">
              <ShieldCheck className="h-3.5 w-3.5" />
              <span>HMAC-SHA256 Protected</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3 text-xs">
            <div>
              <label className="block font-semibold text-gray-700 mb-1">Source Header</label>
              <input
                type="text"
                value={source}
                onChange={(e) => setSource(e.target.value)}
                className="w-full border border-gray-300 rounded-lg p-2 font-mono"
              />
            </div>
            <div>
              <label className="block font-semibold text-gray-700 mb-1">Event Type</label>
              <input
                type="text"
                value={eventType}
                onChange={(e) => setEventType(e.target.value)}
                className="w-full border border-gray-300 rounded-lg p-2 font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block font-semibold text-gray-700 mb-1 text-xs">JSON Body</label>
            <textarea
              rows={8}
              value={payloadText}
              onChange={(e) => setPayloadText(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-3 font-mono text-xs text-green-400 bg-gray-900 focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <button
            onClick={handleSendWebhook}
            disabled={loading}
            className="w-full flex items-center justify-center space-x-2 bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-semibold text-xs shadow-xs disabled:opacity-50 transition"
          >
            {loading ? <RefreshCw className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />}
            <span>{loading ? 'Ingesting Webhook...' : 'Dispatch Live Ingestion Webhook'}</span>
          </button>

          {result && (
            <div className="p-4 bg-green-50 border border-green-200 rounded-xl space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2 text-green-800 font-bold">
                  <CheckCircle2 className="h-4 w-4 text-green-600" />
                  <span>Webhook Successfully Ingested & FSM Instance Spawned!</span>
                </div>
                <button
                  onClick={() => onWorkflowTriggered(result.id)}
                  className="bg-green-600 hover:bg-green-700 text-white px-3 py-1 rounded font-semibold text-xs"
                >
                  View Spawned Workflow (WF-{result.id}) →
                </button>
              </div>
              <p className="text-gray-600">
                Created workflow: <strong>{result.title}</strong> under category <strong>{result.category}</strong>.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
