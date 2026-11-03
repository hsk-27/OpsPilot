import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../api/client';
import { AuditEvent, PagedResponse, ApiResponse } from '../types';
import { Search, ChevronLeft, ChevronRight, User } from 'lucide-react';

export const Audit: React.FC = () => {
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedEvent, setSelectedEvent] = useState<AuditEvent | null>(null);

  const { data, isLoading } = useQuery<PagedResponse<AuditEvent>>({
    queryKey: ['audit-events', page],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PagedResponse<AuditEvent>>>(`/audit-events?page=${page}&size=20`);
      return res.data.data;
    },
  });

  const filtered = data?.content?.filter((evt) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      evt.action.toLowerCase().includes(q) ||
      evt.entityType.toLowerCase().includes(q) ||
      evt.entityId.toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-900 tracking-tight">Compliance & Audit Trail</h2>
        <p className="text-xs text-slate-500">
          Append-only immutable record of all system events, state transitions, and user actions
        </p>
      </div>

      {/* Search Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search by action, entity..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3.5 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {/* Audit Events Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 text-slate-600 border-b border-slate-200 uppercase font-semibold">
              <tr>
                <th className="py-3 px-4">Timestamp</th>
                <th className="py-3 px-4">Action</th>
                <th className="py-3 px-4">Entity Type</th>
                <th className="py-3 px-4">Entity ID</th>
                <th className="py-3 px-4">Actor</th>
                <th className="py-3 px-4 text-right">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    Loading audit trail...
                  </td>
                </tr>
              ) : filtered && filtered.length > 0 ? (
                filtered.map((evt) => (
                  <tr key={evt.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3.5 px-4 font-mono text-slate-500 text-[11px] whitespace-nowrap">
                      {evt.createdAt}
                    </td>
                    <td className="py-3.5 px-4 font-bold text-indigo-700 font-mono uppercase tracking-wider">
                      {evt.action}
                    </td>
                    <td className="py-3.5 px-4 font-semibold text-slate-700">
                      {evt.entityType}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">
                      {evt.entityId.substring(0, 8)}...
                    </td>
                    <td className="py-3.5 px-4">
                      <span className="inline-flex items-center gap-1 text-slate-700 font-medium">
                        <User className="w-3 h-3 text-slate-400" />
                        <span>{evt.actorType}</span>
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-right">
                      <button
                        onClick={() => setSelectedEvent(evt)}
                        className="px-2.5 py-1 text-[11px] font-semibold bg-slate-100 hover:bg-slate-200 rounded text-slate-700 transition-colors"
                      >
                        Inspect
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    No audit records match the current criteria.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {data && data.totalPages > 1 && (
          <div className="p-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
            <span>
              Showing page {data.page + 1} of {data.totalPages} ({data.totalElements} total)
            </span>
            <div className="flex items-center gap-1.5">
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

      {/* Inspect Event Modal */}
      {selectedEvent && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-slate-100 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-bold text-base text-slate-900 font-mono">
                {selectedEvent.action}
              </h3>
              <button
                onClick={() => setSelectedEvent(null)}
                className="text-xs font-semibold text-slate-500 hover:text-slate-800"
              >
                Close
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Entity:</span>
                <span className="font-mono font-semibold text-slate-800">
                  {selectedEvent.entityType} ({selectedEvent.entityId})
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Actor:</span>
                <span className="font-semibold text-slate-800">
                  {selectedEvent.actorType} (ID: {selectedEvent.actorUserId || 'SYSTEM'})
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Timestamp:</span>
                <span className="font-mono text-slate-600">{selectedEvent.createdAt}</span>
              </div>

              {selectedEvent.metadata && (
                <div className="pt-2">
                  <span className="font-semibold text-slate-700 block mb-1">Metadata:</span>
                  <pre className="p-3 bg-slate-900 text-emerald-400 font-mono rounded-lg overflow-x-auto text-[11px]">
                    {selectedEvent.metadata}
                  </pre>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
