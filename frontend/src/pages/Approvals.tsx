import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { ApprovalTask, PagedResponse, ApiResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  CheckCircle2,
  XCircle,
  HelpCircle,
  Clock,
  Building,
  FileText,
  ShieldAlert,
  ExternalLink,
  ArrowUpRight,
} from 'lucide-react';

export const Approvals: React.FC = () => {
  const queryClient = useQueryClient();
  const { user, hasRole } = useAuth();
  const canDecide = hasRole(['OWNER', 'ADMIN', 'APPROVER']);

  const [selectedTask, setSelectedTask] = useState<ApprovalTask | null>(null);
  const [decisionType, setDecisionType] = useState<'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED'>('APPROVED');
  const [comment, setComment] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);

  const [escalateTask, setEscalateTask] = useState<ApprovalTask | null>(null);
  const [escalateReason, setEscalateReason] = useState('');

  const { data, isLoading } = useQuery<PagedResponse<ApprovalTask>>({
    queryKey: ['approvals-inbox'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PagedResponse<ApprovalTask>>>('/approvals/inbox');
      return res.data.data;
    },
  });

  const decisionMutation = useMutation({
    mutationFn: async ({ taskId, decision, commentText }: { taskId: string; decision: string; commentText: string }) => {
      const res = await api.post<ApiResponse<any>>(`/approvals/${taskId}/decide`, {
        decision,
        comment: commentText,
      });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['approvals-inbox'] });
      queryClient.invalidateQueries({ queryKey: ['analytics-overview'] });
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      setSelectedTask(null);
      setComment('');
      setActionError(null);
    },
    onError: (err: any) => {
      setActionError(err.response?.data?.message || 'Failed to submit approval decision');
    },
  });

  const escalateMutation = useMutation({
    mutationFn: async ({ taskId, reason }: { taskId: string; reason: string }) => {
      const res = await api.post<ApiResponse<any>>(`/approvals/${taskId}/escalate`, { reason });
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['approvals-inbox'] });
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      setEscalateTask(null);
      setEscalateReason('');
      setActionError(null);
    },
    onError: (err: any) => {
      setActionError(err.response?.data?.message || 'Failed to escalate approval task');
    },
  });

  const handleOpenDecision = (task: ApprovalTask, type: 'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED') => {
    setSelectedTask(task);
    setDecisionType(type);
    setComment('');
    setActionError(null);
  };

  const handleConfirmDecision = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTask) return;
    decisionMutation.mutate({
      taskId: selectedTask.id,
      decision: decisionType,
      commentText: comment,
    });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight">Approval Task Inbox</h2>
          <p className="text-xs text-slate-500">
            Human-in-the-loop governance for high-value or exception-flagged invoices
          </p>
        </div>
        {!canDecide && (
          <div className="p-2.5 bg-amber-50 border border-amber-200 rounded-lg flex items-center gap-2 text-amber-800 text-xs">
            <ShieldAlert className="w-4 h-4 flex-shrink-0" />
            <span>
              Your role is <strong>{user?.role}</strong>. Approval decisions require <strong>APPROVER</strong>, <strong>ADMIN</strong>, or <strong>OWNER</strong>.
            </span>
          </div>
        )}
      </div>

      {/* Task List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="bg-white p-12 rounded-xl border border-slate-200 text-center text-slate-400 text-xs">
            Loading approval tasks...
          </div>
        ) : data?.content && data.content.length > 0 ? (
          data.content.map((task) => {
            const inv = task.invoice;
            return (
              <div
                key={task.id}
                className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 hover:border-indigo-300 transition-all"
              >
                <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                  <div className="space-y-2">
                    <div className="flex items-center gap-3">
                      <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-indigo-50 text-indigo-700 border border-indigo-200">
                        Task: {task.assignedRole || 'APPROVER'}
                      </span>
                      <span className="text-xs text-slate-400 font-mono">
                        ID: {task.id.substring(0, 8)}...
                      </span>
                      <span className="text-xs text-slate-400 flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5" />
                        Created: {task.createdAt}
                      </span>
                    </div>

                    <div className="flex items-center gap-4 flex-wrap">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-indigo-600" />
                        <Link
                          to={`/invoices/${task.invoiceId}`}
                          className="font-bold text-sm text-slate-900 hover:text-indigo-600 flex items-center gap-1"
                        >
                          <span>{inv?.invoiceNumber || `Invoice #${task.invoiceId.substring(0, 8)}`}</span>
                          <ExternalLink className="w-3 h-3 text-slate-400" />
                        </Link>
                      </div>

                      <div className="flex items-center gap-1.5 text-xs text-slate-600">
                        <Building className="w-3.5 h-3.5 text-slate-400" />
                        <span>{inv?.supplierName || 'Verified Supplier'}</span>
                      </div>

                      {inv?.purchaseOrderNumber && (
                        <div className="text-xs text-slate-600">
                          PO: <code className="bg-slate-100 px-1.5 py-0.5 rounded text-slate-800">{inv.purchaseOrderNumber}</code>
                        </div>
                      )}

                      <div className="text-sm font-extrabold text-slate-900">
                        {inv?.currency || 'USD'} {Number(inv?.totalAmount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 pt-2 lg:pt-0 border-t lg:border-t-0 border-slate-100">
                    <button
                      onClick={() => handleOpenDecision(task, 'APPROVED')}
                      disabled={!canDecide}
                      className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-40 text-white rounded-lg text-xs font-semibold shadow-sm flex items-center gap-1.5 transition-all"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Approve</span>
                    </button>

                    <button
                      onClick={() => handleOpenDecision(task, 'CHANGES_REQUESTED')}
                      disabled={!canDecide}
                      className="px-3.5 py-2 bg-amber-500 hover:bg-amber-600 disabled:opacity-40 text-white rounded-lg text-xs font-semibold shadow-sm flex items-center gap-1.5 transition-all"
                    >
                      <HelpCircle className="w-4 h-4" />
                      <span>Request Changes</span>
                    </button>

                    <button
                      onClick={() => handleOpenDecision(task, 'REJECTED')}
                      disabled={!canDecide}
                      className="px-3.5 py-2 bg-rose-600 hover:bg-rose-700 disabled:opacity-40 text-white rounded-lg text-xs font-semibold shadow-sm flex items-center gap-1.5 transition-all"
                    >
                      <XCircle className="w-4 h-4" />
                      <span>Reject</span>
                    </button>

                    <button
                      onClick={() => {
                        setEscalateTask(task);
                        setEscalateReason('');
                        setActionError(null);
                      }}
                      className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all"
                      title="Escalate task to Admin / Owner"
                    >
                      <ArrowUpRight className="w-4 h-4 text-indigo-600" />
                      <span>Escalate</span>
                    </button>
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <div className="bg-white p-12 rounded-xl border border-slate-200 text-center space-y-2">
            <CheckCircle2 className="w-12 h-12 text-emerald-500 mx-auto" />
            <h3 className="font-bold text-base text-slate-800">Inbox is Clear</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              There are no pending approvals requiring attention right now. Newly uploaded invoices with amounts exceeding workflow approval thresholds will appear here.
            </p>
          </div>
        )}
      </div>

      {/* Decision Modal */}
      {selectedTask && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="font-bold text-base text-slate-900">
              Submit Approval Decision: <span className="uppercase text-indigo-600">{decisionType}</span>
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              For Invoice {selectedTask.invoice?.invoiceNumber || selectedTask.invoiceId}
            </p>

            {actionError && (
              <div className="mt-3 p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-800">
                {actionError}
              </div>
            )}

            <form onSubmit={handleConfirmDecision} className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Approval Notes / Justification:
                </label>
                <textarea
                  rows={3}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder={
                    decisionType === 'APPROVED'
                      ? 'Verified purchase order items and budget allocation...'
                      : 'Specify reason for rejection or requested corrections...'
                  }
                  required={decisionType !== 'APPROVED'}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setSelectedTask(null)}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={decisionMutation.isPending}
                  className={`px-4 py-2 text-xs font-semibold text-white rounded-lg shadow-sm ${
                    decisionType === 'APPROVED'
                      ? 'bg-emerald-600 hover:bg-emerald-700'
                      : decisionType === 'CHANGES_REQUESTED'
                      ? 'bg-amber-500 hover:bg-amber-600'
                      : 'bg-rose-600 hover:bg-rose-700'
                  }`}
                >
                  {decisionMutation.isPending ? 'Recording...' : `Confirm ${decisionType}`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Escalation Modal */}
      {escalateTask && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="font-bold text-base text-slate-900 flex items-center gap-2">
              <ArrowUpRight className="w-5 h-5 text-indigo-600" />
              Escalate Approval Task to Management
            </h3>
            <p className="text-xs text-slate-500 mt-1">
              Reassigns this task from {escalateTask.assignedRole} to ADMIN/OWNER for elevated authority review.
            </p>

            {actionError && (
              <div className="mt-3 p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-800">
                {actionError}
              </div>
            )}

            <form
              onSubmit={(e) => {
                e.preventDefault();
                escalateMutation.mutate({ taskId: escalateTask.id, reason: escalateReason });
              }}
              className="mt-4 space-y-4"
            >
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Escalation Justification:
                </label>
                <textarea
                  rows={3}
                  value={escalateReason}
                  onChange={(e) => setEscalateReason(e.target.value)}
                  placeholder="e.g. Exceeds department authorization limit; requires executive CFO sign-off..."
                  required
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setEscalateTask(null)}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={escalateMutation.isPending}
                  className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm"
                >
                  {escalateMutation.isPending ? 'Escalating...' : 'Confirm Escalation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
