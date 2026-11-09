import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';
import { User, Organization, AuthResponse, ApiResponse, Role } from '../types';

interface AuthContextType {
  user: User | null;
  organization: Organization | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  guestLogin: () => Promise<void>;
  register: (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    organizationName: string;
    organizationSlug?: string;
  }) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('opspilot_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [organization, setOrganization] = useState<Organization | null>(() => {
    const saved = localStorage.getItem('opspilot_org');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('opspilot_access_token');
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const verifyAuth = async () => {
      const storedToken = localStorage.getItem('opspilot_access_token');
      if (storedToken) {
        try {
          const res = await api.get<ApiResponse<User>>('/auth/me');
          if (res.data?.data) {
            setUser(res.data.data);
            localStorage.setItem('opspilot_user', JSON.stringify(res.data.data));
          }
        } catch {
          // Token expired or invalid
          localStorage.removeItem('opspilot_access_token');
          localStorage.removeItem('opspilot_user');
          localStorage.removeItem('opspilot_org');
          setUser(null);
          setOrganization(null);
          setToken(null);
        }
      }
      setIsLoading(false);
    };

    verifyAuth();
  }, []);

  const login = async (email: string, password: string) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
    const { accessToken, user: authUser, organization: authOrg } = res.data.data;
    localStorage.setItem('opspilot_access_token', accessToken);
    localStorage.setItem('opspilot_user', JSON.stringify(authUser));
    localStorage.setItem('opspilot_org', JSON.stringify(authOrg));
    setToken(accessToken);
    setUser(authUser);
    setOrganization(authOrg);
  };

  const guestLogin = async () => {
    try {
      const res = await api.post<ApiResponse<AuthResponse>>('/auth/guest');
      if (res.data?.data) {
        const { accessToken, user: authUser, organization: authOrg } = res.data.data;
        localStorage.setItem('opspilot_access_token', accessToken);
        localStorage.setItem('opspilot_user', JSON.stringify(authUser));
        localStorage.setItem('opspilot_org', JSON.stringify(authOrg));
        setToken(accessToken);
        setUser(authUser);
        setOrganization(authOrg);
        return;
      }
    } catch {
      // Fallback guest session for offline / standalone preview testing
      const mockUser: User = {
        id: '00000000-0000-0000-0000-000000000001',
        email: 'guest@opspilot.io',
        firstName: 'Guest',
        lastName: 'Tester',
        role: 'OWNER',
        organizationId: '00000000-0000-0000-0000-000000000002',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };
      const mockOrg: Organization = {
        id: '00000000-0000-0000-0000-000000000002',
        name: 'Global Logistics Enterprise',
        slug: 'global-logistics',
        defaultCurrency: 'USD',
        timezone: 'UTC',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };
      const mockToken = 'mock_guest_jwt_token_for_testing';
      localStorage.setItem('opspilot_access_token', mockToken);
      localStorage.setItem('opspilot_user', JSON.stringify(mockUser));
      localStorage.setItem('opspilot_org', JSON.stringify(mockOrg));
      setToken(mockToken);
      setUser(mockUser);
      setOrganization(mockOrg);
    }
  };

  const register = async (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    organizationName: string;
    organizationSlug?: string;
  }) => {
    const res = await api.post<ApiResponse<AuthResponse>>('/auth/register', data);
    const { accessToken, user: authUser, organization: authOrg } = res.data.data;
    localStorage.setItem('opspilot_access_token', accessToken);
    localStorage.setItem('opspilot_user', JSON.stringify(authUser));
    localStorage.setItem('opspilot_org', JSON.stringify(authOrg));
    setToken(accessToken);
    setUser(authUser);
    setOrganization(authOrg);
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout', {});
    } catch {
      // Ignore network errors on logout
    } finally {
      localStorage.removeItem('opspilot_access_token');
      localStorage.removeItem('opspilot_user');
      localStorage.removeItem('opspilot_org');
      setToken(null);
      setUser(null);
      setOrganization(null);
    }
  };

  const hasRole = (roles: Role[]): boolean => {
    if (!user) return false;
    return roles.includes(user.role);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        organization,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        guestLogin,
        register,
        logout,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
