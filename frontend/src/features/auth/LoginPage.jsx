import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { Mail, Lock, LogIn } from 'lucide-react';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import ErrorMessage from '../../components/common/ErrorMessage';

export function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email) {
      setError('Vui lòng nhập địa chỉ email');
      return;
    }

    try {
      setIsLoading(true);
      setError('');
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Đăng nhập không thành công');
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickLogin = (demoEmail) => {
    setEmail(demoEmail);
    setPassword('password123');
    login(demoEmail, 'password123');
    navigate('/dashboard');
  };

  return (
    <div>
      <h3 className="text-xl font-bold text-slate-900 text-center mb-6">
        Đăng nhập vào hệ thống
      </h3>

      {error && <ErrorMessage message={error} className="mb-4" />}

      <form onSubmit={handleSubmit} className="space-y-4">
        <InputField
          label="Địa chỉ Email"
          type="email"
          icon={Mail}
          placeholder="admin@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <InputField
          label="Mật khẩu"
          type="password"
          icon={Lock}
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        <div className="flex items-center justify-between text-xs">
          <label className="flex items-center text-slate-600">
            <input
              type="checkbox"
              defaultChecked
              className="rounded border-slate-300 text-blue-600 focus:ring-blue-500 mr-2"
            />
            Ghi nhớ đăng nhập
          </label>
          <a href="#" className="font-medium text-blue-600 hover:text-blue-500">
            Quên mật khẩu?
          </a>
        </div>

        <Button
          type="submit"
          variant="primary"
          className="w-full mt-2"
          isLoading={isLoading}
          icon={LogIn}
        >
          Đăng nhập
        </Button>
      </form>
    </div>
  );
}

export default LoginPage;
