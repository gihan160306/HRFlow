// Vai trò nền tảng: trùng đúng giá trị backend trả về (user_account.role).
export const ROLES = {
  ADMIN: 'ADMIN',
  HR: 'HR',
  MANAGER: 'MANAGER',
  EMPLOYEE: 'EMPLOYEE',
};

export const ROLE_LABELS = {
  ADMIN: 'Quản trị hệ thống',
  HR: 'Nhân sự (HR)',
  MANAGER: 'Quản lý',
  EMPLOYEE: 'Nhân viên',
};

// Quyền payroll cấp riêng theo tài khoản (payroll_grant), không suy ra từ role.
export const PAYROLL_GRANTS = {
  VIEW: 'VIEW',
  PREPARE: 'PREPARE',
  APPROVE: 'APPROVE',
  LOCK: 'LOCK',
};

export const EMPLOYEE_TYPES = [
  { value: 'FULL_TIME', label: 'Toàn thời gian' },
  { value: 'PART_TIME', label: 'Bán thời gian' },
  { value: 'PROBATION', label: 'Thử việc' },
  { value: 'INTERN', label: 'Thực tập sinh' },
];

export const WAGE_REGIONS = [
  { value: 'REGION_I', label: 'Vùng I' },
  { value: 'REGION_II', label: 'Vùng II' },
  { value: 'REGION_III', label: 'Vùng III' },
  { value: 'REGION_IV', label: 'Vùng IV' },
];

export const GENDERS = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
];

export const CONTRACT_TYPES = [
  { value: 'INDEFINITE', label: 'Không xác định thời hạn' },
  { value: 'FIXED_TERM', label: 'Xác định thời hạn' },
  { value: 'PROBATION', label: 'Thử việc' },
  { value: 'INTERNSHIP', label: 'Thực tập' },
  { value: 'PART_TIME', label: 'Bán thời gian' },
];

export const LEAVE_UNITS = [
  { value: 'FULL_DAY', label: 'Cả ngày' },
  { value: 'HALF_DAY_AM', label: 'Nửa ngày sáng' },
  { value: 'HALF_DAY_PM', label: 'Nửa ngày chiều' },
  { value: 'HOURS', label: 'Theo giờ' },
];

export const STORAGE_KEYS = {
  AUTH_TOKEN: 'hrflow_auth_token',
  CURRENT_USER: 'hrflow_current_user',
};
