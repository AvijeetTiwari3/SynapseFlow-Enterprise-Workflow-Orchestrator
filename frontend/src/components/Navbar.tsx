import React from 'react';
import { AuthResponse } from '../types';
import { ShieldCheck, LogOut, Cpu, Activity, User } from 'lucide-react';

interface NavbarProps {
  user: AuthResponse | null;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, onLogout }) => {
  return (
    <header className="bg-white border-b border-gray-200 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          {/* Brand Logo & Name */}
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-sm">
              <Cpu className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg text-gray-900 tracking-tight">SynapseFlow</span>
                <span className="bg-blue-100 text-blue-800 text-xs font-semibold px-2 py-0.5 rounded border border-blue-200">
                  Google AppEng Edition
                </span>
              </div>
              <p className="text-xs text-gray-500 hidden sm:block">Enterprise Workflow Orchestrator & Integration Gateway</p>
            </div>
          </div>

          {/* Right Header Status & Profile */}
          <div className="flex items-center space-x-4">
            {/* Live System Indicator */}
            <div className="hidden md:flex items-center space-x-2 bg-green-50 text-green-700 px-3 py-1 rounded-full text-xs font-medium border border-green-200">
              <Activity className="h-3.5 w-3.5 animate-pulse text-green-600" />
              <span>Engine Active (Virtual Threads)</span>
            </div>

            {user ? (
              <div className="flex items-center space-x-3 border-l border-gray-200 pl-4">
                <div className="text-right hidden sm:block">
                  <div className="text-sm font-semibold text-gray-800">{user.fullName}</div>
                  <div className="flex items-center justify-end space-x-1">
                    <ShieldCheck className="h-3 w-3 text-blue-600" />
                    <span className="text-xs text-blue-600 font-medium">
                      {user.roles?.[0]?.replace('ROLE_', '') || 'USER'}
                    </span>
                    <span className="text-xs text-gray-400">• {user.department || 'CorpEng'}</span>
                  </div>
                </div>

                <div className="h-9 w-9 rounded-full bg-gray-100 border border-gray-300 flex items-center justify-center text-gray-700 font-bold">
                  {user.fullName ? user.fullName.charAt(0) : <User className="h-4 w-4" />}
                </div>

                <button
                  onClick={onLogout}
                  title="Logout"
                  className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
};
