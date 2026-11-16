import React, { useState } from 'react';
import { useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Mail, Lock, LogIn } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import ErrorMessage from '../../components/common/ErrorMessage';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, isInitializing } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isInitializing && isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      const u = await login(email.trim(), password);
      const target = u.mustChangePassword ? '/change-password' : location.state?.from || '/dashboard';
      navigate(target, { replace: true });
    } catch (err) {
      setError(err.message || 'Đăng nhập không thành công');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <h3 className="text-xl font-bold text-slate-900 text-center mb-6">Đăng nhập vào hệ thống</h3>

      {error && <ErrorMessage title="Không đăng nhập được" message={error} className="mb-4" />}

      <form onSubmit={handleSubmit} className="space-y-4">
        <InputField
          label="Địa chỉ Email"
          type="email"
          icon={Mail}
          placeholder="ten@congty.vn"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <InputField
          label="Mật khẩu"
          type="password"
          icon={Lock}
          placeholder="••••••••"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        <Button type="submit" variant="primary" className="w-full mt-2" isLoading={isLoading} icon={LogIn}>
          Đăng nhập
        </Button>
      </form>
    </div>
  );
}

export default LoginPage;
