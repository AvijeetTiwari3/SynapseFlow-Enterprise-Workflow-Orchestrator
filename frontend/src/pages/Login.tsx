import React, { useState } from 'react';
import { api } from '../api/client';
import { AuthResponse } from '../types';
import { Cpu, ShieldCheck, Lock, User, ArrowRight, Check } from 'lucide-react';

interface LoginProps {
  onLoginSuccess: (user: AuthResponse) => void;
}

export const Login: React.FC<LoginProps> = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      setError('');
      const res = await api.login(username, password);
      localStorage.setItem('synapse_token', res.data.token);
      localStorage.setItem('synapse_user', JSON.stringify(res.data));
      onLoginSuccess(res.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  const quickSwitchUser = (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-950 to-slate-900 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-3xl p-8 shadow-2xl border border-gray-100 space-y-6">
        {/* Brand */}
        <div className="text-center space-y-2">
          <div className="h-12 w-12 rounded-2xl bg-blue-600 text-white flex items-center justify-center mx-auto shadow-md">
            <Cpu className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">SynapseFlow</h1>
          <p className="text-xs text-gray-500">Google Corporate Engineering Control Plane</p>
        </div>

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
            {error}
          </div>
        )}

        {/* Login Form */}
        <form onSubmit={handleLogin} className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold text-gray-700 mb-1">Corporate Username</label>
            <div className="relative">
              <User className="h-4 w-4 text-gray-400 absolute left-3 top-2.5" />
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full pl-9 pr-3 py-2.5 border border-gray-300 rounded-xl text-gray-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block font-semibold text-gray-700 mb-1">Password</label>
            <div className="relative">
              <Lock className="h-4 w-4 text-gray-400 absolute left-3 top-2.5" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-9 pr-3 py-2.5 border border-gray-300 rounded-xl text-gray-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full flex items-center justify-center space-x-2 bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-xl font-bold text-xs shadow-md transition disabled:opacity-50"
          >
            <span>{loading ? 'Authenticating with JWT...' : 'Sign In to Workspace'}</span>
            <ArrowRight className="h-4 w-4" />
          </button>
        </form>

        {/* 1-Click Persona Switcher */}
        <div className="pt-4 border-t border-gray-100 space-y-2">
          <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider text-center">
            Quick Persona Switcher (Pre-Seeded)
          </div>

          <div className="grid grid-cols-2 gap-2 text-[11px]">
            <button
              type="button"
              onClick={() => quickSwitchUser('admin', 'admin123')}
              className={`p-2 rounded-lg border text-left transition ${
                username === 'admin' ? 'bg-blue-50 border-blue-400 text-blue-800' : 'bg-gray-50 border-gray-200'
              }`}
            >
              <div className="font-bold">admin</div>
              <div className="text-[10px] text-gray-500">Corporate Admin (All Roles)</div>
            </button>

            <button
              type="button"
              onClick={() => quickSwitchUser('manager_jane', 'password123')}
              className={`p-2 rounded-lg border text-left transition ${
                username === 'manager_jane' ? 'bg-blue-50 border-blue-400 text-blue-800' : 'bg-gray-50 border-gray-200'
              }`}
            >
              <div className="font-bold">manager_jane</div>
              <div className="text-[10px] text-gray-500">L6 Engineering Manager</div>
            </button>

            <button
              type="button"
              onClick={() => quickSwitchUser('secops_alex', 'password123')}
              className={`p-2 rounded-lg border text-left transition ${
                username === 'secops_alex' ? 'bg-blue-50 border-blue-400 text-blue-800' : 'bg-gray-50 border-gray-200'
              }`}
            >
              <div className="font-bold">secops_alex</div>
              <div className="text-[10px] text-gray-500">Security & Ops Lead</div>
            </button>

            <button
              type="button"
              onClick={() => quickSwitchUser('auditor_bob', 'password123')}
              className={`p-2 rounded-lg border text-left transition ${
                username === 'auditor_bob' ? 'bg-blue-50 border-blue-400 text-blue-800' : 'bg-gray-50 border-gray-200'
              }`}
            >
              <div className="font-bold">auditor_bob</div>
              <div className="text-[10px] text-gray-500">Compliance Auditor</div>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
