import type { Role, VisitStatus } from './types';

export type PageKey = 'services' | 'dashboard' | 'appointments' | 'owners' | 'pets' | 'vets' | 'vaccinations' | 'vaccine-types' | 'medicines' | 'invoices' | 'search' | 'notifications' | 'knowledge' | 'assistant' | 'support' | 'users' | 'audit';
export type Permission = 'patients.write' | 'owners.archive' | 'pets.archive' | 'appointments.manage' | 'clinical.write' | 'catalog.manage' | 'invoices.manage' | 'support.manage';
const all: Role[] = ['ADMIN', 'VET', 'RECEPTIONIST'];
const clinical: Role[] = ['ADMIN', 'VET'];
const reception: Role[] = ['ADMIN', 'RECEPTIONIST'];

export const pageRoles: Record<PageKey, readonly Role[]> = {
  services: all, dashboard: all, appointments: all, owners: all, pets: all,
  vets: ['ADMIN'], vaccinations: clinical, 'vaccine-types': clinical, medicines: clinical,
  invoices: reception, search: all, notifications: all, knowledge: clinical,
  assistant: clinical, support: all, users: ['ADMIN'], audit: ['ADMIN'],
};
const permissionRoles: Record<Permission, readonly Role[]> = {
  'patients.write': reception, 'owners.archive': ['ADMIN'], 'pets.archive': reception,
  'appointments.manage': reception, 'clinical.write': clinical, 'catalog.manage': ['ADMIN'],
  'invoices.manage': reception, 'support.manage': ['ADMIN'],
};
export const canAccessPage = (role: Role, page: PageKey) => pageRoles[page].includes(role);
export const can = (role: Role, permission: Permission) => permissionRoles[permission].includes(role);

export function visitTransitions(role: Role, status: VisitStatus): VisitStatus[] {
  const transitions: Partial<Record<VisitStatus, VisitStatus[]>> = {
    SCHEDULED: ['CHECKED_IN', 'CANCELLED'], CHECKED_IN: ['IN_EXAM', 'CANCELLED'], IN_EXAM: ['COMPLETED'],
  };
  return (transitions[status] || []).filter(next =>
    role === 'ADMIN' || (role === 'VET' ? ['IN_EXAM', 'COMPLETED'].includes(next) : ['CHECKED_IN', 'CANCELLED'].includes(next)));
}

