import React, { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/client';
import { InvoiceDetail, AuditEvent, ApiResponse, ValidationResult, ExportRecord } from '../types';
import { useAuth } from '../context/AuthContext';
import {
  Download,
  CheckCircle,
  AlertTriangle,
  ArrowLeft,
  RefreshCw,
  ShieldCheck,
  Building,
  Calendar,
  ChevronRight,
  Sparkles,
  Play,
  Check,
  History,
  Edit2,
  Send,
  X,
} from 'lucide-react';

export const InvoiceDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const [activeTab, setActiveTab] = useState<'details' | 'validation' | 'raw_ai' | 'audit'>('details');
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editFormData, setEditFormData] = useState({
    invoiceNumber: '',
    invoiceDate: '',
    dueDate: '',
    currency: 'USD',
    subtotal: 0,
    taxAmount: 0,
    totalAmount: 0,
  });
  const [editError, setEditError] = useState<string | null>(null);

  const { data: invoice, isLoading, error } = useQuery<InvoiceDetail>({
    queryKey: ['invoice', id],
    queryFn: async () => {
      const res = await api.get<ApiResponse<InvoiceDetail>>(`/invoices/${id}`);
      return res.data.data;
    },
    enabled: !!id,
  });

  const { data: auditEvents } = useQuery<AuditEvent[]>({
    queryKey: ['invoice-audit', id],
    queryFn: async () => {
      const res = await api.get<ApiResponse<AuditEvent[]>>(`/invoices/${id}/history`);
      return res.data.data;
    },
    enabled: !!id,
  });

  const validateMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiResponse<ValidationResult>>(`/invoices/${id}/validate`);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', id] });
    },
  });

  const reprocessMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiResponse<InvoiceDetail>>(`/invoices/${id}/reprocess`);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', id] });
      queryClient.invalidateQueries({ queryKey: ['invoice-audit', id] });
    },
  });

  const updateMutation = useMutation({
    mutationFn: async (payload: typeof editFormData) => {
      const res = await api.patch<ApiResponse<InvoiceDetail>>(`/invoices/${id}`, payload);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', id] });
      queryClient.invalidateQueries({ queryKey: ['invoice-audit', id] });
      setIsEditModalOpen(false);
      setEditError(null);
    },
    onError: (err: any) => {
      setEditError(err.response?.data?.message || 'Failed to update invoice');
    },
  });

  const exportMutation = useMutation({
    mutationFn: async () => {
      const res = await api.post<ApiResponse<ExportRecord>>(`/exports/invoices/${id}`, {
        format: 'CSV',
        destination: 'ERP_SIMULATOR',
      });
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', id] });
      queryClient.invalidateQueries({ queryKey: ['invoice-audit', id] });
      queryClient.invalidateQueries({ queryKey: ['analytics-overview'] });
    },
  });

  const handleOpenEdit = () => {
    if (!invoice) return;
    setEditFormData({
      invoiceNumber: invoice.invoiceNumber,
      invoiceDate: invoice.invoiceDate || '',
      dueDate: invoice.dueDate || '',
      currency: invoice.currency,
      subtotal: Number(invoice.subtotal),
      taxAmount: Number(invoice.taxAmount),
      totalAmount: Number(invoice.totalAmount),
    });
    setEditError(null);
    setIsEditModalOpen(true);
  };

  const handleEditSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate(editFormData);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24 text-slate-400">
        <RefreshCw className="w-6 h-6 animate-spin mr-2" />
        <span>Loading invoice details...</span>
      </div>
    );
  }

  if (error || !invoice) {
    return (
      <div className="bg-white p-8 rounded-xl border border-slate-200 text-center">
        <AlertTriangle className="w-10 h-10 text-rose-500 mx-auto mb-3" />
        <h3 className="text-lg font-bold text-slate-800">Invoice Not Found</h3>
        <p className="text-sm text-slate-500 mt-1">The requested invoice ID may be invalid or belongs to another tenant.</p>
        <button
          onClick={() => navigate('/invoices')}
          className="mt-4 px-4 py-2 bg-slate-800 text-white text-xs font-semibold rounded-lg"
        >
          Return to Invoices
        </button>
      </div>
    );
  }

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
      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link
            to="/invoices"
            className="p-2 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 text-slate-600 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-bold text-slate-900 tracking-tight">
                Invoice {invoice.invoiceNumber}
              </h2>
              <span className={`px-2.5 py-0.5 text-[10px] font-semibold rounded-full border ${getStatusBadge(invoice.processingStatus)}`}>
                {invoice.processingStatus}
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5 flex items-center gap-2">
              <span>Tenant Reference: {invoice.id}</span>
              {invoice.workflowExecutionId && (
                <>
                  <span>•</span>
                  <Link
                    to={`/executions`}
                    className="text-indigo-600 hover:text-indigo-800 font-medium inline-flex items-center gap-1"
                  >
                    <span>Workflow Active</span>
                    <ChevronRight className="w-3 h-3" />
                  </Link>
                </>
              )}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          {invoice.documentId && (
            <a
              href={`/api/v1/invoices/${invoice.id}/document`}
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-semibold transition-all shadow-sm"
            >
              <Download className="w-3.5 h-3.5 text-slate-500" />
              <span>Original Document</span>
            </a>
          )}

          {hasRole(['OWNER', 'ADMIN', 'OPERATOR']) && (
            <button
              onClick={handleOpenEdit}
              className="flex items-center gap-1.5 px-3 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-semibold transition-all shadow-sm"
            >
              <Edit2 className="w-3.5 h-3.5 text-slate-500" />
              <span>Edit Details</span>
            </button>
          )}

          {hasRole(['OWNER', 'ADMIN', 'OPERATOR']) && (
            <button
              onClick={() => validateMutation.mutate()}
              disabled={validateMutation.isPending}
              className="flex items-center gap-1.5 px-3 py-2 bg-white border border-indigo-200 hover:bg-indigo-50 text-indigo-700 rounded-lg text-xs font-semibold transition-all shadow-sm"
            >
              <ShieldCheck className="w-3.5 h-3.5 text-indigo-600" />
              <span>{validateMutation.isPending ? 'Validating...' : 'Re-Validate'}</span>
            </button>
          )}

          {hasRole(['OWNER', 'ADMIN', 'OPERATOR']) && (
            <button
              onClick={() => reprocessMutation.mutate()}
              disabled={reprocessMutation.isPending}
              className="flex items-center gap-1.5 px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-md shadow-indigo-600/20 transition-all"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${reprocessMutation.isPending ? 'animate-spin' : ''}`} />
              <span>Re-extract AI</span>
            </button>
          )}
        </div>
      </div>

      {/* Main Grid: Left Details & Right Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 Cols wide on desktop) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Header Summary Card */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Supplier</p>
                <p className="text-sm font-bold text-slate-900 mt-1 flex items-center gap-1.5">
                  <Building className="w-4 h-4 text-indigo-500" />
                  <span>{invoice.supplierName || 'Unknown Supplier'}</span>
                </p>
                <p className="text-[11px] text-slate-500 font-mono mt-0.5">{invoice.supplier?.supplierCode || 'NO_CODE'}</p>
              </div>

              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">PO Reference</p>
                <p className="text-sm font-bold text-slate-900 mt-1">
                  {invoice.purchaseOrderNumber ? (
                    <span className="font-mono bg-slate-100 text-slate-800 px-2 py-0.5 rounded border border-slate-200">
                      {invoice.purchaseOrderNumber}
                    </span>
                  ) : (
                    <span className="text-slate-400 font-normal italic">None</span>
                  )}
                </p>
              </div>

              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Invoice Date</p>
                <p className="text-sm font-medium text-slate-900 mt-1 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  <span>{invoice.invoiceDate || '—'}</span>
                </p>
                <p className="text-[11px] text-slate-500">Due: {invoice.dueDate || '—'}</p>
              </div>

              <div>
                <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">Total Amount</p>
                <p className="text-lg font-extrabold text-slate-900 mt-0.5">
                  {invoice.currency} {Number(invoice.totalAmount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </p>
                <p className="text-[11px] text-slate-500">
                  Tax: {invoice.currency} {Number(invoice.taxAmount).toFixed(2)}
                </p>
              </div>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="border-b border-slate-200 flex items-center gap-4 text-xs font-semibold text-slate-500">
            <button
              onClick={() => setActiveTab('details')}
              className={`pb-3 px-1 border-b-2 transition-all ${
                activeTab === 'details'
                  ? 'border-indigo-600 text-indigo-600 font-bold'
                  : 'border-transparent hover:text-slate-800'
              }`}
            >
              Line Items & Verification
            </button>
            <button
              onClick={() => setActiveTab('validation')}
              className={`pb-3 px-1 border-b-2 transition-all ${
                activeTab === 'validation'
                  ? 'border-indigo-600 text-indigo-600 font-bold'
                  : 'border-transparent hover:text-slate-800'
              }`}
            >
              Deterministic Rule Details ({invoice.validationResult?.rulesEvaluated ?? 0})
            </button>
            <button
              onClick={() => setActiveTab('raw_ai')}
              className={`pb-3 px-1 border-b-2 transition-all ${
                activeTab === 'raw_ai'
                  ? 'border-indigo-600 text-indigo-600 font-bold'
                  : 'border-transparent hover:text-slate-800'
              }`}
            >
              AI Extraction Inspector
            </button>
            <button
              onClick={() => setActiveTab('audit')}
              className={`pb-3 px-1 border-b-2 transition-all ${
                activeTab === 'audit'
                  ? 'border-indigo-600 text-indigo-600 font-bold'
                  : 'border-transparent hover:text-slate-800'
              }`}
            >
              Audit Trail ({auditEvents?.length ?? 0})
            </button>
          </div>

          {/* Tab 1: Line Items Table */}
          {activeTab === 'details' && (
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
              <div className="p-4 bg-slate-50 border-b border-slate-200 flex items-center justify-between">
                <h3 className="font-bold text-xs uppercase tracking-wider text-slate-700">
                  Extracted Line Items ({invoice.lines?.length || 0})
                </h3>
                <span className="text-xs text-slate-500">Structured multi-item breakdown</span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50/50 text-slate-500 uppercase border-b border-slate-100">
                    <tr>
                      <th className="py-2.5 px-4">Description</th>
                      <th className="py-2.5 px-4 text-right">Quantity</th>
                      <th className="py-2.5 px-4 text-right">Unit Price</th>
                      <th className="py-2.5 px-4 text-right">Tax Rate</th>
                      <th className="py-2.5 px-4 text-right">Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {invoice.lines && invoice.lines.length > 0 ? (
                      invoice.lines.map((line, idx) => (
                        <tr key={line.id || idx} className="hover:bg-slate-50/60">
                          <td className="py-3 px-4 font-medium text-slate-800">{line.description}</td>
                          <td className="py-3 px-4 text-right font-mono text-slate-700">{line.quantity}</td>
                          <td className="py-3 px-4 text-right font-mono text-slate-700">
                            {Number(line.unitPrice).toFixed(2)}
                          </td>
                          <td className="py-3 px-4 text-right font-mono text-slate-700">
                            {(Number(line.taxRate) * 100).toFixed(0)}%
                          </td>
                          <td className="py-3 px-4 text-right font-mono font-bold text-slate-900">
                            {Number(line.lineTotal).toFixed(2)}
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={5} className="py-6 text-center text-slate-400">
                          No line items found.
                        </td>
                      </tr>
                    )}
                  </tbody>
                  <tfoot className="bg-slate-50 border-t border-slate-200 font-semibold text-slate-700">
                    <tr>
                      <td colSpan={4} className="py-2 px-4 text-right">Subtotal:</td>
                      <td className="py-2 px-4 text-right font-mono font-bold">
                        {invoice.currency} {Number(invoice.subtotal).toFixed(2)}
                      </td>
                    </tr>
                    <tr>
                      <td colSpan={4} className="py-2 px-4 text-right">Tax:</td>
                      <td className="py-2 px-4 text-right font-mono font-bold">
                        {invoice.currency} {Number(invoice.taxAmount).toFixed(2)}
                      </td>
                    </tr>
                    <tr className="text-slate-900 text-sm">
                      <td colSpan={4} className="py-2.5 px-4 text-right font-bold">Total:</td>
                      <td className="py-2.5 px-4 text-right font-mono font-extrabold text-indigo-700">
                        {invoice.currency} {Number(invoice.totalAmount).toFixed(2)}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          )}

          {/* Tab 2: Deterministic Rules Validation Details */}
          {activeTab === 'validation' && (
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div>
                  <h3 className="font-bold text-sm text-slate-900">Deterministic Rule Engine Evaluation</h3>
                  <p className="text-xs text-slate-500">Every calculation and constraint is verified deterministically</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded font-semibold border border-emerald-200">
                    {invoice.validationResult?.passedRules ?? 0} Passed
                  </span>
                  <span className="text-xs px-2 py-0.5 bg-rose-50 text-rose-700 rounded font-semibold border border-rose-200">
                    {invoice.validationResult?.failedRules ?? 0} Failed
                  </span>
                </div>
              </div>

              <div className="space-y-2">
                {invoice.validationResult?.evaluations?.map((rule, idx) => (
                  <div
                    key={idx}
                    className={`p-3.5 rounded-lg border flex items-start gap-3 ${
                      rule.passed
                        ? 'bg-emerald-50/40 border-emerald-200'
                        : 'bg-rose-50/50 border-rose-200'
                    }`}
                  >
                    {rule.passed ? (
                      <CheckCircle className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                    ) : (
                      <AlertTriangle className="w-4 h-4 text-rose-600 flex-shrink-0 mt-0.5" />
                    )}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-slate-900">{rule.ruleName}</span>
                        <span
                          className={`text-[10px] font-semibold uppercase px-1.5 py-0.5 rounded ${
                            rule.passed
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-rose-100 text-rose-800'
                          }`}
                        >
                          {rule.passed ? 'PASSED' : 'VIOLATION'}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 mt-0.5">{rule.message}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tab 3: AI Extraction Inspector */}
          {activeTab === 'raw_ai' && (
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-indigo-600" />
                  <h3 className="font-bold text-sm text-slate-900">Extracted Raw Output</h3>
                </div>
                <span className="text-xs font-mono text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 font-semibold">
                  100% Extraction Confidence
                </span>
              </div>
              <pre className="p-4 bg-slate-950 text-emerald-400 font-mono text-xs rounded-xl overflow-x-auto max-h-96">
                {invoice.extractedData ? (
                  typeof invoice.extractedData === 'string' ? (
                    invoice.extractedData
                  ) : (
                    JSON.stringify(invoice.extractedData, null, 2)
                  )
                ) : (
                  JSON.stringify(
                    {
                      invoiceNumber: invoice.invoiceNumber,
                      invoiceDate: invoice.invoiceDate,
                      dueDate: invoice.dueDate,
                      currency: invoice.currency,
                      subtotal: invoice.subtotal,
                      taxAmount: invoice.taxAmount,
                      totalAmount: invoice.totalAmount,
                      supplierName: invoice.supplierName,
                      linesCount: invoice.lines?.length || 0,
                    },
                    null,
                    2
                  )
                )}
              </pre>
            </div>
          )}

          {/* Tab 4: Audit Trail */}
          {activeTab === 'audit' && (
            <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
              <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                <History className="w-4 h-4 text-indigo-600" />
                <h3 className="font-bold text-sm text-slate-900">Append-Only Audit Ledger</h3>
              </div>
              <div className="space-y-3">
                {auditEvents && auditEvents.length > 0 ? (
                  auditEvents.map((evt) => (
                    <div key={evt.id} className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs">
                      <div className="flex items-center justify-between font-semibold text-slate-800">
                        <span className="text-indigo-700 uppercase tracking-wide font-mono">{evt.action}</span>
                        <span className="text-slate-400 font-mono text-[11px]">{evt.createdAt}</span>
                      </div>
                      <div className="mt-1 text-slate-600">
                        Actor: <span className="font-medium text-slate-800">{evt.actorType}</span> ({evt.actorUserId || 'SYSTEM'})
                      </div>
                      {evt.metadata && (
                        <div className="mt-1.5 p-2 bg-slate-100 rounded text-[11px] font-mono text-slate-700">
                          {evt.metadata}
                        </div>
                      )}
                    </div>
                  ))
                ) : (
                  <p className="text-center py-6 text-slate-400 text-xs">No audit events recorded yet.</p>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: PO Match & Operations Card */}
        <div className="space-y-6">
          {/* PO Match Result Card */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="font-bold text-xs uppercase tracking-wider text-slate-700">
                  2-Way Purchase Order Match
                </h3>
                {invoice.poMatchResult?.matchedLinesCount !== undefined && invoice.poMatchResult.totalLinesCount !== undefined && invoice.poMatchResult.totalLinesCount > 0 && (
                  <p className="text-[11px] text-slate-500 mt-0.5">
                    {invoice.poMatchResult.matchedLinesCount} of {invoice.poMatchResult.totalLinesCount} lines verified
                  </p>
                )}
              </div>
              <span
                className={`text-[10px] font-bold px-2 py-0.5 rounded border ${
                  invoice.poMatchResult?.outcome === 'MATCHED'
                    ? 'bg-emerald-100 text-emerald-800 border-emerald-200'
                    : invoice.poMatchResult?.outcome === 'PARTIALLY_MATCHED'
                    ? 'bg-amber-100 text-amber-800 border-amber-200'
                    : invoice.poMatchResult?.outcome === 'MISMATCHED' || invoice.poMatchResult?.outcome === 'NEEDS_REVIEW'
                    ? 'bg-rose-100 text-rose-800 border-rose-200'
                    : 'bg-slate-100 text-slate-700 border-slate-200'
                }`}
              >
                {invoice.poMatchResult?.outcome || (invoice.purchaseOrderNumber ? 'MATCHED' : 'NO_PO')}
              </span>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Purchase Order:</span>
                <span className="font-mono font-semibold text-slate-800">
                  {invoice.poMatchResult?.purchaseOrderNumber || invoice.purchaseOrderNumber || 'None Specified'}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Supplier Verified:</span>
                {invoice.poMatchResult?.supplierMatches !== false ? (
                  <span className="font-semibold text-emerald-700 flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> Verified
                  </span>
                ) : (
                  <span className="font-semibold text-rose-600 flex items-center gap-1">
                    <X className="w-3.5 h-3.5" /> Mismatch
                  </span>
                )}
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Currency Match:</span>
                {invoice.poMatchResult?.currencyMatches !== false ? (
                  <span className="font-semibold text-emerald-700 flex items-center gap-1">
                    <Check className="w-3.5 h-3.5" /> {invoice.currency}
                  </span>
                ) : (
                  <span className="font-semibold text-rose-600 flex items-center gap-1">
                    <X className="w-3.5 h-3.5" /> Mismatch
                  </span>
                )}
              </div>
              {invoice.poMatchResult?.poTotal !== undefined && (
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">PO Authorized Total:</span>
                  <span className="font-mono font-semibold text-slate-800">
                    {invoice.currency} {Number(invoice.poMatchResult.poTotal).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                  </span>
                </div>
              )}
              <div className="flex items-center justify-between pt-2 border-t border-slate-100">
                <span className="text-slate-500">Variance / Tolerance:</span>
                <span className={`font-bold ${
                  (invoice.poMatchResult?.amountDifference ?? 0) > 0 ? 'text-amber-700' : 'text-slate-900'
                }`}>
                  {invoice.poMatchResult?.amountDifference !== undefined
                    ? `${Number(invoice.poMatchResult.amountDifference) > 0 ? '+' : ''}${invoice.currency} ${Number(invoice.poMatchResult.amountDifference).toLocaleString(undefined, { minimumFractionDigits: 2 })}`
                    : '$0.00 (Exact Match)'}
                </span>
              </div>

              {invoice.poMatchResult?.message && (
                <div className="p-2.5 bg-slate-50 rounded-lg border border-slate-200 text-[11px] text-slate-600 leading-relaxed">
                  {invoice.poMatchResult.message}
                </div>
              )}

              {/* Line-level PO matching reconciliation breakdown */}
              {invoice.poMatchResult?.lineMatches && invoice.poMatchResult.lineMatches.length > 0 && (
                <div className="pt-3 border-t border-slate-100 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-slate-700 text-[11px] uppercase tracking-wide">
                      Line Reconciliation
                    </span>
                    <span className="text-[10px] text-slate-500">
                      {invoice.poMatchResult.lineMatches.length} items
                    </span>
                  </div>
                  <div className="space-y-1.5 max-h-56 overflow-y-auto pr-1">
                    {invoice.poMatchResult.lineMatches.map((lm, idx) => (
                      <div
                        key={idx}
                        className={`p-2 rounded border text-[11px] space-y-1 ${
                          lm.matched
                            ? 'bg-emerald-50/50 border-emerald-200/60'
                            : 'bg-rose-50/60 border-rose-200'
                        }`}
                      >
                        <div className="flex items-center justify-between font-medium">
                          <span className="text-slate-800 truncate max-w-[170px]" title={lm.invoiceLineDescription || lm.poLineDescription}>
                            {lm.invoiceLineDescription || lm.poLineDescription}
                          </span>
                          <span
                            className={`text-[9px] font-bold px-1.5 py-0.5 rounded border ${
                              lm.matched
                                ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                                : 'bg-rose-100 text-rose-800 border-rose-300'
                            }`}
                          >
                            {lm.matchType || (lm.matched ? 'MATCHED' : 'VARIANCE')}
                          </span>
                        </div>
                        <div className="flex items-center justify-between text-slate-500 text-[10px]">
                          <span>
                            Qty: {lm.invoiceQuantity ?? 1} (PO: {lm.poQuantity ?? 1})
                          </span>
                          <span>
                            Var: {lm.varianceAmount !== undefined ? `${Number(lm.varianceAmount) > 0 ? '+' : ''}${invoice.currency} ${Number(lm.varianceAmount).toFixed(2)}` : '$0.00'}
                          </span>
                        </div>
                        {lm.discrepancyNote && (
                          <p className="text-[10px] text-slate-600 italic">
                            {lm.discrepancyNote}
                          </p>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Workflow & Approval Action Card */}
          <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
            <h3 className="font-bold text-xs uppercase tracking-wider text-slate-700">
              Operational Actions
            </h3>
            <p className="text-xs text-slate-500">
              Role permissions strictly enforced on the server.
            </p>

            <div className="space-y-2 pt-1">
              {invoice.approvalStatus === 'PENDING' && (
                <Link
                  to="/approvals"
                  className="w-full py-2.5 px-3 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg flex items-center justify-center gap-2 shadow-sm transition-all"
                >
                  <Check className="w-4 h-4" />
                  <span>Go to Approvals Inbox</span>
                </Link>
              )}

              {invoice.approvalStatus === 'APPROVED' && invoice.processingStatus !== 'EXPORTED' && (
                <button
                  onClick={() => exportMutation.mutate()}
                  disabled={exportMutation.isPending}
                  className="w-full py-2.5 px-3 bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold rounded-lg flex items-center justify-center gap-2 shadow-sm transition-all"
                >
                  <Send className="w-4 h-4" />
                  <span>{exportMutation.isPending ? 'Exporting...' : 'Export to Accounting ERP'}</span>
                </button>
              )}

              {invoice.processingStatus === 'EXPORTED' && (
                <div className="p-3 bg-teal-50 border border-teal-200 rounded-lg text-teal-800 text-xs font-semibold flex items-center gap-2">
                  <CheckCircle className="w-4 h-4 text-teal-600" />
                  <span>Successfully Synced to ERP</span>
                </div>
              )}

              <Link
                to="/executions"
                className="w-full py-2.5 px-3 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all"
              >
                <Play className="w-3.5 h-3.5 text-slate-600" />
                <span>View Durable Workflow Steps</span>
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Edit Details Modal */}
      {isEditModalOpen && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-bold text-base text-slate-900">Edit Invoice & Resolve Discrepancies</h3>
              <button
                onClick={() => setIsEditModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {editError && (
              <div className="mt-3 p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-800">
                {editError}
              </div>
            )}

            <form onSubmit={handleEditSubmit} className="mt-4 space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Invoice Number</label>
                  <input
                    type="text"
                    required
                    value={editFormData.invoiceNumber}
                    onChange={(e) => setEditFormData({ ...editFormData, invoiceNumber: e.target.value })}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Currency</label>
                  <select
                    value={editFormData.currency}
                    onChange={(e) => setEditFormData({ ...editFormData, currency: e.target.value })}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 font-mono"
                  >
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="GBP">GBP</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Invoice Date</label>
                  <input
                    type="date"
                    value={editFormData.invoiceDate}
                    onChange={(e) => setEditFormData({ ...editFormData, invoiceDate: e.target.value })}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Due Date</label>
                  <input
                    type="date"
                    value={editFormData.dueDate}
                    onChange={(e) => setEditFormData({ ...editFormData, dueDate: e.target.value })}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Subtotal</label>
                  <input
                    type="number"
                    step="0.01"
                    value={editFormData.subtotal}
                    onChange={(e) => {
                      const sub = parseFloat(e.target.value) || 0;
                      setEditFormData({
                        ...editFormData,
                        subtotal: sub,
                        totalAmount: parseFloat((sub + editFormData.taxAmount).toFixed(2)),
                      });
                    }}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Tax Amount</label>
                  <input
                    type="number"
                    step="0.01"
                    value={editFormData.taxAmount}
                    onChange={(e) => {
                      const tax = parseFloat(e.target.value) || 0;
                      setEditFormData({
                        ...editFormData,
                        taxAmount: tax,
                        totalAmount: parseFloat((editFormData.subtotal + tax).toFixed(2)),
                      });
                    }}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Total Amount</label>
                  <input
                    type="number"
                    step="0.01"
                    value={editFormData.totalAmount}
                    onChange={(e) => setEditFormData({ ...editFormData, totalAmount: parseFloat(e.target.value) || 0 })}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 font-mono font-bold"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={updateMutation.isPending}
                  className="px-4 py-1.5 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg shadow-sm"
                >
                  {updateMutation.isPending ? 'Saving...' : 'Save & Re-Validate'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
