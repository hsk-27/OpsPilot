import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/client';
import { OrganizationSettings, IntegrationConfig, ApiResponse } from '../types';
import {
  Settings as SettingsIcon,
  Server,
  Sliders,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Plus,
  Shield,
  Zap,
  Globe,
} from 'lucide-react';

export const Settings: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'settings' | 'integrations'>('settings');
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);

  // Settings query
  const { data: settingsData } = useQuery({
    queryKey: ['admin-settings'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<OrganizationSettings>>('/admin/settings');
      return res.data.data;
    },
  });

  // Integrations query
  const { data: integrationsData } = useQuery({
    queryKey: ['integrations'],
    queryFn: async () => {
      const res = await api.get<ApiResponse<IntegrationConfig[]>>('/integrations');
      return res.data.data;
    },
  });

  // Settings form state
  const [formData, setFormData] = useState<{
    approvalThresholdAmount: string;
    matchingToleranceAmount: string;
    duplicateWindowDays: string;
    autoExportApproved: boolean;
  }>({
    approvalThresholdAmount: '5000.00',
    matchingToleranceAmount: '1.00',
    duplicateWindowDays: '90',
    autoExportApproved: false,
  });

  React.useEffect(() => {
    if (settingsData) {
      setFormData({
        approvalThresholdAmount: String(settingsData.approvalThresholdAmount ?? 5000),
        matchingToleranceAmount: String(settingsData.matchingToleranceAmount ?? 1),
        duplicateWindowDays: String(settingsData.duplicateWindowDays ?? 90),
        autoExportApproved: Boolean(settingsData.autoExportApproved),
      });
    }
  }, [settingsData]);

  // Update settings mutation
  const updateSettingsMutation = useMutation({
    mutationFn: async (payload: {
      approvalThresholdAmount: number;
      matchingToleranceAmount: number;
      duplicateWindowDays: number;
      autoExportApproved: boolean;
    }) => {
      const res = await api.patch<ApiResponse<OrganizationSettings>>('/admin/settings', payload);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-settings'] });
      setSaveSuccess('Operational settings successfully updated.');
      setSaveError(null);
      setTimeout(() => setSaveSuccess(null), 4000);
    },
    onError: (err: any) => {
      setSaveError(err.response?.data?.message || 'Failed to update settings');
    },
  });

  const handleSaveSettings = (e: React.FormEvent) => {
    e.preventDefault();
    updateSettingsMutation.mutate({
      approvalThresholdAmount: parseFloat(formData.approvalThresholdAmount) || 5000,
      matchingToleranceAmount: parseFloat(formData.matchingToleranceAmount) || 1,
      duplicateWindowDays: parseInt(formData.duplicateWindowDays, 10) || 90,
      autoExportApproved: formData.autoExportApproved,
    });
  };

  // Add integration state
  const [showAddModal, setShowAddModal] = useState(false);
  const [newIntegration, setNewIntegration] = useState({
    name: '',
    integrationType: 'ERP_SIMULATOR',
    endpointUrl: '',
    authType: 'API_KEY',
    apiKey: '',
  });

  const createIntegrationMutation = useMutation({
    mutationFn: async (payload: typeof newIntegration) => {
      const res = await api.post<ApiResponse<IntegrationConfig>>('/integrations', payload);
      return res.data.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
      setShowAddModal(false);
      setNewIntegration({
        name: '',
        integrationType: 'ERP_SIMULATOR',
        endpointUrl: '',
        authType: 'API_KEY',
        apiKey: '',
      });
    },
  });

  // Test integration mutation
  const [testingId, setTestingId] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<{ id: string; message: string; latencyMs: number } | null>(null);

  const testIntegrationMutation = useMutation({
    mutationFn: async (id: string) => {
      setTestingId(id);
      const res = await api.post<ApiResponse<{ status: string; message: string; latencyMs: number }>>(
        `/integrations/${id}/test`
      );
      return { id, ...res.data.data };
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['integrations'] });
      setTestResult(data);
      setTestingId(null);
      setTimeout(() => setTestResult(null), 5000);
    },
    onError: () => {
      setTestingId(null);
    },
  });

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 flex items-center gap-2.5">
            <SettingsIcon className="w-6 h-6 text-indigo-600" />
            Operational Settings & Integrations
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Configure financial approval thresholds, deterministic matching tolerances, and external ERP adapters.
          </p>
        </div>

        {/* Tab switch */}
        <div className="flex bg-slate-200/80 p-1 rounded-xl">
          <button
            onClick={() => setActiveTab('settings')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg transition-all ${
              activeTab === 'settings'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Sliders className="w-3.5 h-3.5" />
            Operational Policies
          </button>
          <button
            onClick={() => setActiveTab('integrations')}
            className={`flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg transition-all ${
              activeTab === 'integrations'
                ? 'bg-white text-slate-900 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Server className="w-3.5 h-3.5" />
            External ERP Integrations
          </button>
        </div>
      </div>

      {saveSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center gap-3 text-emerald-800 text-sm">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
          <span>{saveSuccess}</span>
        </div>
      )}

      {saveError && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl flex items-center gap-3 text-rose-800 text-sm">
          <AlertCircle className="w-5 h-5 text-rose-600 flex-shrink-0" />
          <span>{saveError}</span>
        </div>
      )}

      {/* Tab 1: Operational Policies */}
      {activeTab === 'settings' && (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <div className="p-6 border-b border-slate-200 bg-slate-50/50">
            <h2 className="text-base font-semibold text-slate-900 flex items-center gap-2">
              <Shield className="w-4 h-4 text-indigo-600" />
              Automated Business Rules & Approval Bounds
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              These rules govern deterministic invoice validation and trigger required human authorization workflows.
            </p>
          </div>

          <form onSubmit={handleSaveSettings} className="p-6 space-y-6 max-w-2xl">
            {/* Approval Threshold */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Financial Approval Threshold Amount ($)
              </label>
              <div className="relative rounded-lg shadow-sm">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400 font-medium text-sm">$</span>
                <input
                  type="number"
                  step="0.01"
                  value={formData.approvalThresholdAmount}
                  onChange={(e) => setFormData({ ...formData, approvalThresholdAmount: e.target.value })}
                  className="w-full pl-8 pr-4 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              <p className="text-[11px] text-slate-500 mt-1.5">
                Invoices with total amount exceeding this threshold strictly require human authorization before accounting export.
              </p>
            </div>

            {/* Matching Tolerance */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Purchase Order Matching Tolerance ($)
              </label>
              <div className="relative rounded-lg shadow-sm">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400 font-medium text-sm">$</span>
                <input
                  type="number"
                  step="0.01"
                  value={formData.matchingToleranceAmount}
                  onChange={(e) => setFormData({ ...formData, matchingToleranceAmount: e.target.value })}
                  className="w-full pl-8 pr-4 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  required
                />
              </div>
              <p className="text-[11px] text-slate-500 mt-1.5">
                Maximum allowable variance between invoice total and PO amount before raising a price discrepancy.
              </p>
            </div>

            {/* Duplicate Window */}
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                Duplicate Invoice Detection Window (Days)
              </label>
              <input
                type="number"
                value={formData.duplicateWindowDays}
                onChange={(e) => setFormData({ ...formData, duplicateWindowDays: e.target.value })}
                className="w-full px-3.5 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                required
              />
              <p className="text-[11px] text-slate-500 mt-1.5">
                Invoices sharing identical supplier code and invoice number within this window are flagged as duplicate risks.
              </p>
            </div>

            {/* Auto-export toggle */}
            <div className="flex items-center justify-between p-4 bg-slate-50 rounded-xl border border-slate-200">
              <div>
                <span className="text-sm font-semibold text-slate-800">Automatic Accounting ERP Export</span>
                <p className="text-xs text-slate-500 mt-0.5">
                  Immediately trigger accounting export upon final human approval decision.
                </p>
              </div>
              <input
                type="checkbox"
                checked={formData.autoExportApproved}
                onChange={(e) => setFormData({ ...formData, autoExportApproved: e.target.checked })}
                className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500 cursor-pointer"
              />
            </div>

            <div className="pt-4 border-t border-slate-200 flex items-center justify-end">
              <button
                type="submit"
                disabled={updateSettingsMutation.isPending}
                className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-lg shadow-sm transition-colors flex items-center gap-2"
              >
                {updateSettingsMutation.isPending && <RefreshCw className="w-4 h-4 animate-spin" />}
                Save Operational Policies
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Tab 2: Integrations */}
      {activeTab === 'integrations' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-base font-semibold text-slate-900">Configured Integration Endpoints</h2>
              <p className="text-xs text-slate-500">
                Connect external ERP platforms, accounting systems, and webhook receivers.
              </p>
            </div>
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-2 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg shadow-sm transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Integration Adapter
            </button>
          </div>

          {testResult && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between text-emerald-800 text-sm">
              <div className="flex items-center gap-2.5">
                <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
                <span>{testResult.message}</span>
              </div>
              <span className="px-2.5 py-1 bg-emerald-100 font-mono text-xs rounded-full font-medium">
                Latency: {testResult.latencyMs}ms
              </span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {integrationsData?.map((item) => (
              <div key={item.id} className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-3">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 bg-indigo-50 text-indigo-600 rounded-lg flex items-center justify-center font-bold">
                      <Globe className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-slate-900">{item.name}</h3>
                      <span className="text-[10px] uppercase font-semibold text-slate-500 bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                        {item.integrationType}
                      </span>
                    </div>
                  </div>
                  <span className="px-2 py-0.5 text-[10px] font-semibold bg-emerald-100 text-emerald-800 rounded-full border border-emerald-200">
                    {item.status}
                  </span>
                </div>

                <div className="text-xs text-slate-600 space-y-1 bg-slate-50 p-3 rounded-lg border border-slate-100">
                  <p className="truncate">
                    <span className="font-semibold text-slate-700">Endpoint:</span> {item.endpointUrl || 'Local ERP Simulator Mock'}
                  </p>
                  <p>
                    <span className="font-semibold text-slate-700">Auth:</span> {item.authType} ({item.apiKeyMasked || 'Mock Credential'})
                  </p>
                  {item.lastTestedAt && (
                    <p className="text-[11px] text-slate-400">
                      Last Tested: {new Date(item.lastTestedAt).toLocaleString()} ({item.lastTestResult})
                    </p>
                  )}
                </div>

                <div className="pt-2 flex items-center justify-between">
                  <span className="text-[11px] text-slate-400">Deterministic Adapter</span>
                  <button
                    onClick={() => testIntegrationMutation.mutate(item.id)}
                    disabled={testingId === item.id}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg transition-colors"
                  >
                    {testingId === item.id ? (
                      <RefreshCw className="w-3.5 h-3.5 animate-spin text-indigo-600" />
                    ) : (
                      <Zap className="w-3.5 h-3.5 text-amber-500" />
                    )}
                    Test Connection
                  </button>
                </div>
              </div>
            ))}

            {(!integrationsData || integrationsData.length === 0) && (
              <div className="col-span-2 p-8 text-center bg-white rounded-xl border border-dashed border-slate-300">
                <Server className="w-8 h-8 text-slate-400 mx-auto mb-2" />
                <p className="text-sm font-semibold text-slate-700">No external integrations configured</p>
                <p className="text-xs text-slate-400 mt-1">
                  Connect your ERP or accounting simulation webhook to enable real-time export pipelines.
                </p>
              </div>
            )}
          </div>

          {/* Add Integration Modal */}
          {showAddModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
              <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6 space-y-4">
                <div className="flex items-center justify-between border-b pb-3">
                  <h3 className="font-bold text-slate-900 text-base">Connect ERP Integration</h3>
                  <button onClick={() => setShowAddModal(false)} className="text-slate-400 hover:text-slate-600">✕</button>
                </div>

                <div className="space-y-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Integration Name</label>
                    <input
                      type="text"
                      placeholder="e.g. NetSuite ERP Production"
                      value={newIntegration.name}
                      onChange={(e) => setNewIntegration({ ...newIntegration, name: e.target.value })}
                      className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Integration Adapter Type</label>
                    <select
                      value={newIntegration.integrationType}
                      onChange={(e) => setNewIntegration({ ...newIntegration, integrationType: e.target.value })}
                      className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-indigo-500 bg-white"
                    >
                      <option value="ERP_SIMULATOR">Built-in ERP Simulator (Development)</option>
                      <option value="ERP_NETSUITE">Oracle NetSuite REST API</option>
                      <option value="ERP_QUICKBOOKS">Intuit QuickBooks Online</option>
                      <option value="ERP_SAP">SAP S/4HANA OData</option>
                      <option value="WEBHOOK">Custom HTTPS Webhook Adapter</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">Endpoint URL</label>
                    <input
                      type="url"
                      placeholder="https://api.erp.domain.com/v1/invoices"
                      value={newIntegration.endpointUrl}
                      onChange={(e) => setNewIntegration({ ...newIntegration, endpointUrl: e.target.value })}
                      className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">API Key / Secret Token</label>
                    <input
                      type="password"
                      placeholder="••••••••••••••••"
                      value={newIntegration.apiKey}
                      onChange={(e) => setNewIntegration({ ...newIntegration, apiKey: e.target.value })}
                      className="w-full px-3 py-2 text-sm border rounded-lg focus:ring-2 focus:ring-indigo-500"
                    />
                  </div>
                </div>

                <div className="pt-3 border-t flex justify-end gap-2">
                  <button
                    onClick={() => setShowAddModal(false)}
                    className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => createIntegrationMutation.mutate(newIntegration)}
                    disabled={!newIntegration.name || createIntegrationMutation.isPending}
                    className="px-4 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm"
                  >
                    Save & Activate
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
