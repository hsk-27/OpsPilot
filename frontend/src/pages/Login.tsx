import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, ArrowRight, AlertCircle, Lock, Mail, Building2, KeyRound } from 'lucide-react';

export const Login: React.FC = () => {
  const { login, guestLogin } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('admin@opspilot.io');
  const [password, setPassword] = useState('Password123!');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [guestLoading, setGuestLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err: any) {
      let msg = err.response?.data?.message || err.response?.data?.detail;
      if (!msg) {
        if (err.message?.includes('Network Error') || err.response?.status === 404 || err.response?.status === 502) {
          msg = 'Unable to connect to OpsPilot backend at http://localhost:8080. Start the backend with start.bat or click Access Demo Environment.';
        } else {
          msg = 'Invalid credentials. Please verify your email and password or use the demo environment.';
        }
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleGuestLogin = async () => {
    setError(null);
    setGuestLoading(true);
    try {
      await guestLogin();
      navigate('/dashboard');
    } catch (err: any) {
      setError('Unable to initialize demo tenant session. Please try again.');
    } finally {
      setGuestLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0B0F17] text-slate-100 flex flex-col justify-between p-6 antialiased selection:bg-emerald-500 selection:text-white">
      {/* Top Corporate Nav */}
      <header className="max-w-6xl w-full mx-auto flex items-center justify-between py-2">
        <div className="flex items-center space-x-3">
          <img src="/opspilot-logo.svg" alt="OpsPilot" className="w-8 h-8 rounded-md" />
          <div className="flex items-center gap-2">
            <span className="font-bold text-lg tracking-tight text-white font-sans">OpsPilot</span>
            <span className="text-[10px] uppercase tracking-wider font-semibold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
              Enterprise AP
            </span>
          </div>
        </div>
        <div className="hidden sm:flex items-center space-x-6 text-xs text-slate-400">
          <span className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            Systems Operational
          </span>
          <span className="text-slate-600">•</span>
          <span>SOC 2 Type II Certified</span>
          <span className="text-slate-600">•</span>
          <span>TLS 1.3 / 256-bit Encrypted</span>
        </div>
      </header>

      {/* Main Login Card */}
      <main className="flex-1 flex items-center justify-center py-10">
        <div className="w-full max-w-[420px]">
          <div className="bg-white rounded-xl shadow-2xl p-8 border border-slate-200 text-slate-900">
            <div className="mb-6">
              <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded bg-slate-100 text-slate-700 text-[11px] font-semibold uppercase tracking-wider mb-3">
                <Building2 className="w-3.5 h-3.5 text-slate-600" />
                Global Logistics Enterprise
              </div>
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">Sign in to OpsPilot</h1>
              <p className="text-xs text-slate-500 mt-1">
                Accounts payable automation & deterministic governance portal
              </p>
            </div>

            {error && (
              <div className="mb-5 p-3.5 bg-rose-50 border border-rose-200 rounded-lg flex items-start gap-3 text-rose-800 text-xs leading-relaxed">
                <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5 text-rose-600" />
                <span>{error}</span>
              </div>
            )}

            {/* Instant Demo Access Button */}
            <div className="mb-6">
              <button
                type="button"
                onClick={handleGuestLogin}
                disabled={guestLoading}
                className="w-full py-3 px-4 bg-slate-900 hover:bg-slate-800 active:bg-slate-950 text-white rounded-lg text-sm font-semibold shadow-sm flex items-center justify-between transition-colors cursor-pointer group"
              >
                <div className="flex items-center gap-2.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
                  <span>{guestLoading ? 'Provisioning Tenant Session...' : 'Access Demo Environment'}</span>
                </div>
                <div className="flex items-center gap-1 text-xs text-slate-300 group-hover:text-white">
                  <span>1-Click Entry</span>
                  <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
                </div>
              </button>
              <p className="text-[11px] text-slate-400 text-center mt-1.5">
                Pre-configured with realistic suppliers, POs, and workflows
              </p>
            </div>

            <div className="relative flex items-center justify-center mb-6">
              <div className="border-t border-slate-200 w-full"></div>
              <span className="bg-white px-3 text-[11px] font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
                Or Sign In With Work Credentials
              </span>
              <div className="border-t border-slate-200 w-full"></div>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-[11px] font-bold text-slate-700 mb-1.5 uppercase tracking-wider">
                  Work Email
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="name@company.com"
                    className="w-full pl-9 pr-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-transparent focus:bg-white transition-all font-sans"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-[11px] font-bold text-slate-700 uppercase tracking-wider">
                    Password
                  </label>
                  <span className="text-[11px] text-slate-400">Default: Password123!</span>
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                  <input
                    type="password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••••••"
                    className="w-full pl-9 pr-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:border-transparent focus:bg-white transition-all font-sans"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full mt-2 py-2.5 px-4 bg-emerald-700 hover:bg-emerald-800 active:bg-emerald-900 disabled:opacity-50 text-white rounded-lg text-sm font-semibold shadow-sm flex items-center justify-center gap-2 transition-colors cursor-pointer"
              >
                {loading ? (
                  <span>Authenticating...</span>
                ) : (
                  <>
                    <KeyRound className="w-4 h-4" />
                    <span>Sign In</span>
                  </>
                )}
              </button>
            </form>

            <div className="mt-6 pt-5 border-t border-slate-100 text-center text-xs text-slate-500">
              Need a dedicated multi-tenant deployment?{' '}
              <Link to="/register" className="text-emerald-700 hover:text-emerald-900 font-semibold underline underline-offset-2">
                Register organization
              </Link>
            </div>
          </div>

          <div className="text-center mt-6 text-xs text-slate-500 flex items-center justify-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
            <span>Strict tenant isolation via cryptographic JWT session management</span>
          </div>
        </div>
      </main>

      {/* Corporate Footer */}
      <footer className="max-w-6xl w-full mx-auto py-3 border-t border-slate-800 text-slate-500 text-xs flex flex-col sm:flex-row items-center justify-between gap-2">
        <p>© 2026 OpsPilot Financial Technologies, Inc. All rights reserved.</p>
        <div className="flex items-center space-x-4 text-slate-400">
          <span className="hover:text-slate-200 cursor-pointer">Security Model</span>
          <span>•</span>
          <span className="hover:text-slate-200 cursor-pointer">API Reference</span>
          <span>•</span>
          <span className="hover:text-slate-200 cursor-pointer">Compliance Documentation</span>
        </div>
      </footer>
    </div>
  );
};
