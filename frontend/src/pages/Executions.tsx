import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/client';
import { WorkflowExecution, WorkflowNodeExecution, PagedResponse, ApiResponse } from '../types';
import { useAuth } from '../context/AuthContext';
import { ChevronLeft, ChevronRight } from 'lucide-react';

export const Executions: React.FC = () => {
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const canCancel = hasRole(['OWNER', 'ADMIN']);

  const [page, setPage] = useState(0);
  const [selectedExecutionId, setSelectedExecutionId] = useState<string | null>(null);

  const { data: executions, isLoading } = useQuery<PagedResponse<WorkflowExecution>>({
    queryKey: ['executions-list', page],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PagedResponse<WorkflowExecution>>>(`/executions?page=${page}&size=10`);
      return res.data.data;
    },
  });

  const { data: nodeExecutions, isLoading: nodesLoading } = useQuery<WorkflowNodeExecution[]>({
    queryKey: ['node-executions', selectedExecutionId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<WorkflowNodeExecution[]>>(`/executions/${selectedExecutionId}/nodes`);
      return res.data.data;
    },
    enabled: !!selectedExecutionId,
  });

  const cancelMutation = useMutation({
    mutationFn: async (id: string) => {
      const res = await api.post<ApiResponse<WorkflowExecution>>(`/executions/${id}/cancel`);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['executions-list'] });
    },
  });

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      case 'RUNNING':
        return 'bg-blue-100 text-blue-800 border-blue-200 animate-pulse';
      case 'WAITING_APPROVAL':
        return 'bg-amber-100 text-amber-800 border-amber-200';
      case 'FAILED':
        return 'bg-rose-100 text-rose-800 border-rose-200';
      case 'CANCELLED':
        return 'bg-slate-100 text-slate-700 border-slate-200';
      default:
        return 'bg-slate-100 text-slate-600 border-slate-200';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-900 tracking-tight">Workflow Executions</h2>
        <p className="text-xs text-slate-500">
          Durable state machine execution history, step audit trail, and runtime snapshots
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Executions Table (Left 2 cols) */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="p-4 bg-slate-50 border-b border-slate-200 font-bold text-xs uppercase tracking-wider text-slate-700">
            Pipeline Execution Instances
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50/50 text-slate-500 uppercase border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Execution ID</th>
                  <th className="py-3 px-4">Reference</th>
                  <th className="py-3 px-4">Trigger</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-400">
                      Loading executions...
                    </td>
                  </tr>
                ) : executions?.content && executions.content.length > 0 ? (
                  executions.content.map((exec) => {
                    const isSelected = selectedExecutionId === exec.id;
                    return (
                      <tr
                        key={exec.id}
                        onClick={() => setSelectedExecutionId(exec.id)}
                        className={`hover:bg-slate-50 cursor-pointer transition-colors ${
                          isSelected ? 'bg-indigo-50/60 font-semibold' : ''
                        }`}
                      >
                        <td className="py-3.5 px-4 font-mono text-indigo-600">
                          {exec.id.substring(0, 8)}...
                        </td>
                        <td className="py-3.5 px-4 text-slate-700">
                          <span className="font-semibold text-slate-900">{exec.businessReferenceType}</span>
                          <span className="text-[11px] text-slate-400 block font-mono">
                            {exec.businessReferenceId?.substring(0, 8)}...
                          </span>
                        </td>
                        <td className="py-3.5 px-4 font-mono text-slate-500">{exec.triggerType}</td>
                        <td className="py-3.5 px-4">
                          <span className={`px-2 py-0.5 text-[10px] font-semibold rounded-full border ${getStatusBadge(exec.status)}`}>
                            {exec.status}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 text-right">
                          {canCancel && (exec.status === 'RUNNING' || exec.status === 'WAITING_APPROVAL') && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                cancelMutation.mutate(exec.id);
                              }}
                              className="px-2 py-1 text-[11px] text-rose-600 hover:bg-rose-50 rounded border border-rose-200"
                            >
                              Cancel
                            </button>
                          )}
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={5} className="py-8 text-center text-slate-400">
                      No executions recorded yet. Process an invoice to trigger a workflow.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {executions && executions.totalPages > 1 && (
            <div className="p-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
              <span>Showing page {executions.page + 1} of {executions.totalPages}</span>
              <div className="flex items-center gap-1">
                <button
                  disabled={executions.first}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="p-1 rounded border border-slate-200 hover:bg-white disabled:opacity-40"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  disabled={executions.last}
                  onClick={() => setPage((p) => p + 1)}
                  className="p-1 rounded border border-slate-200 hover:bg-white disabled:opacity-40"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Node-by-Node Timeline Inspector (Right 1 col) */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <h3 className="font-bold text-xs uppercase tracking-wider text-slate-700">
              Execution Node Timeline
            </h3>
            {selectedExecutionId && (
              <span className="text-[11px] font-mono text-slate-500">
                {selectedExecutionId.substring(0, 8)}...
              </span>
            )}
          </div>

          {!selectedExecutionId ? (
            <div className="py-12 text-center text-slate-400 text-xs">
              Select an execution instance on the left to inspect node transitions and duration metrics.
            </div>
          ) : nodesLoading ? (
            <div className="py-12 text-center text-slate-400 text-xs">
              Loading node transitions...
            </div>
          ) : nodeExecutions && nodeExecutions.length > 0 ? (
            <div className="relative pl-6 space-y-4 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
              {nodeExecutions.map((node) => (
                <div key={node.id} className="relative text-xs">
                  <div
                    className={`absolute -left-6 top-1 w-3 h-3 rounded-full border-2 bg-white ${
                      node.status === 'COMPLETED'
                        ? 'border-emerald-500 ring-2 ring-emerald-100'
                        : node.status === 'WAITING_APPROVAL'
                        ? 'border-amber-500 ring-2 ring-amber-100'
                        : node.status === 'FAILED'
                        ? 'border-rose-500 ring-2 ring-rose-100'
                        : 'border-slate-400'
                    }`}
                  />
                  <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-slate-800">{node.nodeId}</span>
                      <span className={`text-[10px] font-semibold px-1.5 py-0.2 rounded border ${getStatusBadge(node.status)}`}>
                        {node.status}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-[11px] text-slate-500 mt-1">
                      <span>Type: <code className="font-mono text-slate-700">{node.nodeType}</code></span>
                      {node.durationMs != null && (
                        <span>{node.durationMs} ms</span>
                      )}
                    </div>
                    {node.outputSnapshot && (
                      <details className="mt-2 text-[10px]">
                        <summary className="cursor-pointer text-indigo-600 hover:text-indigo-800 font-semibold">
                          View Output Snapshot
                        </summary>
                        <pre className="mt-1 p-2 bg-slate-900 text-emerald-400 rounded overflow-x-auto max-h-32">
                          {node.outputSnapshot}
                        </pre>
                      </details>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="py-12 text-center text-slate-400 text-xs">
              No node executions recorded for this instance.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
