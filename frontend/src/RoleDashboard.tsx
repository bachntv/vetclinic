import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useRequiredAuth } from './auth';
import { roleProfiles } from './permissions';
import { Badge, Heading, LoadState, Table, localDate, money, useData } from './ui';
import type { Invoice, Pet, Summary, Visit } from './types';
import PatientIdentity from './PatientIdentity';
import NavIcon from './NavIcon';

function QuickActions() {
  const {user} = useRequiredAuth();
  return <section className="panel quick-actions">
    <h2>Thao tác nhanh</h2>
    <div className="quick-action-grid">
      {roleProfiles[user.role].shortcuts.map(item =>
        <Link key={item.to} to={item.to}><NavIcon page={item.page}/><span>{item.label}</span><span aria-hidden="true">↗</span></Link>
      )}
    </div>
  </section>;
}

function Metric({label, value}: {label: string; value: ReactNode}) {
  return <div className="stat"><small>{label}</small><strong>{value}</strong></div>;
}

function AlertLink({count, label, to, tone = 'amber'}: {
  count: number; label: string; to: string; tone?: string;
}) {
  return <Link className={'clinic-alert alert-' + tone} to={to}>
    <strong>{count}</strong><span>{label}</span><span aria-hidden="true">→</span>
  </Link>;
}

function VisitList({visits, pets, empty}: {visits: Visit[]; pets: Pet[]; empty: string}) {
  const {user} = useRequiredAuth();
  const sorted = [...visits].sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt));
  return <Table rows={sorted} empty={empty} columns={[
    {label: 'Bệnh nhân', render: visit => {
      const pet = pets.find(p => p.id === visit.petId);
      return <Link to={'/visits/' + visit.id}><PatientIdentity name={visit.petName} species={pet?.species} detail={pet?.ownerName}/></Link>;
    }},
    {label: 'Giờ hẹn', render: visit => <span className="appointment-time">{visit.scheduledAt.slice(11, 16)}</span>},
    {label: 'Bác sĩ', key: 'vetName'},
    {label: 'Trạng thái', render: visit => <Badge value={visit.status}/>},
    {label: 'Thao tác', render: visit =>
      <Link className="button" to={'/visits/' + visit.id}>
        {visit.status === 'IN_EXAM' && user.role !== 'RECEPTIONIST' ? 'Tiếp tục khám'
          : visit.status === 'CHECKED_IN' && user.role !== 'RECEPTIONIST' ? 'Mở để khám'
          : visit.status === 'SCHEDULED' && user.role !== 'VET' ? 'Tiếp nhận' : 'Xem lượt khám'}
      </Link>},
  ]}/>;
}

function FollowUps({visits, pets}: {visits: Visit[]; pets: Pet[]}) {
  const rows = visits.filter(v => v.status !== 'CANCELLED' && v.followUpDate && v.followUpDate >= localDate())
    .sort((a, b) => (a.followUpDate || '').localeCompare(b.followUpDate || ''));
  return <section className="panel"><h2>Lịch tái khám</h2>
    <Table rows={rows} empty="Chưa có lịch tái khám sắp tới." columns={[
      {label: 'Bệnh nhân', render: visit => <Link to={'/visits/' + visit.id}><PatientIdentity name={visit.petName} species={pets.find(p => p.id === visit.petId)?.species}/></Link>},
      {label: 'Ngày hẹn', key: 'followUpDate'},
      {label: 'Bác sĩ', key: 'vetName'},
    ]}/>
  </section>;
}

export default function Dashboard() {
  const {user} = useRequiredAuth();
  return user.role === 'ADMIN' ? <AdminDashboard/> : <StaffDashboard/>;
}

