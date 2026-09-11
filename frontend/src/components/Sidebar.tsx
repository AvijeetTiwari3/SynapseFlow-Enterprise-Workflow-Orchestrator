import React from 'react';
import { 
  LayoutDashboard, 
  GitMerge, 
  AlertOctagon, 
  ScrollText, 
  Radio, 
  Layers
} from 'lucide-react';

interface SidebarProps {
  currentTab: string;
  onSelectTab: (tab: string) => void;
  dlqCount?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, onSelectTab, dlqCount = 0 }) => {
  const navItems = [
    { id: 'dashboard', label: 'Overview & Telemetry', icon: LayoutDashboard },
    { id: 'workflows', label: 'Workflow Instances', icon: GitMerge },
    { id: 'templates', label: 'Workflow Templates', icon: Layers },
    { id: 'dlq', label: 'Dead-Letter Queue (DLQ)', icon: AlertOctagon, badge: dlqCount > 0 ? dlqCount : null },
    { id: 'audit', label: 'Audit & Compliance Log', icon: ScrollText },
    { id: 'simulator', label: 'SaaS Webhook Simulator', icon: Radio },
  ];

  return (
    <aside className="w-64 bg-white border-r border-gray-200 min-h-[calc(100vh-4rem)] p-4 flex flex-col justify-between">
      <nav className="space-y-1.5">
        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider px-3 mb-2">
          Enterprise Control Plane
        </div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = currentTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => onSelectTab(item.id)}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                isActive
                  ? 'bg-blue-50 text-blue-700 font-semibold shadow-xs'
                  : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
              }`}
            >
              <div className="flex items-center space-x-3">
                <Icon className={`h-4.5 w-4.5 ${isActive ? 'text-blue-600' : 'text-gray-500'}`} />
                <span>{item.label}</span>
              </div>
              {item.badge ? (
                <span className="bg-red-500 text-white text-xs px-2 py-0.5 rounded-full font-bold animate-pulse">
                  {item.badge}
                </span>
              ) : null}
            </button>
          );
        })}
      </nav>

      <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg text-xs text-gray-500 space-y-1">
        <div className="font-semibold text-gray-700">Architecture Specs</div>
        <div>• Java 21 LTS + Spring Boot 3.3</div>
        <div>• Resilience4j Circuit Breakers</div>
        <div>• Transactional Outbox Pattern</div>
        <div>• Optimistic Locking (@Version)</div>
      </div>
    </aside>
  );
};
