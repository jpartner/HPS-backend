'use client';

import Link from 'next/link';
import { Users, Briefcase, Calendar, DollarSign, ArrowRight } from 'lucide-react';
import { AdminLayout } from '@/components/layout/AdminLayout';

const stats = [
  { label: 'Total Users', value: '---', icon: Users, href: '/users' },
  { label: 'Total Providers', value: '---', icon: Briefcase, href: '/providers' },
  { label: 'Active Bookings', value: '---', icon: Calendar, href: '#' },
  { label: 'Revenue', value: '---', icon: DollarSign, href: '#' },
];

const quickLinks = [
  { label: 'Tenants', href: '/tenants', description: 'Manage tenant configurations' },
  { label: 'Categories', href: '/categories', description: 'Service category hierarchy' },
  { label: 'Service Templates', href: '/service-templates', description: 'Template definitions' },
  { label: 'Attributes', href: '/attributes', description: 'Attribute schema management' },
  { label: 'Users', href: '/users', description: 'User accounts and roles' },
  { label: 'Providers', href: '/providers', description: 'Provider verification and management' },
  { label: 'API Keys', href: '/api-keys', description: 'API key lifecycle' },
];

const recentActivity = [
  { id: 1, text: 'System initialized', time: 'Just now' },
  { id: 2, text: 'Awaiting activity data from API', time: '--' },
];

export default function DashboardPage() {
  return (
    <AdminLayout>
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-sm text-gray-500 mt-1">Overview of your service marketplace</p>
        </div>

        {/* Stat Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {stats.map((stat) => {
            const Icon = stat.icon;
            return (
              <Link
                key={stat.label}
                href={stat.href}
                className="bg-white rounded-lg border border-gray-200 p-5 hover:shadow-md transition-shadow"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-500">{stat.label}</p>
                    <p className="text-2xl font-semibold text-gray-900 mt-1">{stat.value}</p>
                  </div>
                  <div className="p-2 bg-indigo-50 rounded-lg">
                    <Icon className="h-5 w-5 text-indigo-600" />
                  </div>
                </div>
              </Link>
            );
          })}
        </div>

        {/* Quick Links + Recent Activity */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <h2 className="text-lg font-semibold text-gray-900 mb-3">Quick Links</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {quickLinks.map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className="flex items-center justify-between bg-white rounded-lg border border-gray-200 p-4 hover:border-indigo-300 hover:shadow-sm transition-all group"
                >
                  <div>
                    <p className="font-medium text-gray-900">{link.label}</p>
                    <p className="text-sm text-gray-500">{link.description}</p>
                  </div>
                  <ArrowRight className="h-4 w-4 text-gray-400 group-hover:text-indigo-600 transition-colors" />
                </Link>
              ))}
            </div>
          </div>

          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-3">Recent Activity</h2>
            <div className="bg-white rounded-lg border border-gray-200 divide-y divide-gray-100">
              {recentActivity.map((item) => (
                <div key={item.id} className="p-4">
                  <p className="text-sm text-gray-900">{item.text}</p>
                  <p className="text-xs text-gray-400 mt-1">{item.time}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