function AdminDashboard() {
  const summary = useData<Summary>('/dashboard/summary');
  const pets = useData<Pet[]>('/pets');
  const d = summary.data;
  return <>
    <Heading title="Tổng quan phòng khám" subtitle={new Date().toLocaleDateString('vi-VN', {weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'})}>
      <Link className="button primary" to="/appointments">Mở lịch khám</Link>
      <button onClick={() => {summary.reload(); pets.reload();}}>Làm mới</button>
    </Heading>
    <LoadState resource={summary}>{d && <>
      <div className="stats dashboard-stats">
        <Metric label="Lịch hôm nay" value={d.todaySchedule.length}/>
        <Metric label="Thú cưng đang quản lý" value={d.petCount}/>
        <Metric label="Doanh thu đã thu" value={money(d.paidRevenue)}/>
        <Metric label="Hóa đơn chưa thanh toán" value={d.unpaidCount}/>
      </div>
      <div className="dashboard-grid">
        <div className="dashboard-main">
          <section className="panel">
            <div className="section-heading"><h2>Lịch khám hôm nay</h2><Link to="/appointments">Xem tất cả →</Link></div>
            <LoadState resource={pets}><VisitList visits={d.todaySchedule} pets={pets.data || []} empty="Hôm nay chưa có lịch khám."/></LoadState>
          </section>
          <section className="panel"><h2>Tiêm chủng đến hạn</h2>
            <Table rows={d.vaccinationAlerts} empty="Chưa có lịch tiêm chủng đến hạn." columns={[
              {label: 'Bệnh nhân', render: row => <Link to={'/pets/' + row.petId}><PatientIdentity name={row.petName} species={pets.data?.find(p => p.id === row.petId)?.species}/></Link>},
              {label: 'Vaccine', key: 'vaccineType'},
              {label: 'Ngày hẹn', render: row => <span className={row.nextDueDate && row.nextDueDate < localDate() ? 'warning' : ''}>{row.nextDueDate}</span>},
            ]}/>
          </section>
          <FollowUps visits={d.followUps} pets={pets.data || []}/>
          <details className="panel dashboard-reports"><summary>Báo cáo hoạt động</summary>
            <div className="two-col">
              <section><h2>Doanh thu theo tháng thu tiền</h2>{d.revenueWithoutPaymentDate > 0 && <p className="muted">Đã thu nhưng chưa rõ ngày thu {money(d.revenueWithoutPaymentDate)}.</p>}<Bars values={d.revenueByMonth} format={money}/></section>
              <section><h2>Số lượt theo bác sĩ</h2><Bars values={d.vetWorkload}/></section>
            </div>
            <h2>Lượt khám theo tháng</h2><Bars values={d.appointmentTrend}/>
            <p className="muted">{d.ownerCount} chủ nuôi · {d.inactivePatientCount} thú cưng ít hoạt động trong 2 năm.</p>
          </details>
        </div>
        <div className="dashboard-side">
          <section className="panel"><h2>Việc cần làm</h2><div className="clinic-alerts">
            <AlertLink count={d.todaySchedule.filter(v => v.status === 'CHECKED_IN').length} label="Bắt đầu khám bệnh nhân đã đến" to={"/appointments?status=CHECKED_IN&day="+localDate()} tone="blue"/>
            <AlertLink count={d.unpaidCount} label="Kiểm tra hóa đơn chờ xử lý" to="/invoices?status=PENDING"/>
            <AlertLink count={d.vaccinationAlerts.length} label="Lịch tiêm chủng đến hạn" to="/vaccinations"/>
            <AlertLink count={d.lowStock.length} label="Thuốc cần bổ sung" to="/medicines" tone="rose"/>
          </div></section>
          <QuickActions/>
          <section className="panel"><div className="section-heading"><h2>Thuốc sắp hết</h2><Link to="/medicines">Xem kho →</Link></div>
            {d.lowStock.length ? <ul className="stock-list">{d.lowStock.map(m => <li key={m.id}><strong>{m.name}</strong><span>{m.stockQuantity} {m.unit}</span></li>)}</ul> : <p className="muted">Tồn kho đang trên ngưỡng cảnh báo.</p>}
          </section>
        </div>
      </div>
    </>}</LoadState>
  </>;
}

function StaffDashboard() {
  const {user} = useRequiredAuth();
  const doctor = user.role === 'VET';
  const visits = useData<Visit[]>(doctor ? '/visits' : '/appointments');
  const pets = useData<Pet[]>('/pets');
  const invoices = useData<Invoice[]>(doctor ? null : '/invoices');
  const all = (visits.data || []).filter(v => !doctor || !user.vetId || v.vetId === user.vetId);
  const today = all.filter(v => v.scheduledAt.startsWith(localDate()) && v.status !== 'CANCELLED');
  const pending = (invoices.data || []).filter(i => i.status !== 'PAID');
  const unpaid = pending.filter(i => i.status === 'SENT');
  const needsInvoice = all.filter(v => v.status === 'COMPLETED' && !(invoices.data || []).some(i => i.visitId === v.id));
  const paidToday = (invoices.data || []).filter(i => i.status === 'PAID' && i.paidAt?.startsWith(localDate()));
  return <>
    <Heading title={doctor ? 'Bàn làm việc bác sĩ' : 'Tiếp nhận & thu tiền'}
      subtitle={doctor ? user.vetId ? 'Lịch khám và bệnh nhân của bạn hôm nay.' : 'Lịch toàn phòng khám. Tài khoản chưa liên kết bác sĩ.' : 'Lịch đến khám và các hóa đơn cần xử lý.'}>
      <Link className="button primary" to="/appointments">{doctor ? 'Mở lịch khám' : 'Đặt lịch / tiếp nhận'}</Link>
      <button onClick={() => {visits.reload(); invoices.reload(); pets.reload();}}>Làm mới</button>
    </Heading>
    <LoadState resource={visits}><LoadState resource={invoices}>
      <div className="stats dashboard-stats">
        <Metric label="Lịch hôm nay" value={today.length}/>
        <Metric label={doctor ? 'Chờ khám' : 'Chờ tiếp nhận'} value={today.filter(v => v.status === (doctor ? 'CHECKED_IN' : 'SCHEDULED')).length}/>
        <Metric label={doctor ? 'Đang khám' : 'Hóa đơn chờ thu'} value={doctor ? today.filter(v => v.status === 'IN_EXAM').length : unpaid.length}/>
        <Metric label={doctor ? 'Đã khám xong' : 'Số tiền chờ thu'} value={doctor ? today.filter(v => v.status === 'COMPLETED').length : money(unpaid.reduce((sum, i) => sum + i.total, 0))}/>
      </div>
      <div className="dashboard-grid">
        <div className="dashboard-main">
          <section className="panel"><div className="section-heading"><h2>{doctor ? 'Bệnh nhân hôm nay' : 'Lịch đến khám hôm nay'}</h2><Link to="/appointments">Xem lịch →</Link></div>
            <LoadState resource={pets}><VisitList visits={today} pets={pets.data || []} empty="Hôm nay chưa có lịch khám."/></LoadState>
          </section>
          {doctor ? <FollowUps visits={all} pets={pets.data || []}/> : <>
            <section className="panel"><h2>Hóa đơn cần xử lý</h2>
              <Table rows={pending} empty="Đã xử lý xong các hóa đơn." columns={[
                {label: 'Bệnh nhân', render: i => <Link to={'/invoices?visitId=' + i.visitId + '&invoiceId=' + i.id}><PatientIdentity name={i.petName} species={pets.data?.find(p => p.id === i.petId)?.species} detail={'Hóa đơn #' + i.id}/></Link>},
                {label: 'Số tiền', render: i => money(i.total)},
                {label: 'Trạng thái', render: i => <Badge value={i.status}/>},
                {label: 'Thao tác', render: i => <Link className="button" to={'/invoices?visitId=' + i.visitId + '&invoiceId=' + i.id}>{i.status === 'DRAFT' ? 'Xem và chốt' : 'Thu tiền'}</Link>},
              ]}/>
            </section>
            <section className="panel"><h2>Đã khám xong, chờ lập hóa đơn</h2>
              <Table rows={needsInvoice} empty="Chưa có lượt khám chờ lập hóa đơn." columns={[
                {label: 'Bệnh nhân', render: v => <PatientIdentity name={v.petName} species={pets.data?.find(p => p.id === v.petId)?.species} detail={'Lượt khám #' + v.id}/>},
                {label: 'Bác sĩ', key: 'vetName'},
                {label: 'Thao tác', render: v => <Link className="button" to={'/invoices?visitId=' + v.id + '&create=1'}>Lập hóa đơn</Link>},
              ]}/>
            </section>
            <section className="panel"><h2>Đã thu hôm nay</h2>
              <Table rows={paidToday} empty="Chưa ghi nhận khoản thu hôm nay." columns={[
                {label: 'Hóa đơn', render: i => <Link to={'/invoices?visitId=' + i.visitId + '&invoiceId=' + i.id}>#{i.id} · {i.petName}</Link>},
                {label: 'Số tiền', render: i => money(i.total)},
                {label: 'Giờ thu', render: i => i.paidAt?.slice(11, 16)},
              ]}/>
            </section>
          </>}
        </div>
        <div className="dashboard-side">
          <section className="panel"><h2>Việc cần làm</h2><div className="clinic-alerts">
            <AlertLink count={today.filter(v => v.status === (doctor ? 'CHECKED_IN' : 'SCHEDULED')).length} label={doctor ? 'Bắt đầu khám bệnh nhân đã đến' : 'Tiếp nhận thú cưng đã đặt lịch'} to={'/appointments?status='+(doctor?'CHECKED_IN':'SCHEDULED')+'&day='+localDate()} tone="blue"/>
            {doctor ? <AlertLink count={today.filter(v => v.status === 'IN_EXAM').length} label="Tiếp tục các lượt đang khám" to={"/appointments?status=IN_EXAM&day="+localDate()}/> : <>
              <AlertLink count={needsInvoice.length} label="Lập hóa đơn cho lượt đã khám xong" to={needsInvoice.length===1?"/invoices?visitId="+needsInvoice[0].id+"&create=1":"/invoices?task=create"}/>
              <AlertLink count={unpaid.length} label="Thu tiền hóa đơn đã chốt" to={unpaid.length===1?"/invoices?visitId="+unpaid[0].visitId+"&invoiceId="+unpaid[0].id:"/invoices?status=SENT"} tone="rose"/>
            </>}
          </div></section>
          <QuickActions/>
        </div>
      </div>
    </LoadState></LoadState>
  </>;
}

function Bars({values, format = n => n}: {values: Record<string, number>; format?: (n: number) => ReactNode}) {
  const max = Math.max(1, ...Object.values(values));
  return Object.entries(values).length ? Object.entries(values).map(([label, value]) =>
    <div className="bar-row" key={label}><div><span>{label}</span><strong>{format(value)}</strong></div><meter aria-label={label} min={0} max={max} value={value}/></div>
  ) : <p className="empty">Chưa có dữ liệu.</p>;
}
