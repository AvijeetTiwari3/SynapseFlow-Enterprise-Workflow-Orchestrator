import React from 'react';
import { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  color: 'blue' | 'green' | 'yellow' | 'red' | 'purple';
}

export const MetricCard: React.FC<MetricCardProps> = ({ title, value, subtitle, icon: Icon, color }) => {
  const colorMap = {
    blue: { bg: 'bg-blue-50', text: 'text-blue-600', border: 'border-blue-100' },
    green: { bg: 'bg-green-50', text: 'text-green-600', border: 'border-green-100' },
    yellow: { bg: 'bg-amber-50', text: 'text-amber-600', border: 'border-amber-100' },
    red: { bg: 'bg-red-50', text: 'text-red-600', border: 'border-red-100' },
    purple: { bg: 'bg-purple-50', text: 'text-purple-600', border: 'border-purple-100' },
  };

  const scheme = colorMap[color];

  return (
    <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-xs hover:shadow-md transition">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">{title}</p>
          <h3 className="text-2xl font-bold text-gray-900 mt-1">{value}</h3>
          {subtitle && <p className="text-xs text-gray-400 mt-0.5">{subtitle}</p>}
        </div>
        <div className={`h-11 w-11 rounded-lg ${scheme.bg} ${scheme.border} border flex items-center justify-center ${scheme.text}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
    </div>
  );
};
