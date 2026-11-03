import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/client';
import {
  WorkflowDefinition,
  WorkflowVersion,
  WorkflowGraph,
  NodeType,
  PagedResponse,
  ApiResponse,
} from '../types';
import { useAuth } from '../context/AuthContext';
import {
  GitBranch,
  Play,
  CheckCircle2,
  AlertTriangle,
  Save,
  Send,
  X,
  FileText,
  ShieldCheck,
  CheckSquare,
  Layers,
  Sparkles,
} from 'lucide-react';

const DEFAULT_GRAPH: WorkflowGraph = {
  nodes: [
    { id: 'node_start', type: 'START', label: 'Start Intake', x: 80, y: 180 },
    { id: 'node_ai', type: 'AI_EXTRACTION', label: 'AI Extraction', x: 260, y: 180, config: { provider: 'MOCK', temperature: 0.1 } },
    { id: 'node_validate', type: 'VALIDATE_INVOICE', label: 'Deterministic Validation', x: 460, y: 180, config: { strictTolerances: true } },
    { id: 'node_po_match', type: 'MATCH_PURCHASE_ORDER', label: 'PO 2-Way Match', x: 680, y: 180, config: { toleranceAmount: 1.0 } },
    { id: 'node_condition', type: 'CONDITION', label: 'Amount > $5,000?', x: 890, y: 180, config: { expression: 'invoice.totalAmount > 5000' } },
    { id: 'node_approval', type: 'APPROVAL', label: 'Manager Approval', x: 1100, y: 90, config: { assignedRole: 'APPROVER', deadlineHours: 48 } },
    { id: 'node_export', type: 'EXPORT_INVOICE', label: 'ERP Accounting Export', x: 1320, y: 180, config: { format: 'CSV', destination: 'ERP_SIMULATOR' } },
    { id: 'node_end', type: 'END', label: 'Completed', x: 1520, y: 180 },
  ],
  edges: [
    { id: 'e1', source: 'node_start', target: 'node_ai' },
    { id: 'e2', source: 'node_ai', target: 'node_validate' },
    { id: 'e3', source: 'node_validate', target: 'node_po_match' },
    { id: 'e4', source: 'node_po_match', target: 'node_condition' },
    { id: 'e5', source: 'node_condition', target: 'node_approval', conditionBranch: 'true' },
    { id: 'e6', source: 'node_condition', target: 'node_export', conditionBranch: 'false' },
    { id: 'e7', source: 'node_approval', target: 'node_export' },
    { id: 'e8', source: 'node_export', target: 'node_end' },
  ],
};

