import React, { useState, useRef, useEffect } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import { AppNotification, ApiResponse } from '../../types';
import {
  LayoutDashboard,
  FileText,
  CheckSquare,
  Building2,
  ShoppingCart,
  GitBranch,
  PlayCircle,
  BarChart3,
  History,
  Settings as SettingsIcon,
  LogOut,
  ShieldCheck,
  Bell,
  Check,
  ExternalLink,
} from 'lucide-react';

export const AppLayout: React.FC = () => {
  const { user, organization, logout } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showNotifications, setShowNotifications] = useState(false);
  const notificationRef = useRef<HTMLDivElement>(null);

  // Close notifications on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (notificationRef.current && !notificationRef.current.contains(e.target as Node)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Notifications query
  const { data: notifications = [] } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<AppNotification[]>>('/notifications');
      return res.data.data;
    },
    refetchInterval: 10000,
  });

  // Unread count query
  const { data: unreadData } = useQuery({
    queryKey: ['notifications-unread-count'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count');
      return res.data.data;
    },
    refetchInterval: 10000,
  });

  const unreadCount = unreadData?.unreadCount ?? notifications.filter((n) => !n.read).length;

  // Mark single as read
  const markReadMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.post(`/notifications/${id}/read`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
  });

  // Mark all as read
  const markAllReadMutation = useMutation({
    mutationFn: async () => {
      await api.post('/notifications/read-all');
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count'] });
    },
  });

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/invoices', label: 'Invoices', icon: FileText },
    { to: '/approvals', label: 'Approvals Inbox', icon: CheckSquare },
    { to: '/purchase-orders', label: 'Purchase Orders', icon: ShoppingCart },
    { to: '/suppliers', label: 'Suppliers', icon: Building2 },
    { to: '/workflows', label: 'Workflow Builder', icon: GitBranch },
    { to: '/executions', label: 'Executions & Logs', icon: PlayCircle },
    { to: '/analytics', label: 'Operational Analytics', icon: BarChart3 },
    { to: '/audit', label: 'Compliance Audit Trail', icon: History },
    { to: '/settings', label: 'Settings & Integrations', icon: SettingsIcon },
  ];

  const getRoleBadgeColor = (role?: string) => {
    switch (role) {
      case 'OWNER':
        return 'bg-emerald-950/80 text-emerald-300 border-emerald-700/60';
      case 'ADMIN':
        return 'bg-slate-800 text-slate-200 border-slate-700';
      case 'OPERATOR':
        return 'bg-blue-950/80 text-blue-300 border-blue-700/60';
      case 'APPROVER':
        return 'bg-amber-950/80 text-amber-300 border-amber-700/60';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <div className="flex h-screen bg-[#F8FAFC] text-slate-800 overflow-hidden font-sans antialiased">
      {/* Sidebar */}
      <aside className="w-64 bg-[#0B0F17] text-slate-200 flex flex-col flex-shrink-0 border-r border-slate-800">
        <div className="p-4 border-b border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <img src="/opspilot-logo.svg" alt="OpsPilot" className="w-8 h-8 rounded-md" />
            <div>
              <div className="flex items-center gap-1.5">
                <h1 className="font-bold text-base text-white tracking-tight">OpsPilot</h1>
                <span className="text-[9px] font-bold tracking-wider uppercase px-1.5 py-0.5 rounded bg-emerald-950 text-emerald-400 border border-emerald-800/60">
                  AP
                </span>
              </div>
              <p className="text-[11px] text-slate-400 flex items-center gap-1 mt-0.5">
                <ShieldCheck className="w-3 h-3 text-emerald-400" /> Enterprise Operations
              </p>
            </div>
          </div>
        </div>

        {/* Tenant Banner */}
        <div className="px-4 py-3 bg-[#0E1523] border-b border-slate-800/70">
          <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Active Tenant</p>
          <p className="text-xs font-semibold text-slate-100 truncate mt-0.5">{organization?.name || 'Global Logistics Enterprise'}</p>
          <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-400">
            <span className="font-mono text-slate-300">{organization?.defaultCurrency || 'USD'}</span>
            <span>•</span>
            <span>{organization?.timezone || 'UTC'}</span>
            <span className="ml-auto inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-400">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span> Live
            </span>
          </div>
        </div>

        {/* Nav Links */}
        <nav className="flex-1 px-2.5 py-3 space-y-0.5 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-3 py-2 rounded-lg text-xs font-medium transition-colors ${
                    isActive
                      ? 'bg-emerald-950/70 text-emerald-300 border-l-2 border-emerald-500 font-semibold shadow-xs'
                      : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/50'
                  }`
                }
              >
                <Icon className="w-4 h-4 flex-shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* User Card & Logout */}
        <div className="p-3 border-t border-slate-800 bg-[#070A10]">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center space-x-2.5 min-w-0">
              <div className="w-7 h-7 rounded bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 font-bold text-xs">
                {user?.firstName?.charAt(0) || 'U'}
              </div>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-slate-200 truncate">
                  {user ? `${user.firstName} ${user.lastName}` : 'Eleanor Vance'}
                </p>
                <p className="text-[11px] text-slate-400 truncate">{user?.email || 'admin@opspilot.io'}</p>
              </div>
            </div>
          </div>
          <div className="flex items-center justify-between gap-2 pt-1 border-t border-slate-800/60">
            <span
              className={`px-2 py-0.5 text-[9px] font-bold uppercase tracking-wider rounded border ${getRoleBadgeColor(
                user?.role
              )}`}
            >
              {user?.role || 'OWNER'}
            </span>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 px-2 py-1 text-xs text-slate-400 hover:text-rose-400 hover:bg-slate-800/80 rounded transition-colors"
              title="Sign out"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </aside>

      {/* Main Content Pane */}
      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <header className="h-14 bg-white border-b border-slate-200/90 flex items-center justify-between px-6 flex-shrink-0 shadow-xs relative">
          <div className="flex items-center gap-3">
            <span className="text-[11px] font-bold uppercase tracking-wider px-2 py-0.5 bg-slate-100 text-slate-700 rounded border border-slate-200">
              Financial Operations Console
            </span>
            <span className="text-xs text-slate-500 hidden sm:inline">
              Tenant ID: <code className="font-mono text-slate-700">{organization?.id?.substring(0, 8)}...</code>
            </span>
          </div>

          <div className="flex items-center gap-4">
            <span className="inline-flex items-center gap-1.5 text-xs text-emerald-700 bg-emerald-50 px-2.5 py-1 rounded-full font-medium border border-emerald-200">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 animate-pulse"></span>
              Deterministic Engine Ready
            </span>

            {/* Notification Bell */}
            <div className="relative" ref={notificationRef}>
              <button
                onClick={() => setShowNotifications(!showNotifications)}
                className="relative p-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition-colors"
                title="Notifications"
              >
                <Bell className="w-5 h-5" />
                {unreadCount > 0 && (
                  <span className="absolute top-1 right-1 w-4 h-4 bg-rose-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center animate-pulse">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {/* Notification Popover */}
              {showNotifications && (
                <div className="absolute right-0 mt-2 w-96 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden animate-in fade-in duration-150">
                  <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                    <div className="flex items-center gap-2">
                      <h3 className="text-sm font-bold text-slate-900">Notifications</h3>
                      {unreadCount > 0 && (
                        <span className="px-2 py-0.5 bg-rose-100 text-rose-700 text-[10px] font-bold rounded-full">
                          {unreadCount} new
                        </span>
                      )}
                    </div>
                    {unreadCount > 0 && (
                      <button
                        onClick={() => markAllReadMutation.mutate()}
                        className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold flex items-center gap-1"
                      >
                        <Check className="w-3.5 h-3.5" />
                        Mark all read
                      </button>
                    )}
                  </div>

                  <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
                    {notifications.length > 0 ? (
                      notifications.map((n) => (
                        <div
                          key={n.id}
                          className={`p-3.5 hover:bg-slate-50 transition-colors ${
                            !n.read ? 'bg-indigo-50/40' : ''
                          }`}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <span className="text-xs font-bold text-slate-800">{n.title}</span>
                            <span className="text-[10px] text-slate-400 flex-shrink-0">
                              {new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </span>
                          </div>
                          <p className="text-xs text-slate-600 mt-1">{n.message}</p>
                          <div className="mt-2 flex items-center justify-between">
                            {n.linkUrl ? (
                              <button
                                onClick={() => {
                                  markReadMutation.mutate(n.id);
                                  setShowNotifications(false);
                                  navigate(n.linkUrl!);
                                }}
                                className="text-[11px] text-indigo-600 hover:underline font-medium flex items-center gap-1"
                              >
                                View Details <ExternalLink className="w-3 h-3" />
                              </button>
                            ) : <span />}
                            {!n.read && (
                              <button
                                onClick={() => markReadMutation.mutate(n.id)}
                                className="text-[10px] text-slate-400 hover:text-slate-700 font-medium"
                              >
                                Mark read
                              </button>
                            )}
                          </div>
                        </div>
                      ))
                    ) : (
                      <div className="p-8 text-center text-slate-400 text-xs">
                        No notifications at this time.
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6 bg-slate-50">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
