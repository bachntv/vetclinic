import { useState } from 'react';
import type { FormEvent } from 'react';
import { api } from './api';
import type { Visit, Medicine, Prescription, PrescriptionItem, Invoice } from './types';
import { Badge, Field, LoadState, Notice, Table, dateTime, money, useAction, useData } from './ui';
type Line=Partial<PrescriptionItem>&{key:string};
export default function PrescriptionPanel({visit}:{visit:Visit}){
 const resource=useData<Prescription>(`/prescriptions/by-visit/${visit.id}`),medicines=useData<Medicine[]>('/medicines'),invoices=useData<Invoice[]>('/invoices'),action=useAction();
 const [editing,setEditing]=useState(false),[cancelling,setCancelling]=useState(false),[items,setItems]=useState<Line[]>([{key:crypto.randomUUID()}]);
 const p=resource.data,missing=resource.status===404;
 const blocked=(invoices.data||[]).some(i=>i.visitId===visit.id&&i.status!=='DRAFT');
 const ready=!invoices.loading&&!invoices.error;
 const canEdit=visit.status==='IN_EXAM'&&!blocked&&ready;
 function edit(){setItems(p?p.items.map(i=>({...i,key:crypto.randomUUID()})):[{key:crypto.randomUUID()}]);setEditing(true);setCancelling(false);}
 function refresh(){setEditing(false);setCancelling(false);resource.reload();medicines.reload();invoices.reload();}
 async function save(e:FormEvent<HTMLFormElement>){e.preventDefault();const f=new FormData(e.currentTarget);
  const body={visitId:visit.id,revision:p?.revision,reason:f.get('reason'),notes:f.get('notes')||null,items:items.map((_,i)=>({medicineId:Number(f.get(`medicine${i}`)),dosage:f.get(`dosage${i}`),frequency:f.get(`frequency${i}`),durationDays:Number(f.get(`duration${i}`)),quantity:Number(f.get(`quantity${i}`)),instructions:f.get(`instructions${i}`)||null}))};
  await action.run(async()=>{await api(p?`/prescriptions/by-visit/${visit.id}`:'/prescriptions',{method:p?'PUT':'POST',body});refresh();},p?'Đã cập nhật đơn và điều chỉnh tồn kho.':'Đã kê đơn và trừ tồn kho.');
 }
 return <section className="panel"><div className="section-heading"><h2>{p?`Đơn thuốc #${p.id}`:'Đơn thuốc'}</h2><button type="button" disabled={action.busy} onClick={refresh}>Làm mới đơn</button></div><Notice action={action}/>
 {resource.loading?<p role="status">Đang tải đơn thuốc…</p>:resource.error&&!missing?<LoadState resource={resource}/>:<>
 {p&&<><p><Badge value={p.status==='ACTIVE'?'Hoạt động':'CANCELLED'}/> · Phiên bản {p.revision} · {dateTime(p.prescribedAt)}</p><p className="pre-wrap">{p.notes}</p>{p.status==='CANCELLED'&&<p className="warning">Đã hoàn kho lúc {dateTime(p.cancelledAt)}. Lý do: {p.cancelReason}</p>}
 <Table rows={p.items} columns={[{label:'Thuốc',key:'medicineName'},{label:'Liều',key:'dosage'},{label:'Tần suất',key:'frequency'},{label:'Số ngày',key:'durationDays'},{label:'Số lượng',key:'quantity'},{label:'Đơn giá',render:r=>money(r.unitPrice)},{label:'Dặn dò',key:'instructions'}]}/></>}
 <LoadState resource={invoices}/>{blocked&&<p className="muted">Hóa đơn đã chốt hoặc thanh toán. Đơn thuốc được khóa để bảo toàn tiền và tồn kho.</p>}
 {!editing&&!cancelling&&<div className="actions">{canEdit&&<button className="primary" onClick={edit}>{!p?'Kê đơn thuốc':p.status==='CANCELLED'?'Kê lại đơn':'Sửa đơn thuốc'}</button>}{p?.status==='ACTIVE'&&!blocked&&ready&&['IN_EXAM','COMPLETED'].includes(visit.status)&&<button className="danger" onClick={()=>setCancelling(true)}>Hủy đơn và hoàn kho</button>}</div>}
 {!p&&!canEdit&&<p className="muted">Chưa kê đơn. Có thể kê đơn khi lượt khám đang khám và chưa chốt hóa đơn.</p>}
 {editing&&<LoadState resource={medicines}><form onSubmit={save}><fieldset disabled={action.busy}><legend>{p?'Điều chỉnh đơn thuốc':'Kê đơn mới'}</legend>{items.map((item,i)=><div className="line-item" key={item.key}><Field name={`medicine${i}`} label={`Thuốc ${i+1}`} required value={item.medicineId} options={(medicines.data||[]).filter(m=>m.active||p?.items.some(old=>old.medicineId===m.id)).map(m=>({value:m.id,label:`${m.name} · tồn ${m.stockQuantity} ${m.unit}${m.active?'':' · ngừng sử dụng'}`}))}/><Field name={`dosage${i}`} label={`Liều dùng ${i+1}`} required maxLength={200} value={item.dosage}/><Field name={`frequency${i}`} label={`Tần suất ${i+1}`} required maxLength={120} value={item.frequency}/><Field name={`duration${i}`} label={`Số ngày ${i+1}`} required type="number" min={1} step={1} value={item.durationDays}/><Field name={`quantity${i}`} label={`Số lượng ${i+1}`} required type="number" min={0.01} step={0.01} max={9999999999.99} value={item.quantity}/><Field name={`instructions${i}`} label={`Dặn dò ${i+1}`} maxLength={500} value={item.instructions}/><button type="button" disabled={items.length===1} onClick={()=>setItems(items.filter(l=>l.key!==item.key))}>Bỏ dòng {i+1}</button></div>)}<button type="button" onClick={()=>setItems([...items,{key:crypto.randomUUID()}])}>+ Thêm thuốc</button><Field name="notes" label="Ghi chú đơn thuốc" type="textarea" maxLength={2000} value={p?.notes}/>{p&&<Field name="reason" label="Lý do sửa hoặc kê lại" required maxLength={1000}/>}</fieldset><p className="muted">Khi lưu, hệ thống điều chỉnh tồn kho trong một giao dịch; đơn cũ được lưu trong nhật ký.</p><div className="actions"><button className="primary" disabled={action.busy||!medicines.data?.length}>Lưu đơn thuốc</button><button type="button" disabled={action.busy} onClick={()=>setEditing(false)}>Đóng chỉnh sửa</button></div></form></LoadState>}
 {cancelling&&p&&<form onSubmit={async e=>{e.preventDefault();const reason=new FormData(e.currentTarget).get('reason');await action.run(async()=>{await api(`/prescriptions/by-visit/${visit.id}/cancel`,{method:'POST',body:{revision:p.revision,reason}});refresh();},'Đã hủy đơn và hoàn kho.');}}><Field name="reason" label="Lý do hủy đơn" required maxLength={1000}/><p className="warning">Chỉ xác nhận nếu thuốc chưa giao hoặc đã thực sự nhận lại. Hệ thống sẽ hoàn lượng thuốc trong đơn vào kho.</p><div className="actions"><button className="danger" disabled={action.busy}>Xác nhận hủy và hoàn kho</button><button type="button" disabled={action.busy} onClick={()=>setCancelling(false)}>Giữ đơn thuốc</button></div></form>}
 </>}
 </section>;
}
