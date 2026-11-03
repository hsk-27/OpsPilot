import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Sparkles, ShieldCheck, ArrowRight, AlertCircle, Lock, Mail, Users } from 'lucide-react';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('admin@opspilot.io');
  const [password, setPassword] = useState('Password123!');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid credentials or connection error');
    } finally {
      setLoading(false);
    }
  };

  const setDemoAccount = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('Password123!');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-indigo-950 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo and Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-500 to-cyan-400 shadow-lg shadow-indigo-500/30 mb-4">
            <Sparkles className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-tight">OpsPilot</h1>
          <p className="text-sm text-slate-400 mt-1">Autonomous Accounts Payable Operations Platform</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl p-8 border border-slate-100">
          <div className="mb-6">
            <h2 className="text-xl font-bold text-slate-900">Sign in to your organization</h2>
            <p className="text-xs text-slate-500 mt-0.5">Enter your operational credentials to access your tenant</p>
          </div>

          {error && (
            <div className="mb-5 p-3.5 bg-rose-50 border border-rose-200 rounded-xl flex items-start gap-3 text-rose-800 text-xs">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5 uppercase tracking-wider">
                Work Email
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@company.com"
                  className="w-full pl-10 pr-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5 uppercase tracking-wider">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-10 pr-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-2 py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-lg text-sm font-semibold shadow-md shadow-indigo-600/30 flex items-center justify-center gap-2 transition-all"
            >
              {loading ? (
                <span>Signing in...</span>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Logins */}
          <div className="mt-6 pt-5 border-t border-slate-100">
            <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2.5">
              <Users className="w-3.5 h-3.5" />
              <span>Quick Demo Accounts (Password: Password123!)</span>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <button
                type="button"
                onClick={() => setDemoAccount('admin@opspilot.io')}
                className="p-2 border border-slate-200 rounded-lg text-left hover:border-indigo-400 hover:bg-indigo-50/50 transition-all"
              >
                <div className="font-semibold text-slate-800">Sarah Connor</div>
                <div className="text-[10px] text-purple-600 font-medium">Role: OWNER / ADMIN</div>
              </button>
              <button
                type="button"
                onClick={() => setDemoAccount('approver@opspilot.io')}
                className="p-2 border border-slate-200 rounded-lg text-left hover:border-indigo-400 hover:bg-indigo-50/50 transition-all"
              >
                <div className="font-semibold text-slate-800">Michael Scott</div>
                <div className="text-[10px] text-amber-600 font-medium">Role: APPROVER</div>
              </button>
              <button
                type="button"
                onClick={() => setDemoAccount('operator@opspilot.io')}
                className="p-2 border border-slate-200 rounded-lg text-left hover:border-indigo-400 hover:bg-indigo-50/50 transition-all"
              >
                <div className="font-semibold text-slate-800">Pam Beesly</div>
                <div className="text-[10px] text-blue-600 font-medium">Role: OPERATOR</div>
              </button>
              <button
                type="button"
                onClick={() => setDemoAccount('viewer@opspilot.io')}
                className="p-2 border border-slate-200 rounded-lg text-left hover:border-indigo-400 hover:bg-indigo-50/50 transition-all"
              >
                <div className="font-semibold text-slate-800">Jim Halpert</div>
                <div className="text-[10px] text-slate-500 font-medium">Role: VIEWER</div>
              </button>
            </div>
          </div>

          <div className="mt-6 text-center text-xs text-slate-500">
            Need a new tenant?{' '}
            <Link to="/register" className="text-indigo-600 hover:text-indigo-800 font-semibold">
              Create an organization
            </Link>
          </div>
        </div>

        <div className="text-center mt-6 text-xs text-slate-400 flex items-center justify-center gap-1.5">
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>Multi-tenant isolated with deterministic verification</span>
        </div>
      </div>
    </div>
  );
};