export interface NavItem { page: PageKey; label: string; to: string }
export interface NavSection { label: string; items: NavItem[] }
const item = (page: PageKey, label: string): NavItem => ({ page, label, to: '/' + page });
export const roleProfiles: Record<Role, {label: string; title: string; description: string; sections: NavSection[]; shortcuts: NavItem[]}> = {
  ADMIN: {
    label: 'Quản trị viên', title: 'Không gian quản trị',
    description: 'Điều hành phòng khám, quản lý nhân sự, danh mục, tài chính và kiểm tra hoạt động.',
    sections: [
      {label: 'Điều hành', items: [item('dashboard', 'Tổng quan phòng khám'), item('appointments', 'Lịch hẹn & khám'), item('owners', 'Quản lý chủ nuôi'), item('pets', 'Quản lý thú cưng')]},
      {label: 'Chuyên môn & danh mục', items: [item('vets', 'Quản lý bác sĩ'), item('vaccinations', 'Tiêm chủng'), item('vaccine-types', 'Danh mục vaccine'), item('medicines', 'Thuốc & tồn kho'), item('knowledge', 'Quản lý kiến thức'), item('assistant', 'Trợ lý bệnh án')]},
      {label: 'Tài chính & hệ thống', items: [item('services', 'Bảng giá dịch vụ'), item('invoices', 'Hóa đơn & thanh toán'), item('users', 'Tài khoản & vai trò'), item('audit', 'Nhật ký thao tác')]},
      {label: 'Tiện ích', items: [item('search', 'Tìm kiếm'), item('notifications', 'Thông báo'), item('support', 'Quản lý hỗ trợ')]},
    ],
    shortcuts: [item('users', 'Quản lý tài khoản'), item('vets', 'Quản lý bác sĩ'), item('medicines', 'Quản lý kho thuốc'), item('audit', 'Kiểm tra nhật ký')],
  },
  VET: {
    label: 'Bác sĩ thú y', title: 'Không gian bác sĩ',
    description: 'Theo dõi lịch khám, ghi bệnh án, kê đơn, tiêm chủng và chăm sóc bệnh nhân.',
    sections: [
      {label: 'Khám & điều trị', items: [item('dashboard', 'Bàn làm việc bác sĩ'), item('appointments', 'Lịch khám'), item('pets', 'Hồ sơ bệnh nhân'), item('owners', 'Tra cứu chủ nuôi'), item('vaccinations', 'Tiêm chủng')]},
      {label: 'Tra cứu chuyên môn', items: [item('medicines', 'Tra cứu thuốc'), item('vaccine-types', 'Tra cứu vaccine'), item('knowledge', 'Kho kiến thức'), item('assistant', 'Trợ lý bệnh án')]},
      {label: 'Tiện ích', items: [item('search', 'Tìm bệnh nhân'), item('notifications', 'Nhắc việc chuyên môn'), item('support', 'Hỗ trợ của tôi')]},
    ],
    shortcuts: [item('appointments', 'Mở lịch khám'), item('pets', 'Tra cứu bệnh nhân'), item('vaccinations', 'Ghi nhận tiêm chủng'), item('assistant', 'Mở trợ lý bệnh án')],
  },
  RECEPTIONIST: {
    label: 'Thu ngân / tiếp nhận', title: 'Không gian thu ngân',
    description: 'Tiếp đón chủ nuôi, quản lý lịch hẹn, tiếp nhận thú cưng và ghi nhận thanh toán.',
    sections: [
      {label: 'Tiếp nhận & thanh toán', items: [item('dashboard', 'Bàn làm việc thu ngân'), item('appointments', 'Lịch hẹn & tiếp nhận'), item('owners', 'Chủ nuôi'), item('pets', 'Thú cưng'), item('invoices', 'Hóa đơn & thu tiền'), item('services', 'Bảng giá dịch vụ')]},
      {label: 'Tiện ích', items: [item('search', 'Tìm hồ sơ'), item('notifications', 'Nhắc lịch hẹn'), item('support', 'Hỗ trợ của tôi')]},
    ],
    shortcuts: [item('appointments', 'Đặt lịch / tiếp nhận'), item('invoices', 'Lập hóa đơn / thu tiền'), {...item('owners', 'Thêm chủ nuôi'), to: '/owners/new'}, {...item('pets', 'Thêm thú cưng'), to: '/pets/new'}],
  },
};

// A return URL must pass the same rules as the actual route, including edit screens.
export function canAccessPath(role: Role, path: string): boolean {
  if (!path.startsWith('/') || path.startsWith('//')) return false;
  const pathname = path.split(/[?#]/)[0].replace(/\/$/, '') || '/';
  if (pathname === '/') return true;
  const parts = pathname.slice(1).split('/');
  if (parts[0] === 'visits') return parts.length === 2 && /^\d+$/.test(parts[1]);
  if (!Object.hasOwn(pageRoles, parts[0])) return false;
  const page = parts[0] as PageKey;
  if (!canAccessPage(role, page)) return false;
  if (page !== 'owners' && page !== 'pets') return parts.length === 1;
  if (parts.length === 1) return true;
  if (parts.length === 2 && parts[1] === 'new') return can(role, 'patients.write');
  if (!/^\d+$/.test(parts[1])) return false;
  return parts.length === 2 || (parts.length === 3 && parts[2] === 'edit' && can(role, 'patients.write'));
}
