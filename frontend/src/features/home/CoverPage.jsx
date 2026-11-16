import React from 'react';
import { Link, Navigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import BrandBackground from '../../components/brand/BrandBackground';
import hqLogo from '../../assets/hq-logo.webp';

/** Trang bìa công ty: hiển thị trước khi vào đăng nhập. Đã đăng nhập thì vào thẳng hệ thống. */
export function CoverPage() {
  const { isAuthenticated, isInitializing } = useAuth();
  if (!isInitializing && isAuthenticated) return <Navigate to="/dashboard" replace />;

  return (
    <div className="relative min-h-screen flex flex-col">
      <BrandBackground />

      <main className="relative z-10 flex flex-1 flex-col items-center justify-center px-6 py-16 text-center">
        <div className="rounded-3xl bg-white p-4 shadow-[0_25px_70px_-15px_rgba(99,102,241,0.7)]">
          <img src={hqLogo} alt="HNQ Media" className="h-32 sm:h-44 w-auto select-none" draggable="false" />
        </div>

        <p className="mt-10 text-xs sm:text-sm font-semibold uppercase tracking-[0.35em] text-sky-200">Chào mừng đến với</p>
        <h1 className="mt-3 text-4xl sm:text-6xl font-black tracking-tight text-white">
          HNQ Media <span className="bg-gradient-to-r from-cyan-300 via-sky-300 to-violet-300 bg-clip-text text-transparent">HRFlow</span>
        </h1>
        <p className="mt-5 max-w-2xl text-base sm:text-lg text-sky-100/90">
          Nền tảng quản lý nhân sự và tính lương tự động dành cho HNQ Media
        </p>

        <Link
          to="/login"
          className="group mt-10 inline-flex items-center rounded-full bg-white px-8 py-3.5 text-base font-bold text-indigo-900 shadow-xl shadow-indigo-950/40 transition-transform hover:scale-105"
        >
          Đăng nhập hệ thống
          <ArrowRight className="ml-2 h-5 w-5 transition-transform group-hover:translate-x-1" />
        </Link>
      </main>

      <footer className="relative z-10 px-6 py-6 text-center text-xs text-sky-200/70">
        © {new Date().getFullYear()} HNQ Media. Mọi quyền được bảo lưu.
      </footer>
    </div>
  );
}

export default CoverPage;
