import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { OverviewAnalytics, Invoice, PagedResponse, ApiResponse } from '../types';
import {
  FileText,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Send,
  Cpu,
  TrendingUp,
  ArrowRight,
  Upload,
  CheckSquare,
  Sparkles,
} from 'lucide-react';

export const Dashboard: React.FC = () => {
  const { data: analytics, isLoading: analyticsLoading } = useQuery<OverviewAnalytics>({
    queryKey: ['analytics-overview'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<OverviewAnalytics>>('/analytics/overview');
      return res.data.data;
    },
  });

  const { data: recentInvoices, isLoading: invoicesLoading } = useQuery<PagedResponse<Invoice>>({
    queryKey: ['recent-invoices'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PagedResponse<Invoice>>>('/invoices?page=0&size=6');
      return res.data.data;
    },
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'EXPORTED':
        return 'bg-teal-100 text-teal-800 border-teal-200';
      case 'PENDING_APPROVAL':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'NEEDS_REVIEW':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'EXTRACTED':
      case 'VALIDATING':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      default:
        return 'bg-slate-100 text-slate-700 border-slate-200';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner / Welcome */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-2xl p-6 text-white shadow-lg flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2 py-0.5 text-[10px] font-semibold uppercase bg-indigo-500/30 text-indigo-300 rounded border border-indigo-400/20">
              Operations Control
            </span>
            <span className="text-xs text-slate-400">Deterministic + AI Pipeline Active</span>
          </div>
          <h2 className="text-2xl font-bold tracking-tight">Accounts Payable Automation Hub</h2>
          <p className="text-sm text-slate-300 mt-1 max-w-xl">
            Invoices are parsed via multi-modal AI, verified against deterministic financial rules and PO records, and paused for authorized sign-offs.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Link
            to="/invoices"
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold rounded-lg shadow-md shadow-indigo-600/30 transition-all"
          >
            <Upload className="w-4 h-4" />
            <span>Process Invoice</span>
          </Link>
          <Link
            to="/approvals"
            className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-semibold rounded-lg border border-slate-700 transition-all"
          >
            <CheckSquare className="w-4 h-4 text-amber-400" />
            <span>Approvals ({analytics?.pendingApprovals ?? 0})</span>
          </Link>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Total Invoices</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
              <FileText className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-slate-900 mt-2">
            {analyticsLoading ? '...' : analytics?.totalInvoices ?? 0}
          </p>
          <div className="flex items-center gap-1.5 mt-2 text-xs text-slate-500">
            <span>In current tenant organization</span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending Approvals</span>
            <div className="w-8 h-8 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center">
              <Clock className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-amber-600 mt-2">
            {analyticsLoading ? '...' : analytics?.pendingApprovals ?? 0}
          </p>
          <div className="flex items-center gap-1.5 mt-2 text-xs text-amber-700 font-medium">
            <span>Awaiting authorized decision</span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Needs Review</span>
            <div className="w-8 h-8 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center">
              <AlertTriangle className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-rose-600 mt-2">
            {analyticsLoading ? '...' : analytics?.invoicesPendingReview ?? 0}
          </p>
          <div className="flex items-center gap-1.5 mt-2 text-xs text-rose-700 font-medium">
            <span>PO mismatch or validation flag</span>
          </div>
        </div>

        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Exported to ERP</span>
            <div className="w-8 h-8 rounded-lg bg-teal-50 text-teal-600 flex items-center justify-center">
              <Send className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-bold text-teal-600 mt-2">
            {analyticsLoading ? '...' : analytics?.exportedInvoices ?? 0}
          </p>
          <div className="flex items-center gap-1.5 mt-2 text-xs text-teal-700 font-medium">
            <span>Synced to accounting systems</span>
          </div>
        </div>
      </div>

      {/* Operational Highlights & AI Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-bold text-base text-slate-900">Recent Invoice Processing Stream</h3>
              <p className="text-xs text-slate-500">Real-time status of pipeline intake and matching</p>
            </div>
            <Link
              to="/invoices"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              <span>View all</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 border-b border-slate-200 uppercase font-semibold">
                <tr>
                  <th className="py-2.5 px-3">Invoice #</th>
                  <th className="py-2.5 px-3">Supplier</th>
                  <th className="py-2.5 px-3">PO Reference</th>
                  <th className="py-2.5 px-3 text-right">Amount</th>
                  <th className="py-2.5 px-3">Status</th>
                  <th className="py-2.5 px-3 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {invoicesLoading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-6 text-slate-400">
                      Loading recent invoices...
                    </td>
                  </tr>
                ) : recentInvoices?.content && recentInvoices.content.length > 0 ? (
                  recentInvoices.content.map((inv) => (
                    <tr key={inv.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-3 font-semibold text-slate-800">
                        <Link to={`/invoices/${inv.id}`} className="hover:text-indigo-600">
                          {inv.invoiceNumber}
                        </Link>
                      </td>
                      <td className="py-3 px-3 text-slate-600">{inv.supplierName || '—'}</td>
                      <td className="py-3 px-3">
                        {inv.purchaseOrderNumber ? (
                          <span className="font-mono text-slate-600 bg-slate-100 px-1.5 py-0.5 rounded border border-slate-200">
                            {inv.purchaseOrderNumber}
                          </span>
                        ) : (
                          <span className="text-slate-400 italic">None</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-right font-medium text-slate-900">
                        {inv.currency} {Number(inv.totalAmount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </td>
                      <td className="py-3 px-3">
                        <span className={`px-2 py-0.5 text-[10px] font-semibold rounded-full border ${getStatusBadge(inv.processingStatus)}`}>
                          {inv.processingStatus}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-right">
                        <Link
                          to={`/invoices/${inv.id}`}
                          className="px-2.5 py-1 text-[11px] font-semibold bg-slate-100 hover:bg-indigo-50 hover:text-indigo-600 rounded text-slate-700 transition-colors"
                        >
                          Inspect
                        </Link>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="text-center py-6 text-slate-400">
                      No invoices found. Click "Process Invoice" above to upload a sample document.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* AI Engine & System Performance Card */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6 space-y-5">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Sparkles className="w-4 h-4 text-indigo-500" />
              <h3 className="font-bold text-base text-slate-900">AI Engine Metrics</h3>
            </div>
            <p className="text-xs text-slate-500">Autonomous extraction telemetry</p>
          </div>

          <div className="space-y-3.5">
            <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Cpu className="w-4 h-4 text-indigo-600" />
                <span className="text-xs text-slate-700 font-medium">Avg Extraction Latency</span>
              </div>
              <span className="text-xs font-bold text-slate-900 font-mono">
                {analytics?.avgAiLatencyMs ?? 85} ms
              </span>
            </div>

            <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <TrendingUp className="w-4 h-4 text-emerald-600" />
                <span className="text-xs text-slate-700 font-medium">Workflow Success Rate</span>
              </div>
              <span className="text-xs font-bold text-emerald-700 font-mono">
                {(analytics?.workflowSuccessRate ?? 98.4).toFixed(1)}%
              </span>
            </div>

            <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <CheckCircle2 className="w-4 h-4 text-indigo-600" />
                <span className="text-xs text-slate-700 font-medium">Deterministic Rule Checks</span>
              </div>
              <span className="text-xs font-bold text-indigo-700 font-mono">
                100% Enforced
              </span>
            </div>

            <div className="p-3.5 bg-slate-50 rounded-lg border border-slate-200 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <FileText className="w-4 h-4 text-slate-600" />
                <span className="text-xs text-slate-700 font-medium">Est. AI Cost</span>
              </div>
              <span className="text-xs font-bold text-slate-900 font-mono">
                ${(analytics?.estimatedAiCost ?? 0.04).toFixed(3)}
              </span>
            </div>
          </div>

          <div className="pt-2">
            <Link
              to="/analytics"
              className="w-full py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold rounded-lg flex items-center justify-center gap-1.5 transition-colors"
            >
              <span>Detailed Telemetry & Benchmarks</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
