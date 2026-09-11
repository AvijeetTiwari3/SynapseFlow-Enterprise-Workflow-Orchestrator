import React, { useEffect, useState } from 'react';
import { AuthResponse } from './types';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { Dashboard } from './pages/Dashboard';
import { Workflows } from './pages/Workflows';
import { WorkflowDetail } from './pages/WorkflowDetail';
import { Templates } from './pages/Templates';
import { DlqManager } from './pages/DlqManager';
import { AuditExplorer } from './pages/AuditExplorer';
import { WebhookSimulator } from './pages/WebhookSimulator';
import { CreateWorkflowModal } from './pages/CreateWorkflowModal';
import { Login } from './pages/Login';
import { api } from './api/client';

export const App: React.FC = () => {
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [currentTab, setCurrentTab] = useState('dashboard');
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<number | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [dlqCount, setDlqCount] = useState(0);

  useEffect(() => {
    const savedUser = localStorage.getItem('synapse_user');
    const token = localStorage.getItem('synapse_token');
    if (savedUser && token) {
      try {
        setUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.clear();
      }
    }
  }, []);

  const fetchDlqCount = async () => {
    if (!user) return;
    try {
      const res = await api.getMetrics();
      setDlqCount(res.data.dlqQuarantined);
    } catch (e) {
      // Ignore background poll errors
    }
  };

  useEffect(() => {
    fetchDlqCount();
    const interval = setInterval(fetchDlqCount, 10000);
    return () => clearInterval(interval);
  }, [user]);

  const handleLogout = () => {
    localStorage.removeItem('synapse_token');
    localStorage.removeItem('synapse_user');
    setUser(null);
  };

  if (!user) {
    return <Login onLoginSuccess={(u) => setUser(u)} />;
  }

  const renderContent = () => {
    if (selectedWorkflowId) {
      return (
        <WorkflowDetail
          workflowId={selectedWorkflowId}
          onBack={() => setSelectedWorkflowId(null)}
        />
      );
    }

    switch (currentTab) {
      case 'dashboard':
        return (
          <Dashboard
            onSelectWorkflow={(id) => setSelectedWorkflowId(id)}
            onNavigateToTab={(tab) => setCurrentTab(tab)}
          />
        );
      case 'workflows':
        return (
          <Workflows
            onSelectWorkflow={(id) => setSelectedWorkflowId(id)}
            onOpenCreateModal={() => setIsCreateModalOpen(true)}
          />
        );
      case 'templates':
        return <Templates />;
      case 'dlq':
        return <DlqManager />;
      case 'audit':
        return <AuditExplorer />;
      case 'simulator':
        return (
          <WebhookSimulator
            onWorkflowTriggered={(id) => {
              setCurrentTab('workflows');
              setSelectedWorkflowId(id);
            }}
          />
        );
      default:
        return <div>Tab not found</div>;
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#f8f9fa]">
      <Navbar user={user} onLogout={handleLogout} />

      <div className="flex-1 flex max-w-7xl w-full mx-auto">
        <Sidebar
          currentTab={currentTab}
          onSelectTab={(tab) => {
            setSelectedWorkflowId(null);
            setCurrentTab(tab);
          }}
          dlqCount={dlqCount}
        />

        <main className="flex-1 p-6 overflow-y-auto">
          {renderContent()}
        </main>
      </div>

      <CreateWorkflowModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={(id) => {
          setIsCreateModalOpen(false);
          setSelectedWorkflowId(id);
        }}
      />
    </div>
  );
};
