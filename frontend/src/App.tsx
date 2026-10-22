import Assistant from './Assistant';
import { useEffect, useState } from 'react';
import { Link, NavLink, Navigate, Outlet, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom';
import { api,asError } from './api';
import type {FormEvent} from 'react';
import type {PatientRow,Owner,Pet,Page,Invoice} from './types';
import { RequireRole, useAuth, useRequiredAuth } from './auth';
import Resources from './Resources';
import { Appointments, VisitDetail, PetClinical } from './Clinical';
import Invoices from './Invoices';
import { Dashboard, Search, Support, Knowledge, Notifications } from './Workspace';
import { useData, LoadState, ErrorNotice, Table, useAction, Notice } from './ui';

function Login() {
  const auth = useAuth(), navigate = useNavigate(), location = useLocation();
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  if (auth.loading) return <div className="center" role="status">Đang kiểm tra phiên…</div>;
  if (auth.user) return <Navigate to="/dashboard" replace/>;
  async function submit(e:FormEvent<HTMLFormElement>) {
    e.preventDefault(); setBusy(true); setError(''); const values = new FormData(e.currentTarget);
    try { await auth.login(String(values.get('email')||'').trim(), String(values.get('password')||'')); navigate(location.state?.from || '/dashboard', { replace: true }); }
    catch (error) { const e=asError(error); setError(e.status === 401 ? 'Email hoặc mật khẩu không đúng, hoặc tài khoản đã bị khóa.' : e.message); }
    finally { setBusy(false); }
  }
  return <main className="login"><div className="intro"><div className="brand">✚ VetClinic</div><h1>Chăm sóc tốt hơn.<br/>Quản lý dễ dàng hơn.</h1><p>Không gian làm việc dành cho phòng khám thú y.</p></div><form className="login-card" onSubmit={submit}><span className="eyebrow">CHÀO MỪNG TRỞ LẠI</span><h2>Đăng nhập</h2><p className="muted">Sử dụng tài khoản do quản trị viên cung cấp.</p><ErrorNotice error={error || auth.error}/><label>Email<input name="email" type="email" autoComplete="username" required maxLength={255}/></label><label>Mật khẩu<input name="password" type="password" autoComplete="current-password" required/></label><button className="primary" disabled={busy}>{busy ? 'Đang đăng nhập…' : 'Đăng nhập →'}</button></form></main>;
}
function Layout() {
  const { user, logout } = useRequiredAuth();
  return <div className="shell"><aside><Link className="brand" to="/owners">✚ VetClinic</Link><p className="eyebrow">QUẢN LÝ PHÒNG KHÁM</p><nav><NavLink to="/dashboard">Tổng quan</NavLink><NavLink to="/appointments">Lịch hẹn & khám</NavLink><NavLink to="/owners">Chủ nuôi</NavLink><NavLink to="/pets">Thú cưng</NavLink><NavLink to="/vets">Bác sĩ</NavLink><NavLink to="/vaccinations">Tiêm chủng</NavLink><NavLink to="/vaccine-types">Danh mục vaccine</NavLink><NavLink to="/medicines">Thuốc & tồn kho</NavLink><NavLink to="/invoices">Hóa đơn</NavLink><NavLink to="/search">Tìm kiếm</NavLink><NavLink to="/notifications">Thông báo</NavLink><NavLink to="/knowledge">Kho kiến thức</NavLink>{user.role!=='RECEPTIONIST'&&<NavLink to="/assistant">Trợ lý bệnh án</NavLink>}<NavLink to="/support">Hỗ trợ</NavLink>{user.role==='ADMIN'&&<NavLink to="/users">Tài khoản</NavLink>}</nav><div className="account"><strong>{user.fullName}</strong><small>{user.email}</small><span className="badge">{user.role === 'ADMIN' ? 'Quản trị viên' : user.role === 'VET' ? 'Bác sĩ thú y' : 'Lễ tân'}</span><button onClick={logout}>Đăng xuất</button></div></aside><main className="workspace"><Outlet/></main></div>;
}
type Kind='owners'|'pets';
type PatientField=[keyof PatientRow,string,string?,boolean?,number?];
const config:Record<Kind,{title:string;singular:string;fields:PatientField[]}> = {
  owners: { title: 'Chủ nuôi', singular: 'chủ nuôi', fields: [ ['firstName','Tên','text',true,100], ['lastName','Họ','text',true,100], ['phone','Điện thoại','tel',false,30], ['email','Email','email',false,255], ['address','Địa chỉ','text',false,500] ] },
  pets: { title: 'Thú cưng', singular: 'thú cưng', fields: [ ['name','Tên thú cưng','text',true,100], ['species','Loài','text',true,80], ['breed','Giống','text',false,100], ['speciesNote','Ghi chú loài','text',false,500], ['birthDate','Ngày sinh','date'], ['sex','Giới tính','text',false,30], ['weightKg','Cân nặng (kg)','number'], ['allergies','Dị ứng','textarea'], ['chronicConditions','Bệnh mãn tính','textarea'] ] }
};
function nameOf(kind:Kind, item:PatientRow) { return kind === 'owners' ? `${item.firstName} ${item.lastName}` : item.name; }
function ListPage({ kind }:{kind:Kind}) {
  const {user}=useRequiredAuth(),[archived,setArchived]=useState(false),[query,setQuery]=useState(''),[page,setPage]=useState(0),action=useAction();
  const c=config[kind],resource=useData<Page<PatientRow>>('/'+kind+'/page?includeArchived='+archived+'&page='+page+'&size=10&q='+encodeURIComponent(query));
  const rows=resource.data?.content||[];
  return <><header><div><span className="eyebrow">HỒ SƠ PHÒNG KHÁM</span><h1>{c.title}</h1><p className="muted">Tra cứu và quản lý thông tin {c.singular}.</p></div>{['ADMIN','VET','RECEPTIONIST'].includes(user.role)&&<Link className="button primary" to={'/'+kind+'/new'}>+ Thêm {c.singular}</Link>}</header><Notice action={action}/><label className="checkbox"><input type="checkbox" checked={archived} onChange={e=>{setArchived(e.target.checked);setPage(0);}}/>Bao gồm hồ sơ đã lưu trữ</label><div className="toolbar"><label className="search">Tìm kiếm<input value={query} onChange={e=>{setQuery(e.target.value);setPage(0);}} placeholder="Tên hoặc thông tin liên hệ…"/></label><button onClick={resource.reload}>Làm mới</button></div><LoadState resource={resource}><section className="panel"><Table rows={rows} columns={[{label:c.title,render:r=>r.archived?<strong>{nameOf(kind,r)} · Đã lưu trữ</strong>:<Link to={'/'+kind+'/'+r.id}>{nameOf(kind,r)}</Link>},{label:kind==='owners'?'Điện thoại':'Loài',key:kind==='owners'?'phone':'species'},{label:kind==='owners'?'Email':'Chủ nuôi',key:kind==='owners'?'email':'ownerName'},{label:'Thao tác',render:r=>['ADMIN','VET','RECEPTIONIST'].includes(user.role)&&(r.archived?<button disabled={action.busy} onClick={()=>action.run(async()=>{await api('/'+kind+'/'+r.id+'/activate',{method:'POST'});resource.reload();})}>Kích hoạt</button>:<Link to={'/'+kind+'/'+r.id+'/edit'}>Sửa</Link>)}]}/><div className="pagination"><span>{resource.data?.total||0} hồ sơ · Trang {page+1}/{Math.max(1,resource.data?.pages||0)}</span><button disabled={page===0} onClick={()=>setPage(page-1)}>Trang trước</button><button disabled={page+1>=(resource.data?.pages||0)} onClick={()=>setPage(page+1)}>Trang sau</button></div></section></LoadState></>;
}
function DetailPage({ kind }:{kind:Kind}) {
  const { user } = useRequiredAuth();
  const { id } = useParams(), navigate = useNavigate(), c = config[kind], resource = useData<PatientRow>(`/${kind}/${id}`);
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function archive() {
    if (!window.confirm(`Lưu trữ ${c.singular} này? Hồ sơ sẽ không còn trong danh sách hoạt động.`)) return;
    setBusy(true); setError('');
    try { await api(`/${kind}/${id}`, { method: 'DELETE' }); navigate(`/${kind}`, { replace: true }); }
    catch (e) { setError(asError(e).message); } finally { setBusy(false); }
  }
  return <><Link to={`/${kind}`}>← Danh sách {c.singular}</Link><LoadState resource={resource}>{resource.data && <><header><div><span className="eyebrow">HỒ SƠ #{id}</span><h1>{nameOf(kind,resource.data)}</h1></div>{['ADMIN','VET','RECEPTIONIST'].includes(user.role)&&<Link className="button" to={`/${kind}/${id}/edit`}>Chỉnh sửa</Link>}</header><div className="panel"><ErrorNotice error={error}/><dl className="details">{kind === 'pets' && <><dt>Chủ nuôi</dt><dd><Link to={`/owners/${resource.data.ownerId}`}>{resource.data.ownerName}</Link></dd></>}{c.fields.map(([key,label]) => <div className="detail-row" key={key}><dt>{label}</dt><dd>{resource.data?.[key] ?? '—'}</dd></div>)}</dl>{(user.role==='ADMIN'||user.role==='VET'||(kind==='pets'&&user.role==='RECEPTIONIST'))&&<button className="danger" disabled={busy} onClick={archive}>{busy ? 'Đang lưu trữ…' : 'Lưu trữ hồ sơ'}</button>}{kind === 'owners' && <p className="muted">Chỉ lưu trữ chủ nuôi khi không còn thú cưng hoạt động.</p>}</div>{kind === 'owners' && <><OwnerPets id={id!}/><OwnerInvoices id={id!}/></>} {kind === 'pets' && <PetClinical id={id!}/>}</>}</LoadState></>;
}
function OwnerPets({ id }:{id:string}) {
  const resource = useData<Pet[]>(`/owners/${id}/pets`);
  return <section className="panel"><h2>Thú cưng của chủ nuôi</h2><LoadState resource={resource}>{resource.data?.length ? <ul className="pet-list">{resource.data.map(p => <li key={p.id}><Link to={`/pets/${p.id}`}>{p.name}</Link><span>{p.species}</span></li>)}</ul> : <p className="muted">Chưa có thú cưng hoạt động.</p>}</LoadState></section>;
}
function EditPage({ kind }:{kind:Kind}) {
  const { user } = useAuth();
  const { id } = useParams(), navigate = useNavigate(), c = config[kind];
  const resource = useData<PatientRow>(id ? `/${kind}/${id}` : null);
  const owners = useData<Owner[]>('/owners');
  const [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function submit(e:FormEvent<HTMLFormElement>) {
    e.preventDefault(); const values = new FormData(e.currentTarget), body:Record<string,string|number|null> = {};
    for (const [key,, type, required] of c.fields) { const value = String(values.get(key) || '').trim(); if (required && !value) { setError('Các trường bắt buộc không được chỉ chứa khoảng trắng.'); return; } body[key] = value === '' ? null : type === 'number' ? Number(value) : value; }
    if (kind === 'pets') body.ownerId = Number(values.get('ownerId'));
    setBusy(true); setError('');
    try { const saved = await api<PatientRow>(`/${kind}${id ? `/${id}` : ''}`, { method: id ? 'PUT' : 'POST', body }); navigate(`/${kind}/${saved.id}`, { replace: true }); }
    catch (e) { setError(asError(e).message); } finally { setBusy(false); }
  }
  const initial:Partial<PatientRow> = id ? resource.data||{} : {};

  return <><Link to={`/${kind}`}>← Danh sách {c.singular}</Link><header><h1>{id ? 'Chỉnh sửa' : 'Thêm'} {c.singular}</h1></header><LoadState resource={resource}><form className="panel form" onSubmit={submit} key={`${kind}-${id}`}><ErrorNotice error={error}/><fieldset disabled={busy}><div className="form-grid">{kind === 'pets' && <label>Chủ nuôi *<select name="ownerId" required defaultValue={initial?.ownerId || ''} disabled={owners.loading || !!owners.error}><option value="">{owners.loading ? 'Đang tải chủ nuôi…' : 'Chọn chủ nuôi'}</option>{owners.data?.map(o => <option key={o.id} value={o.id}>{nameOf('owners',o)}</option>)}</select>{owners.error && <><ErrorNotice error={owners.error}/><button type="button" onClick={owners.reload}>Tải lại chủ nuôi</button></>}{!owners.loading && !owners.error && owners.data?.length === 0 && <span>Chưa có chủ nuôi. <Link to="/owners/new">Thêm chủ nuôi trước</Link>.</span>}</label>}{c.fields.map(([key,label,type,required,maxLength]) => <label key={key}>{label}{required ? ' *' : ''}{type === 'textarea' ? <textarea name={key} defaultValue={String(initial?.[key] || '')} rows={3}/> : <input name={key} type={type} required={required} maxLength={maxLength} defaultValue={String(initial?.[key] ?? '')} {...(type === 'number' ? { min: '0.01', max: '99999.99', step: '0.01' } : {})}/>}</label>)}</div></fieldset><div className="actions"><button className="primary" disabled={busy || (kind === 'pets' && (owners.loading || !!owners.error || !owners.data?.length))}>{busy ? 'Đang lưu…' : 'Lưu hồ sơ'}</button><Link className="button" to={id ? `/${kind}/${id}` : `/${kind}`}>Hủy</Link></div></form></LoadState></>;
}
export default function App() {
  return <Routes><Route path="/login" element={<Login/>}/><Route element={<RequireRole/>}><Route element={<Layout/>}><Route index element={<Navigate to="/dashboard" replace/>}/><Route path="dashboard" element={<Dashboard/>}/><Route path="appointments" element={<Appointments/>}/><Route path="visits/:id" element={<VisitDetail/>}/><Route path="vets" element={<Resources key="vets" kind="vets"/>}/><Route path="vaccine-types" element={<Resources key="vaccine-types" kind="vaccine-types"/>}/><Route path="medicines" element={<Resources key="medicines" kind="medicines"/>}/><Route path="vaccinations" element={<Resources key="vaccinations" kind="vaccinations"/>}/><Route path="invoices" element={<Invoices/>}/><Route path="search" element={<Search/>}/><Route path="support" element={<Support/>}/><Route path="notifications" element={<Notifications/>}/><Route path="knowledge" element={<Knowledge/>}/><Route element={<RequireRole roles={['ADMIN','VET']}/>}> <Route path="assistant" element={<Assistant/>}/></Route><Route element={<RequireRole roles={['ADMIN']}/>}> <Route path="users" element={<Resources key="users" kind="users"/>}/></Route>{(['owners','pets'] as Kind[]).map(kind => <Route key={kind} path={kind}><Route index element={<ListPage kind={kind}/>}/><Route path="new" element={<EditPage kind={kind}/>}/><Route path=":id" element={<DetailPage kind={kind}/>}/><Route path=":id/edit" element={<EditPage kind={kind}/>}/></Route>)}<Route path="*" element={<div className="panel"><h1>404 — Trang không tồn tại</h1><Link to="/owners">Về danh sách chủ nuôi</Link></div>}/></Route></Route></Routes>;
}

function OwnerInvoices({id}:{id:string}){const resource=useData<Invoice[]>('/owners/'+id+'/invoices');return <section className="panel"><h2>Hóa đơn của chủ nuôi</h2><LoadState resource={resource}><Table rows={resource.data||[]} columns={[{label:'Hóa đơn',render:r=><Link to={'/invoices?visitId='+r.visitId}>#{r.id}</Link>},{label:'Thú cưng',key:'petName'},{label:'Tổng tiền',render:r=>new Intl.NumberFormat('vi-VN').format(r.total)+' đ'},{label:'Trạng thái',key:'status'}]}/></LoadState></section>;}

