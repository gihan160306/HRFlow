import React from 'react';
import { Link, Outlet } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import BrandBackground from '../components/brand/BrandBackground';
import hqLogo from '../assets/hq-logo.webp';

export function AuthLayout() {
  return (
    <div className="relative min-h-screen flex flex-col items-center justify-center px-4 py-10">
      <BrandBackground />

      <div className="relative z-10 w-full max-w-5xl">
        <Link to="/" className="mb-4 inline-flex items-center text-sm font-semibold text-sky-100/90 hover:text-white">
          <ArrowLeft className="mr-1.5 h-4 w-4" /> Trang bìa
        </Link>

        <div className="grid overflow-hidden rounded-[28px] bg-white shadow-[0_40px_100px_-20px_rgba(15,10,60,0.85)] lg:grid-cols-2 lg:min-h-[560px]">
          {/* Cột thương hiệu */}
          <div className="relative hidden lg:flex flex-col items-center justify-center bg-gradient-to-br from-[#0b1a5e] via-[#07123d] to-[#02040d] px-12 py-14 text-center">
            <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(circle, #fff 1px, transparent 1px)', backgroundSize: '26px 26px' }} />
            <div className="relative rounded-3xl bg-white p-3 shadow-2xl shadow-indigo-950/50">
              <img src={hqLogo} alt="HNQ Media" className="h-44 w-auto select-none" draggable="false" />
            </div>
            <h2 className="relative mt-10 text-3xl font-black tracking-tight text-white">HNQ Media</h2>
            <p className="relative mt-2 text-xs font-semibold uppercase tracking-[0.28em] text-sky-200">Creative • Content • Culture</p>
          </div>

          {/* Cột đăng nhập */}
          <div className="flex flex-col justify-center px-7 sm:px-14 py-12 sm:py-14">
            <div className="text-center">
              <img src={hqLogo} alt="HNQ Media" className="mx-auto mb-4 h-20 w-auto select-none lg:hidden" draggable="false" />
              <h2 className="text-3xl font-extrabold tracking-tight text-slate-900">
                HRFlow <span className="bg-gradient-to-r from-blue-600 to-violet-600 bg-clip-text text-transparent">Enterprise</span>
              </h2>
              <p className="mt-2 text-sm text-slate-500">Hệ thống Quản lý Nhân sự & Tính lương Tự động</p>
            </div>

            <div className="my-8 h-px bg-gradient-to-r from-transparent via-slate-200 to-transparent" />

            <Outlet />
          </div>
        </div>

        <p className="mt-6 text-center text-xs text-sky-200/80">© {new Date().getFullYear()} HNQ Media</p>
      </div>
    </div>
  );
}

export default AuthLayout;