export const WorkflowBuilder: React.FC = () => {
  const queryClient = useQueryClient();
  const { hasRole } = useAuth();
  const canPublish = hasRole(['OWNER', 'ADMIN']);

  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string | null>(null);
  const [graph, setGraph] = useState<WorkflowGraph>(DEFAULT_GRAPH);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [validationReport, setValidationReport] = useState<{
    valid: boolean;
    errors: string[];
    warnings: string[];
  } | null>(null);
  const [publishMessage, setPublishMessage] = useState<string | null>(null);

  // Load Workflows
  const { data: workflowsData } = useQuery<PagedResponse<WorkflowDefinition>>({
    queryKey: ['workflows-list'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<PagedResponse<WorkflowDefinition>>>('/workflows?page=0&size=10');
      return res.data.data;
    },
  });

  useEffect(() => {
    if (workflowsData?.content && workflowsData.content.length > 0 && !selectedWorkflowId) {
      setSelectedWorkflowId(workflowsData.content[0].id);
    }
  }, [workflowsData, selectedWorkflowId]);

  // Load Selected Workflow Version / Graph
  const { data: workflowDetail } = useQuery<{ definition: WorkflowDefinition; latestVersion?: WorkflowVersion }>({
    queryKey: ['workflow-detail', selectedWorkflowId],
    queryFn: async () => {
      const res = await api.get<ApiResponse<any>>(`/workflows/${selectedWorkflowId}`);
      return res.data.data;
    },
    enabled: !!selectedWorkflowId,
  });

  useEffect(() => {
    if (workflowDetail?.latestVersion?.graphDefinitionJson) {
      try {
        const parsed = JSON.parse(workflowDetail.latestVersion.graphDefinitionJson);
        if (parsed.nodes && parsed.edges) {
          setGraph(parsed);
        }
      } catch {
        // fallback to default graph
      }
    }
  }, [workflowDetail]);

  const validateMutation = useMutation({
    mutationFn: async () => {
      if (!selectedWorkflowId) return;
      const res = await api.post<ApiResponse<any>>(`/workflows/${selectedWorkflowId}/validate`, graph);
      return res.data.data;
    },
    onSuccess: (data) => {
      setValidationReport(data);
    },
    onError: (err: any) => {
      setValidationReport({
        valid: false,
        errors: [err.response?.data?.message || 'Validation request failed'],
        warnings: [],
      });
    },
  });

  const publishMutation = useMutation({
    mutationFn: async () => {
      if (!selectedWorkflowId) return;
      const res = await api.post<ApiResponse<WorkflowVersion>>(`/workflows/${selectedWorkflowId}/publish`, graph);
      return res.data.data;
    },
    onSuccess: (version) => {
      queryClient.invalidateQueries({ queryKey: ['workflow-detail', selectedWorkflowId] });
      const versionNumber = version?.versionNumber ?? 1;
      setPublishMessage(`Version ${versionNumber} successfully published and activated!`);
      setValidationReport({ valid: true, errors: [], warnings: [] });
      setTimeout(() => setPublishMessage(null), 5000);
    },
    onError: (err: any) => {
      setValidationReport({
        valid: false,
        errors: [err.response?.data?.message || 'Publishing failed'],
        warnings: [],
      });
    },
  });

  const selectedNode = graph.nodes.find((n) => n.id === selectedNodeId);

  const handleUpdateNodeConfig = (key: string, value: any) => {
    if (!selectedNodeId) return;
    setGraph({
      ...graph,
      nodes: graph.nodes.map((node) => {
        if (node.id === selectedNodeId) {
          return {
            ...node,
            config: {
              ...(node.config || {}),
              [key]: value,
            },
          };
        }
        return node;
      }),
    });
  };

  const getNodeIcon = (type: NodeType) => {
    switch (type) {
      case 'START':
        return <Play className="w-4 h-4 text-emerald-600" />;
      case 'AI_EXTRACTION':
        return <Sparkles className="w-4 h-4 text-indigo-600" />;
      case 'VALIDATE_INVOICE':
        return <ShieldCheck className="w-4 h-4 text-cyan-600" />;
      case 'MATCH_PURCHASE_ORDER':
        return <FileText className="w-4 h-4 text-blue-600" />;
      case 'CONDITION':
        return <GitBranch className="w-4 h-4 text-amber-600" />;
      case 'APPROVAL':
        return <CheckSquare className="w-4 h-4 text-purple-600" />;
      case 'EXPORT_INVOICE':
        return <Send className="w-4 h-4 text-teal-600" />;
      case 'END':
        return <CheckCircle2 className="w-4 h-4 text-emerald-600" />;
      default:
        return <Layers className="w-4 h-4 text-slate-600" />;
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header & Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-bold text-slate-900 tracking-tight">Visual Workflow Builder</h2>
            {workflowDetail?.latestVersion && (
              <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-indigo-50 text-indigo-700 border border-indigo-200">
                v{workflowDetail.latestVersion.versionNumber} Active
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500">
            Construct deterministic operational pipelines, condition branches, and human approvals
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={() => validateMutation.mutate()}
            disabled={validateMutation.isPending}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-semibold shadow-sm transition-all"
          >
            <ShieldCheck className="w-4 h-4 text-indigo-600" />
            <span>{validateMutation.isPending ? 'Validating...' : 'Validate Graph'}</span>
          </button>

          {canPublish && (
            <button
              onClick={() => publishMutation.mutate()}
              disabled={publishMutation.isPending}
              className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-lg text-xs font-semibold shadow-md shadow-indigo-600/30 transition-all"
            >
              <Save className="w-4 h-4" />
              <span>{publishMutation.isPending ? 'Publishing...' : 'Publish Version'}</span>
            </button>
          )}
        </div>
      </div>

      {/* Validation or Success Banner */}
      {publishMessage && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-2.5 text-emerald-800 text-xs font-medium">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
          <span>{publishMessage}</span>
        </div>
      )}

      {validationReport && (
        <div
          className={`p-3.5 rounded-xl border flex items-start gap-3 text-xs ${
            validationReport.valid
              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
              : 'bg-rose-50 border-rose-200 text-rose-800'
          }`}
        >
          {validationReport.valid ? (
            <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
          ) : (
            <AlertTriangle className="w-4 h-4 text-rose-600 flex-shrink-0 mt-0.5" />
          )}
          <div className="flex-1">
            <span className="font-bold">
              {validationReport.valid ? 'Workflow Topology is Valid' : 'Graph Topology Errors Detected:'}
            </span>
            {validationReport.errors.length > 0 && (
              <ul className="list-disc pl-4 mt-1 space-y-0.5 text-[11px]">
                {validationReport.errors.map((err, i) => (
                  <li key={i}>{err}</li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {/* Workflow Canvas Workspace */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden flex flex-col min-h-[560px] relative">
        {/* Canvas Toolbar */}
        <div className="h-12 bg-slate-50 border-b border-slate-200 px-4 flex items-center justify-between text-xs text-slate-600">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-slate-700">Nodes ({graph.nodes.length})</span>
            <span className="text-slate-300">•</span>
            <span>Connections ({graph.edges.length})</span>
          </div>
          <div className="text-[11px] text-slate-400">
            Click any node to inspect and configure runtime parameters
          </div>
        </div>

        {/* Visual Graph Canvas (SVG + HTML Cards) */}
        <div className="flex-1 overflow-x-auto p-8 bg-[radial-gradient(#e2e8f0_1px,transparent_1px)] [background-size:16px_16px] relative min-w-[1650px] min-h-[460px]">
          {/* SVG Connection Lines */}
          <svg className="absolute inset-0 w-full h-full pointer-events-none" style={{ minWidth: '1650px', minHeight: '460px' }}>
            {graph.edges.map((edge) => {
              const srcNode = graph.nodes.find((n) => n.id === edge.source);
              const tgtNode = graph.nodes.find((n) => n.id === edge.target);
              if (!srcNode || !tgtNode) return null;

              const x1 = (srcNode.x ?? 0) + 160;
              const y1 = (srcNode.y ?? 0) + 40;
              const x2 = tgtNode.x ?? 0;
              const y2 = (tgtNode.y ?? 0) + 40;
              const dx = (x2 - x1) / 2;

              return (
                <g key={edge.id}>
                  <path
                    d={`M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`}
                    fill="none"
                    stroke={edge.conditionBranch === 'true' ? '#10b981' : edge.conditionBranch === 'false' ? '#f59e0b' : '#6366f1'}
                    strokeWidth="2.5"
                    strokeDasharray={edge.conditionBranch ? '4 2' : 'none'}
                  />
                  {edge.conditionBranch && (
                    <text
                      x={x1 + dx}
                      y={(y1 + y2) / 2 - 8}
                      textAnchor="middle"
                      className="text-[10px] font-bold fill-slate-700 font-mono"
                    >
                      {edge.conditionBranch.toUpperCase()}
                    </text>
                  )}
                </g>
              );
            })}
          </svg>

          {/* Interactive Node Cards */}
          {graph.nodes.map((node) => {
            const isSelected = selectedNodeId === node.id;
            return (
              <div
                key={node.id}
                onClick={() => setSelectedNodeId(node.id)}
                style={{
                  position: 'absolute',
                  left: `${node.x ?? 100}px`,
                  top: `${node.y ?? 100}px`,
                  width: '160px',
                }}
                className={`bg-white rounded-xl border-2 p-3 shadow-md cursor-pointer transition-all ${
                  isSelected
                    ? 'border-indigo-600 ring-4 ring-indigo-50 shadow-lg'
                    : 'border-slate-200 hover:border-indigo-300'
                }`}
              >
                <div className="flex items-center gap-2 mb-1.5">
                  <div className="w-6 h-6 rounded bg-slate-50 flex items-center justify-center flex-shrink-0 border border-slate-200">
                    {getNodeIcon(node.type)}
                  </div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 truncate">
                    {node.type}
                  </span>
                </div>
                <div className="text-xs font-bold text-slate-800 line-clamp-1">{node.label}</div>

                {node.type === 'APPROVAL' && (
                  <div className="mt-2 text-[10px] text-purple-700 bg-purple-50 px-1.5 py-0.5 rounded font-medium">
                    Role: {String(node.config?.assignedRole || 'APPROVER')}
                  </div>
                )}
                {node.type === 'CONDITION' && (
                  <div className="mt-2 text-[10px] text-amber-700 bg-amber-50 px-1.5 py-0.5 rounded font-mono truncate">
                    {String(node.config?.expression || 'total > 5000')}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Node Inspector Drawer */}
      {selectedNode && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-lg p-6 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div className="flex items-center gap-2.5">
              <div className="w-7 h-7 rounded bg-indigo-50 flex items-center justify-center">
                {getNodeIcon(selectedNode.type)}
              </div>
              <div>
                <h3 className="font-bold text-sm text-slate-900">{selectedNode.label}</h3>
                <span className="text-[10px] font-mono text-slate-500 uppercase">ID: {selectedNode.id}</span>
              </div>
            </div>
            <button
              onClick={() => setSelectedNodeId(null)}
              className="p-1 rounded text-slate-400 hover:text-slate-600"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
            {selectedNode.type === 'APPROVAL' && (
              <>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Required Approver Role</label>
                  <select
                    value={String(selectedNode.config?.assignedRole || 'APPROVER')}
                    onChange={(e) => handleUpdateNodeConfig('assignedRole', e.target.value)}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg"
                  >
                    <option value="APPROVER">APPROVER</option>
                    <option value="ADMIN">ADMIN</option>
                    <option value="OWNER">OWNER</option>
                  </select>
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Timeout Deadline (Hours)</label>
                  <input
                    type="number"
                    value={Number(selectedNode.config?.deadlineHours || 48)}
                    onChange={(e) => handleUpdateNodeConfig('deadlineHours', parseInt(e.target.value) || 24)}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg"
                  />
                </div>
              </>
            )}

            {selectedNode.type === 'CONDITION' && (
              <div className="md:col-span-2">
                <label className="block font-semibold text-slate-700 mb-1">Evaluation Expression</label>
                <input
                  type="text"
                  value={String(selectedNode.config?.expression || '')}
                  onChange={(e) => handleUpdateNodeConfig('expression', e.target.value)}
                  placeholder="invoice.totalAmount > 5000"
                  className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg font-mono text-xs"
                />
              </div>
            )}

            {selectedNode.type === 'EXPORT_INVOICE' && (
              <>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Export Format</label>
                  <select
                    value={String(selectedNode.config?.format || 'CSV')}
                    onChange={(e) => handleUpdateNodeConfig('format', e.target.value)}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg"
                  >
                    <option value="CSV">CSV Format</option>
                    <option value="JSON">JSON Schema</option>
                  </select>
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Destination Target</label>
                  <select
                    value={String(selectedNode.config?.destination || 'ERP_SIMULATOR')}
                    onChange={(e) => handleUpdateNodeConfig('destination', e.target.value)}
                    className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg"
                  >
                    <option value="ERP_SIMULATOR">ERP Simulator</option>
                    <option value="S3_BUCKET">Secure S3 Object Store</option>
                  </select>
                </div>
              </>
            )}

            {selectedNode.type === 'AI_EXTRACTION' && (
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Extraction Provider</label>
                <select
                  value={String(selectedNode.config?.provider || 'MOCK')}
                  onChange={(e) => handleUpdateNodeConfig('provider', e.target.value)}
                  className="w-full p-2 bg-slate-50 border border-slate-200 rounded-lg"
                >
                  <option value="MOCK">Deterministic Mock AI Engine</option>
                  <option value="GEMINI">Google Gemini Flash / Pro</option>
                </select>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
