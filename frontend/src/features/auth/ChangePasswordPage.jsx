import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock, KeyRound } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import InputField from '../../components/common/InputField';
import ErrorMessage from '../../components/common/ErrorMessage';

// Chính sách khớp AuthService.validatePolicy ở backend.
function validatePolicy(p) {
  if (p.length < 10) return 'Mật khẩu tối thiểu 10 ký tự.';
  if (!/[A-Z]/.test(p)) return 'Mật khẩu cần ít nhất 1 chữ hoa.';
  if (!/[a-z]/.test(p)) return 'Mật khẩu cần ít nhất 1 chữ thường.';
  if (!/\d/.test(p)) return 'Mật khẩu cần ít nhất 1 chữ số.';
  if (!/[^A-Za-z0-9]/.test(p)) return 'Mật khẩu cần ít nhất 1 ký tự đặc biệt.';
  return '';
}

export function ChangePasswordPage() {
  const navigate = useNavigate();
  const { changePassword, mustChangePassword, logout } = useAuth();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    const policy = validatePolicy(next);
    if (policy) return setError(policy);
    if (next !== confirm) return setError('Mật khẩu xác nhận không khớp.');
    setLoading(true);
    try {
      await changePassword(current, next);
      setDone(true);
      setTimeout(() => navigate('/dashboard', { replace: true }), 800);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-md mx-auto bg-white rounded-2xl border border-slate-200/80 shadow-sm p-6 sm:p-8">
      <h1 className="text-xl font-extrabold text-slate-900 flex items-center">
        <KeyRound className="w-5 h-5 mr-2 text-blue-600" />
        Đổi mật khẩu
      </h1>
      {mustChangePassword && (
        <p className="mt-2 text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg p-3">
          Bạn cần đổi mật khẩu trước khi sử dụng hệ thống.
        </p>
      )}
      {error && <ErrorMessage message={error} className="mt-4" />}
      {done && <p className="mt-4 text-sm text-emerald-700">Đổi mật khẩu thành công.</p>}

      <form onSubmit={handleSubmit} className="space-y-4 mt-5">
        <InputField label="Mật khẩu hiện tại" type="password" icon={Lock} autoComplete="current-password"
          value={current} onChange={(e) => setCurrent(e.target.value)} required />
        <InputField label="Mật khẩu mới" type="password" icon={Lock} autoComplete="new-password"
          helperText="Tối thiểu 10 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt."
          value={next} onChange={(e) => setNext(e.target.value)} required />
        <InputField label="Nhập lại mật khẩu mới" type="password" icon={Lock} autoComplete="new-password"
          value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
        <div className="flex items-center justify-between pt-2">
          {mustChangePassword ? (
            <Button variant="ghost" type="button" onClick={logout}>Đăng xuất</Button>
          ) : (
            <Button variant="ghost" type="button" onClick={() => navigate(-1)}>Quay lại</Button>
          )}
          <Button type="submit" isLoading={loading}>Đổi mật khẩu</Button>
        </div>
      </form>
    </div>
  );
}

export default ChangePasswordPage;
