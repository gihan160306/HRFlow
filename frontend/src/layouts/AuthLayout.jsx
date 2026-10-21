import React from 'react';
import { Outlet } from 'react-router-dom';
import logoSvg from '../assets/logo.svg';

export function AuthLayout() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="flex justify-center mb-3">
          <img src={logoSvg} alt="HRFlow" className="w-12 h-12" />
        </div>
        <h2 className="text-2xl font-extrabold text-slate-900 tracking-tight">
          HRFlow Enterprise
        </h2>
        <p className="mt-1 text-sm text-slate-500">
          Hệ thống Quản lý Nhân sự & Tính lương Tự động
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 sm:px-10 shadow-xl shadow-slate-200/50 rounded-2xl border border-slate-100">
          <Outlet />
        </div>
      </div>
    </div>
  );
}

export default AuthLayout;
