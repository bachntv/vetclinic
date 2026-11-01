import { Link } from 'react-router-dom';
import { useRequiredAuth } from './auth';
import { AdminDashboard } from './Workspace';
import { roleProfiles } from './permissions';
import { Badge, Heading, LoadState, Table, dateTime, localDate, money, useData } from './ui';
import type { Invoice, Visit } from './types';

function Shortcuts() {
  const {user}=useRequiredAuth(), profile=roleProfiles[user.role];
  return <section className="workspace-welcome" aria-label="Công việc theo vai trò"><div><span className="eyebrow">{profile.label}</span><h2>{profile.title}</h2><p>{profile.description}</p></div><div className="workspace-shortcuts">{profile.shortcuts.map(item=><Link className="shortcut-card" key={item.to} to={item.to}><span>{item.label}</span><span aria-hidden="true">→</span></Link>)}</div></section>;
}

export default function Dashboard() {
  const {user}=useRequiredAuth();
  return <><Shortcuts/>{user.role==='ADMIN'?<AdminDashboard/>:user.role==='VET'?<DoctorDashboard/>:<CashierDashboard/>}</>;
}

function DoctorDashboard() {
  const {user}=useRequiredAuth(), visits=useData<Visit[]>('/visits');
  const all=(visits.data||[]).filter(v=>!user.vetId||v.vetId===user.vetId);
  const today=all.filter(v=>v.scheduledAt.startsWith(localDate())&&v.status!=='CANCELLED');
  const pending=today.filter(v=>['CHECKED_IN','IN_EXAM'].includes(v.status));
  const followUps=all.filter(v=>v.followUpDate&&v.followUpDate>=localDate());
  return <><Heading title="Công việc khám bệnh" subtitle={user.vetId?'Lịch khám và tái khám của bạn.':'Lịch toàn phòng khám; tài khoản chưa liên kết hồ sơ bác sĩ.'}><button onClick={visits.reload}>Làm mới</button></Heading><LoadState resource={visits}>
    <div className="stats">{[['Lịch hôm nay',today.length],['Đang chờ khám',today.filter(v=>v.status==='CHECKED_IN').length],['Đang khám',today.filter(v=>v.status==='IN_EXAM').length],['Đã hoàn thành',today.filter(v=>v.status==='COMPLETED').length]].map(([label,value])=><div className="stat" key={label}><small>{label}</small><strong>{value}</strong></div>)}</div>
    <section className="panel"><div className="section-heading"><h2>Bệnh nhân cần khám hôm nay</h2><Link to="/appointments">Xem lịch khám →</Link></div><Table rows={pending} empty="Chưa có bệnh nhân đang chờ khám hôm nay." columns={[{label:'Giờ',render:v=>dateTime(v.scheduledAt)},{label:'Bệnh nhân',render:v=><Link to={`/pets/${v.petId}`}>{v.petName}</Link>},{label:'Lý do khám',key:'chiefComplaint'},{label:'Trạng thái',render:v=><Badge value={v.status}/>},{label:'Khám bệnh',render:v=><Link className="button" to={`/visits/${v.id}`}>{v.status==='IN_EXAM'?'Tiếp tục khám':'Mở bệnh án'}</Link>}]}/></section>
    <section className="panel"><h2>Theo dõi tái khám</h2><Table rows={followUps} columns={[{label:'Bệnh nhân',render:v=><Link to={`/pets/${v.petId}`}>{v.petName}</Link>},{label:'Ngày tái khám',key:'followUpDate'},{label:'Bệnh án',render:v=><Link to={`/visits/${v.id}`}>Lượt khám #{v.id}</Link>}]}/></section>
  </LoadState></>;
}

function CashierDashboard() {
  const visits=useData<Visit[]>('/appointments'), invoices=useData<Invoice[]>('/invoices');
  const today=(visits.data||[]).filter(v=>v.scheduledAt.startsWith(localDate())&&v.status!=='CANCELLED');
  const unpaid=(invoices.data||[]).filter(i=>i.status==='SENT');
  const needsInvoice=(visits.data||[]).filter(v=>v.status==='COMPLETED'&&!(invoices.data||[]).some(i=>i.visitId===v.id));
  return <><Heading title="Tiếp nhận & thu tiền" subtitle="Theo dõi lịch đến khám, hóa đơn chờ thu và lượt khám cần lập hóa đơn."><button onClick={()=>{visits.reload();invoices.reload();}}>Làm mới</button></Heading><LoadState resource={visits}><LoadState resource={invoices}>
    <div className="stats">{[['Lịch hôm nay',today.length],['Chờ tiếp nhận',today.filter(v=>v.status==='SCHEDULED').length],['Hóa đơn chờ thu',unpaid.length],['Số tiền chờ thu',money(unpaid.reduce((sum,i)=>sum+i.total,0))]].map(([label,value])=><div className="stat" key={label}><small>{label}</small><strong>{value}</strong></div>)}</div>
    <section className="panel"><div className="section-heading"><h2>Lịch đến khám hôm nay</h2><Link to="/appointments">Đặt lịch / tiếp nhận →</Link></div><Table rows={today} columns={[{label:'Giờ hẹn',render:v=>dateTime(v.scheduledAt)},{label:'Thú cưng',key:'petName'},{label:'Bác sĩ',key:'vetName'},{label:'Trạng thái',render:v=><Badge value={v.status}/>},{label:'Tiếp nhận',render:v=><Link to={`/visits/${v.id}`}>Mở phiếu tiếp nhận</Link>}]}/></section>
    <section className="panel"><h2>Hóa đơn chờ thu tiền</h2><Table rows={unpaid} columns={[{label:'Hóa đơn',render:i=><Link to={`/invoices?visitId=${i.visitId}`}>#{i.id}</Link>},{label:'Thú cưng',key:'petName'},{label:'Số tiền',render:i=>money(i.total)},{label:'Thu tiền',render:i=><Link className="button" to={`/invoices?visitId=${i.visitId}`}>Mở thanh toán</Link>}]}/></section>
    <section className="panel"><h2>Lượt khám hoàn thành chưa lập hóa đơn</h2><Table rows={needsInvoice} columns={[{label:'Lượt khám',render:v=>`#${v.id} · ${v.petName}`},{label:'Bác sĩ',key:'vetName'},{label:'Hóa đơn',render:v=><Link to={`/invoices?visitId=${v.id}`}>Lập hóa đơn →</Link>}]}/></section>
  </LoadState></LoadState></>;
}
