import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AppLayout } from './components/layout/AppLayout';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Invoices } from './pages/Invoices';
import { InvoiceDetailPage } from './pages/InvoiceDetail';
import { Approvals } from './pages/Approvals';
import { Suppliers } from './pages/Suppliers';
import { PurchaseOrders } from './pages/PurchaseOrders';
import { WorkflowBuilder } from './pages/WorkflowBuilder';
import { Executions } from './pages/Executions';
import { Analytics } from './pages/Analytics';
import { Audit } from './pages/Audit';
import { Settings } from './pages/Settings';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5000,
      retry: 1,
    },
  },
});

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white text-sm">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 border-2 border-indigo-400 border-t-transparent rounded-full animate-spin"></div>
          <span>Authenticating OpsPilot Session...</span>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Auth Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Protected Enterprise Routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AppLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="invoices" element={<Invoices />} />
              <Route path="invoices/:id" element={<InvoiceDetailPage />} />
              <Route path="approvals" element={<Approvals />} />
              <Route path="purchase-orders" element={<PurchaseOrders />} />
              <Route path="suppliers" element={<Suppliers />} />
              <Route path="workflows" element={<WorkflowBuilder />} />
              <Route path="executions" element={<Executions />} />
              <Route path="analytics" element={<Analytics />} />
              <Route path="audit" element={<Audit />} />
              <Route path="settings" element={<Settings />} />
            </Route>

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
};

export default App;
