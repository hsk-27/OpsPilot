import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/client';
import { PurchaseOrder, PagedResponse, ApiResponse, Supplier } from '../types';
import { useAuth } from '../context/AuthContext';
import { Plus, Search, Calendar, ChevronDown, ChevronRight, X, ChevronLeft } from 'lucide-react';

export const PurchaseOrders: React.FC = () => {
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const canCreate = hasRole(['OWNER', 'ADMIN', 'OPERATOR']);

  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [formData, setFormData] = useState({
    purchaseOrderNumber: '',
    supplierId: '',
    issueDate: new Date().toISOString().split('T')[0],
    currency: 'USD',
    lines: [
      { description: 'Cloud Computing Infrastructure Services', quantity: 1, unitPrice: 1200.0, taxRate: 0.1 },
    ],
  });

  const { data, isLoading } = useQuery<PagedResponse<PurchaseOrder>>({
    queryKey: ['purchase-orders', page],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PagedResponse<PurchaseOrder>>>(`/purchase-orders?page=${page}&size=10`);
      return res.data.data;
    },
  });

  const { data: suppliers } = useQuery<PagedResponse<Supplier>>({
    queryKey: ['suppliers-select'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PagedResponse<Supplier>>>('/suppliers?page=0&size=50');
      return res.data.data;
    },
  });

  const createMutation = useMutation({
    mutationFn: async (payload: any) => {
      const res = await api.post<ApiResponse<PurchaseOrder>>('/purchase-orders', payload);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['purchase-orders'] });
      setIsModalOpen(false);
      setFormData({
        purchaseOrderNumber: '',
        supplierId: '',
        issueDate: new Date().toISOString().split('T')[0],
        currency: 'USD',
        lines: [{ description: '', quantity: 1, unitPrice: 0, taxRate: 0.1 }],
      });
      setError(null);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Failed to create purchase order');
    },
  });

  const handleAddLine = () => {
    setFormData({
      ...formData,
      lines: [...formData.lines, { description: '', quantity: 1, unitPrice: 0, taxRate: 0.1 }],
    });
  };

  const handleUpdateLine = (index: number, field: string, value: any) => {
    const updated = [...formData.lines];
    updated[index] = { ...updated[index], [field]: value };
    setFormData({ ...formData, lines: updated });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  const filtered = data?.content?.filter((po) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return po.purchaseOrderNumber.toLowerCase().includes(q);
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 tracking-tight">Purchase Orders</h2>
          <p className="text-xs text-slate-500">
            Approved purchase authorizations for 2-way and 3-way matching validation
          </p>
        </div>
        {canCreate && (
          <button
            onClick={() => setIsModalOpen(true)}
            className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-semibold shadow-md shadow-indigo-600/30 transition-all self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" />
            <span>New Purchase Order</span>
          </button>
        )}
      </div>

      {/* Search */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search PO number..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3.5 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {/* PO List */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 border-b border-slate-200 uppercase font-semibold">
              <tr>
                <th className="py-3 px-4 w-10"></th>
                <th className="py-3 px-4">PO Number</th>
                <th className="py-3 px-4">Issue Date</th>
                <th className="py-3 px-4 text-right">Subtotal</th>
                <th className="py-3 px-4 text-right">Tax</th>
                <th className="py-3 px-4 text-right">Total Amount</th>
                <th className="py-3 px-4">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    Loading purchase orders...
                  </td>
                </tr>
              ) : filtered && filtered.length > 0 ? (
                filtered.map((po) => {
                  const isExpanded = expandedId === po.id;
                  return (
                    <React.Fragment key={po.id}>
                      <tr
                        onClick={() => setExpandedId(isExpanded ? null : po.id)}
                        className="hover:bg-slate-50/80 cursor-pointer transition-colors"
                      >
                        <td className="py-3.5 px-4 text-slate-400">
                          {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                        </td>
                        <td className="py-3.5 px-4 font-mono font-bold text-slate-900">
                          {po.purchaseOrderNumber}
                        </td>
                        <td className="py-3.5 px-4 text-slate-600 flex items-center gap-1.5">
                          <Calendar className="w-3.5 h-3.5 text-slate-400" />
                          <span>{po.issueDate}</span>
                        </td>
                        <td className="py-3.5 px-4 text-right font-mono text-slate-700">
                          {po.currency} {Number(po.subtotal).toFixed(2)}
                        </td>
                        <td className="py-3.5 px-4 text-right font-mono text-slate-700">
                          {po.currency} {Number(po.taxAmount).toFixed(2)}
                        </td>
                        <td className="py-3.5 px-4 text-right font-mono font-bold text-slate-900">
                          {po.currency} {Number(po.totalAmount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                        </td>
                        <td className="py-3.5 px-4">
                          <span className="px-2 py-0.5 text-[10px] font-semibold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                            {po.status}
                          </span>
                        </td>
                      </tr>

                      {/* Expanded Line Items */}
                      {isExpanded && (
                        <tr className="bg-slate-50/70">
                          <td colSpan={7} className="p-4 pl-12">
                            <div className="bg-white rounded-lg border border-slate-200 p-4 shadow-sm">
                              <h4 className="text-xs font-bold text-slate-800 mb-2 uppercase tracking-wider">
                                Order Lines ({po.lines?.length || 0})
                              </h4>
                              {po.lines && po.lines.length > 0 ? (
                                <table className="w-full text-left text-xs">
                                  <thead className="text-slate-500 border-b border-slate-100 pb-1">
                                    <tr>
                                      <th className="py-1.5 px-2">Description</th>
                                      <th className="py-1.5 px-2 text-right">Quantity</th>
                                      <th className="py-1.5 px-2 text-right">Unit Price</th>
                                      <th className="py-1.5 px-2 text-right">Line Total</th>
                                    </tr>
                                  </thead>
                                  <tbody className="divide-y divide-slate-50">
                                    {po.lines.map((l) => (
                                      <tr key={l.id}>
                                        <td className="py-1.5 px-2 text-slate-700">{l.description}</td>
                                        <td className="py-1.5 px-2 text-right font-mono">{l.quantity}</td>
                                        <td className="py-1.5 px-2 text-right font-mono">${Number(l.unitPrice).toFixed(2)}</td>
                                        <td className="py-1.5 px-2 text-right font-mono font-bold">${Number(l.lineTotal).toFixed(2)}</td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              ) : (
                                <p className="text-slate-400 text-xs">No lines attached to this purchase order.</p>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })
              ) : (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-400">
                    No purchase orders found. Click "New Purchase Order" to create one.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {data && data.totalPages > 1 && (
          <div className="p-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
            <span>Showing page {data.page + 1} of {data.totalPages}</span>
            <div className="flex items-center gap-1">
              <button
                disabled={data.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="p-1 rounded border border-slate-200 hover:bg-white disabled:opacity-40"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                disabled={data.last}
                onClick={() => setPage((p) => p + 1)}
                className="p-1 rounded border border-slate-200 hover:bg-white disabled:opacity-40"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-bold text-base text-slate-900">Create Purchase Order</h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {error && (
              <div className="mt-3 p-3 bg-rose-50 border border-rose-200 rounded-lg text-xs text-rose-800">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="mt-4 space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">PO Number</label>
                  <input
                    type="text"
                    required
                    placeholder="PO-2026-9901"
                    value={formData.purchaseOrderNumber}
                    onChange={(e) => setFormData({ ...formData, purchaseOrderNumber: e.target.value })}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500 font-mono"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Issue Date</label>
                  <input
                    type="date"
                    required
                    value={formData.issueDate}
                    onChange={(e) => setFormData({ ...formData, issueDate: e.target.value })}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Supplier</label>
                <select
                  value={formData.supplierId}
                  onChange={(e) => setFormData({ ...formData, supplierId: e.target.value })}
                  className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="">Select an approved supplier...</option>
                  {suppliers?.content?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.supplierCode})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-semibold text-slate-700">Line Items</label>
                  <button
                    type="button"
                    onClick={handleAddLine}
                    className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold"
                  >
                    + Add Line
                  </button>
                </div>
                <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                  {formData.lines.map((line, idx) => (
                    <div key={idx} className="p-2.5 bg-slate-50 border border-slate-200 rounded-lg space-y-2">
                      <input
                        type="text"
                        placeholder="Item description"
                        required
                        value={line.description}
                        onChange={(e) => handleUpdateLine(idx, 'description', e.target.value)}
                        className="w-full p-1.5 bg-white border border-slate-200 rounded text-xs"
                      />
                      <div className="grid grid-cols-3 gap-2">
                        <div>
                          <label className="text-[10px] text-slate-500">Qty</label>
                          <input
                            type="number"
                            min="1"
                            value={line.quantity}
                            onChange={(e) => handleUpdateLine(idx, 'quantity', parseInt(e.target.value) || 1)}
                            className="w-full p-1 bg-white border border-slate-200 rounded text-xs"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-500">Unit Price</label>
                          <input
                            type="number"
                            step="0.01"
                            value={line.unitPrice}
                            onChange={(e) => handleUpdateLine(idx, 'unitPrice', parseFloat(e.target.value) || 0)}
                            className="w-full p-1 bg-white border border-slate-200 rounded text-xs"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-500">Tax Rate</label>
                          <input
                            type="number"
                            step="0.01"
                            value={line.taxRate}
                            onChange={(e) => handleUpdateLine(idx, 'taxRate', parseFloat(e.target.value) || 0)}
                            className="w-full p-1 bg-white border border-slate-200 rounded text-xs"
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="px-4 py-1.5 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg shadow-sm"
                >
                  {createMutation.isPending ? 'Creating...' : 'Save Purchase Order'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
