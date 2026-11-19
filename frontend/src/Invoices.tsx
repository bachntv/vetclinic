import { useEffect, useState } from 'react';
import { can } from './permissions';
import { Link,useSearchParams } from 'react-router-dom';
import { api } from './api';
import type {FormEvent} from 'react';
import type {Visit,Invoice,InvoiceCategory,ClinicService} from './types';
interface ItemDraft {key:string;id?:number;description:string;category:InvoiceCategory;quantity:number|string;unitPrice:number|string;lineTotal?:number;wholeQuantity?:boolean}
import { useRequiredAuth as useAuth } from './auth';
import { Badge,Field,Heading,LoadState,Notice,Table,dateTime,labels,money,useAction,useData } from './ui';
export default function Invoices(){
 const resource=useData<Invoice[]>('/invoices'),visits=useData<Visit[]>('/visits'),{user}=useAuth(),action=useAction(),[params,setParams]=useSearchParams();
 const[editing,setEditing]=useState<Partial<Invoice>|null>(()=>params.get('create')==='1'&&can(user.role,'invoices.manage')?{}:null),[selected,setSelected]=useState<number|null>(null),[editedItems,setItems]=useState<ItemDraft[]|null>(null);
 const [draftVisitId,setDraftVisitId]=useState(params.get('visitId')??'');
 const [confirmation,setConfirmation]=useState<Invoice|null>(null);
 const services=useData<ClinicService[]>('/services');
 const [serviceId,setServiceId]=useState('');
 const [vatRate,setVatRate]=useState(0);
 const [checkout,setCheckout]=useState<{intent:string;total:number;save:()=>Promise<void>}|null>(null);
 const canWrite=can(user.role,'invoices.manage'),visitId=params.get('visitId');
 const filter=params.get('status')||'';
 const setFilter=(value:string)=>{const next=new URLSearchParams(params);if(value)next.set('status',value);else next.delete('status');setParams(next);};
 const create=params.get('create');
 useEffect(()=>{
  if(create==='1'&&canWrite){setEditing({});setDraftVisitId(visitId||'');setItems(null);setVatRate(0);}
 },[create,visitId,canWrite]);
 const needsInvoice=(visits.data||[]).filter(v=>v.status==='COMPLETED'&&!(resource.data||[]).some(i=>i.visitId===v.id));
 const rows=(resource.data||[]).filter(i=>(!filter||(filter==='PENDING'?i.status!=='PAID':i.status===filter))&&(!visitId||String(i.visitId)===visitId));
 const detail=resource.data?.find(i=>i.id===(selected??Number(params.get('invoiceId'))));
 const selectedVisit=visits.data?.find(v=>String(v.id)===draftVisitId);
 const charges=useData<Pick<ItemDraft,'description'|'category'|'quantity'|'unitPrice'|'wholeQuantity'>[]>(
  editing&&!editing.id&&selectedVisit?.status==='COMPLETED'?`/invoices/visits/${draftVisitId}/prescribed-charges`:null
 );
 const items:ItemDraft[]=editedItems??(charges.data?.length
  ? charges.data.map((item,index)=>({...item,key:`prescribed-${index}`}))
  : [{key:'consultation',category:'CONSULTATION' as const,quantity:1,unitPrice:0,description:''}]);
 function closeEditor(){
  setEditing(null);
  setCheckout(null);
  if(params.has('create')){
   const next=new URLSearchParams(params);
   next.delete('create');
   setParams(next,{replace:true});
  }
 }
 function edit(item:Invoice|null){
  setEditing(item||{});
  setCheckout(null);
  setVatRate(Number(item?.vatRate||0));
  setDraftVisitId(visitId??'');
  setItems(item?.items?.map(i=>({...i,key:crypto.randomUUID()}))??null);
 }
 const subtotal=items.reduce((sum,item)=>sum+Math.round(Number(item.quantity)*Number(item.unitPrice)*100),0)/100;
 const tax=Math.round(subtotal*vatRate*100)/100;
 const total=subtotal+tax;
 const cannotSave=!!checkout||action.busy||resource.loading||!!resource.error||(!editing?.id&&(charges.loading||!!charges.error||selectedVisit?.status!=='COMPLETED'||!!resource.data?.some(invoice=>invoice.visitId===selectedVisit.id)));
 async function submit(e:FormEvent<HTMLFormElement>){
  e.preventDefault();
  if(!editing||cannotSave)return;
  const submitter=(e.nativeEvent as SubmitEvent).submitter as HTMLButtonElement|null;
  const intent=submitter?.value||'DRAFT';
  const form=new FormData(e.currentTarget);
  const invoice={
   visitId:editing.id?editing.visitId:Number(form.get('visitId')),
   vatRate:Number(form.get('vatRate')),
   items:items.map(({key,id,wholeQuantity,...item})=>({...item,quantity:Number(item.quantity),unitPrice:Number(item.unitPrice)})),
  };
  const persist=async()=>{await action.run(async()=>{
   const saved=intent==='DRAFT'
    ? await api<Invoice>(editing.id?'/invoices/'+editing.id+'/items':'/invoices',{method:editing.id?'PUT':'POST',body:invoice})
    : await api<Invoice>('/invoices/checkout',{method:'POST',body:{invoiceId:editing.id||null,invoice,targetStatus:intent}});
   closeEditor();setSelected(saved.id);resource.reload();visits.reload();
  },intent==='PAID'?'Đã lưu hóa đơn và ghi nhận thu đủ tiền.':intent==='SENT'?'Đã chốt hóa đơn, đang chờ thanh toán.':'Đã lưu hóa đơn nháp.');};
  if(intent==='DRAFT')await persist();
  else setCheckout({intent,total,save:persist});
 }
 async function finalizeDraft(invoice:Invoice,targetStatus:'SENT'|'PAID'){
  await action.run(async()=>{
   await api<Invoice>('/invoices/checkout',{method:'POST',body:{
    invoiceId:invoice.id,invoice:{visitId:invoice.visitId,vatRate:invoice.vatRate,items:invoice.items},targetStatus,
   }});
   setCheckout(null);setConfirmation(null);setSelected(invoice.id);resource.reload();
  },targetStatus==='PAID'?'Đã chốt hóa đơn và ghi nhận thu đủ tiền.':'Đã chốt hóa đơn, đang chờ thanh toán.');
 }
 function updateItem(key:string,field:'description'|'category'|'quantity'|'unitPrice',value:string){setItems(items.map(i=>i.key===key?{...i,[field]:value} as ItemDraft:i));}
 return <><Heading title="Hóa đơn & thanh toán" subtitle="Lập hóa đơn, kiểm tra tổng tiền rồi chọn đã thu đủ hoặc chờ thanh toán.">{canWrite&&<button className="primary" onClick={()=>edit(null)}>+ Lập hóa đơn</button>}</Heading><Notice action={action}/>{checkout&&<section className="panel task-guidance" aria-label="Xác nhận thu tiền"><h2>{checkout.intent==='PAID'?'Xác nhận đã nhận đủ tiền':'Chốt hóa đơn chờ thanh toán'}</h2><strong>Tổng cần thu: {money(checkout.total)}</strong><p>{checkout.intent==='PAID'?'Chỉ xác nhận sau khi đã nhận đủ tiền từ chủ nuôi.':'Hóa đơn sẽ nằm trong danh sách chờ thu tiền.'}</p><div className="actions"><button className="primary" disabled={action.busy} onClick={()=>checkout.save()}>{checkout.intent==='PAID'?'Đã nhận đủ tiền, lưu hóa đơn':'Chốt và để chờ thanh toán'}</button><button disabled={action.busy} onClick={()=>setCheckout(null)}>Quay lại kiểm tra</button></div></section>}{visitId&&<p>Hóa đơn của lượt khám #{visitId}. <Link to={`/visits/${visitId}`}>Quay lại lượt khám</Link> · <Link to="/invoices">Xem tất cả hóa đơn</Link></p>}<label className="search">Trạng thái<select value={filter} onChange={e=>setFilter(e.target.value)}><option value="">Tất cả</option>{['PENDING','DRAFT','SENT','PAID'].map(s=><option value={s} key={s}>{s==='PENDING'?'Nháp và chờ thanh toán':labels[s]}</option>)}</select></label>
 {!editing&&canWrite&&(params.get('task')==='create'||needsInvoice.length>0)&&<section className="panel">
  <h2>Lượt đã khám xong cần lập hóa đơn</h2>
  <p className="muted">Chọn lượt khám để mở sẵn hóa đơn và các khoản thuốc đã kê.</p>
  <LoadState resource={visits}><LoadState resource={resource}><Table rows={needsInvoice} empty="Đã lập hóa đơn cho tất cả lượt khám hoàn thành." columns={[
   {label:'Thú cưng',key:'petName'},
   {label:'Lượt khám',render:v=>'#'+v.id},
   {label:'Bác sĩ',key:'vetName'},
   {label:'Thao tác',render:v=><Link className="button primary" to={'/invoices?visitId='+v.id+'&create=1'}>Lập hóa đơn</Link>},
  ]}/></LoadState></LoadState>
 </section>}
 {detail&&!editing&&<section className="panel task-guidance" aria-label="Công việc của hóa đơn đang chọn">
  <strong>Hóa đơn #{detail.id} · {detail.petName} · {money(detail.total)}</strong>
  <span>{detail.status==='SENT'?'Chờ thu tiền. Chỉ xác nhận khi đã nhận đủ tiền từ chủ nuôi.':detail.status==='DRAFT'?'Kiểm tra các khoản thu rồi chốt hóa đơn để chuyển sang thu tiền.':'Đã thu tiền cho hóa đơn này.'}</span>
  <div className="row-actions">{canWrite&&detail.status==='DRAFT'&&<button onClick={()=>edit(detail)}>Sửa hóa đơn</button>}{canWrite&&detail.status!=='PAID'&&<button className="primary" disabled={action.busy} onClick={()=>setConfirmation(detail)}>{detail.status==='DRAFT'?'Kiểm tra và chốt hóa đơn':'Xác nhận đã thu tiền'}</button>}</div>
 </section>}
 {confirmation&&<section className="panel" aria-label="Xác nhận hóa đơn"><h2>{confirmation.status==='DRAFT'?'Chốt hóa đơn':'Ghi nhận đã nhận tiền'} #{confirmation.id}</h2><p>Thú cưng: {confirmation.petName}. Tổng tiền: <strong>{money(confirmation.total)}</strong>.</p><p>{confirmation.status==='DRAFT'?'Chỉ chốt sau khi lượt khám hoàn thành. Sau khi chốt, đơn thuốc sẽ bị khóa sửa/hủy.':'Chỉ xác nhận sau khi thực sự nhận đủ tiền. Đây là ghi nhận thu tiền, không phải giao dịch ngân hàng.'}</p><div className="actions">{confirmation.status==='DRAFT'&&<button className="primary" disabled={action.busy} onClick={()=>{
 setCheckout({intent:'PAID',total:confirmation.total,save:()=>finalizeDraft(confirmation,'PAID')});
}}>Chốt và xác nhận đã thu tiền</button>}<button className={confirmation.status==='DRAFT'?'':'primary'} disabled={action.busy} onClick={()=>{if(confirmation.status==='DRAFT'){finalizeDraft(confirmation,'SENT');return;}action.run(async()=>{await api(`/invoices/${confirmation.id}/status`,{method:'PATCH',body:{status:'PAID'}});setConfirmation(null);resource.reload();});}}>{confirmation.status==='DRAFT'?'Chốt, chờ thanh toán':'Xác nhận đã nhận tiền'}</button><button disabled={action.busy} onClick={()=>setConfirmation(null)}>Quay lại</button></div></section>}
 {editing&&<form key={editing.id||'new'} className="panel" onSubmit={submit}><h2>{editing.id?`Sửa hóa đơn #${editing.id}`:'Hóa đơn mới'}</h2><LoadState resource={visits}><fieldset disabled={action.busy||!!checkout}><div className="form-grid">{!editing.id&&<Field name="visitId" label="Lượt khám" required value={draftVisitId} onValueChange={value=>{setDraftVisitId(value);setItems(null);}} options={(visits.data||[]).filter(v=>v.status==='COMPLETED'&&!(resource.data||[]).some(invoice=>invoice.visitId===v.id)).map(v=>({value:v.id,label:`#${v.id} · ${v.petName} · ${dateTime(v.scheduledAt)}`}))}/>}<Field name="vatRate" label="Thuế suất (0.1 = 10%)" type="number" required min={0} max={1} step={0.0001} value={editing.vatRate??0} onValueChange={value=>setVatRate(Number(value))}/></div><LoadState resource={charges}/><div className="task-guidance"><label>Dịch vụ theo bảng giá<select value={serviceId} onChange={e=>setServiceId(e.target.value)}><option value="">Chọn dịch vụ…</option>{services.data?.filter(s=>s.active).map(s=><option key={s.id} value={s.id}>{s.name} · {money(s.unitPrice)}</option>)}</select></label><LoadState resource={services}/><button type="button" disabled={!serviceId||services.loading||!!services.error} onClick={()=>{
 const service=services.data?.find(s=>String(s.id)===serviceId);
 if(!service)return;
 const line={key:crypto.randomUUID(),category:service.category,quantity:1,unitPrice:service.unitPrice,description:service.name};
 const nonempty=items.filter(i=>i.description.trim()||Number(i.unitPrice)!==0);
 setItems([...nonempty,line]);setServiceId('');
}}>Thêm dịch vụ vào hóa đơn</button></div>{items.map((item,i)=><div className="line-item" key={item.key}><label>Mô tả<input aria-label={`Mô tả dòng ${i+1}`} required maxLength={500} value={item.description} onChange={e=>updateItem(item.key,'description',e.target.value)}/></label><label>Loại<select value={item.category} onChange={e=>updateItem(item.key,'category',e.target.value)}>{['CONSULTATION','VACCINATION','SURGERY','HOSPITAL','MEDICATION','OTHER'].map(s=><option key={s} value={s}>{labels[s]}</option>)}</select></label><label>Số lượng<input type="number" required min={item.wholeQuantity?1:0.01} max={99999999.99} step={item.wholeQuantity?1:0.01} value={item.quantity} onChange={e=>updateItem(item.key,'quantity',e.target.value)}/></label><label>Đơn giá<input type="number" required min={0} max={9999999999.99} step={0.01} value={item.unitPrice} onChange={e=>updateItem(item.key,'unitPrice',e.target.value)}/></label><button type="button" disabled={items.length===1} onClick={()=>setItems(items.filter(i=>i.key!==item.key))}>Bỏ dòng</button></div>)}<button type="button" onClick={()=>setItems([...items,{key:crypto.randomUUID(),category:'OTHER',quantity:1,unitPrice:0,description:''}])}>+ Thêm dòng</button><div className="task-guidance" aria-label="Tổng tiền cần thu"><span>Tạm tính: {money(subtotal)}</span><span>VAT: {money(tax)}</span><strong>Tổng cần thu: {money(total)}</strong></div></fieldset><div className="actions"><button type="submit" name="intent" value="DRAFT" disabled={cannotSave}>Lưu nháp</button><button className="primary" type="submit" name="intent" value="PAID" disabled={cannotSave}>Lưu và xác nhận đã thu tiền</button><button type="submit" name="intent" value="SENT" disabled={cannotSave}>Chốt, chờ thanh toán</button><button type="button" onClick={closeEditor}>Hủy</button></div></LoadState></form>}
 <LoadState resource={resource}><section className="panel no-print"><Table rows={rows} columns={[{label:'Hóa đơn',render:r=><button onClick={()=>setSelected(r.id)}>#{r.id}</button>},{label:'Thú cưng',key:'petName'},{label:'Ngày lập',render:r=>dateTime(r.issuedAt)},{label:'Ngày thu tiền',render:r=>r.paidAt?dateTime(r.paidAt):r.status==='PAID'?'Chưa rõ (hóa đơn cũ)':'—'},{label:'Tổng',render:r=>money(r.total)},{label:'Trạng thái',render:r=><Badge value={r.status}/>},{label:'Thao tác',render:r=><div className="row-actions">{canWrite&&r.status==='DRAFT'&&<button onClick={()=>edit(r)}>Sửa</button>}{canWrite&&r.status!=='PAID'&&<button disabled={action.busy} onClick={()=>setConfirmation(r)}>{r.status==='DRAFT'?'Chốt hóa đơn':'Đã nhận tiền'}</button>}</div>}]} /></section></LoadState>
 {detail&&<article className="panel invoice-print"><div className="toolbar"><h2>VetClinic · Hóa đơn #{detail.id}</h2><button className="no-print" onClick={()=>window.print()}>In / Lưu PDF</button></div><p>Thú cưng: {detail.petName} · Lượt khám #{detail.visitId}</p><p>{dateTime(detail.issuedAt)} · {labels[detail.status]}</p>{detail.status==='PAID'&&<p>Ngày thu tiền: {detail.paidAt?dateTime(detail.paidAt):'Chưa rõ (hóa đơn cũ)'}</p>}<Table paginate={false} rows={detail.items} columns={[{label:'Dịch vụ / thuốc',key:'description'},{label:'Nhóm',render:r=>labels[r.category]},{label:'SL',key:'quantity'},{label:'Đơn giá',render:r=>money(r.unitPrice)},{label:'Thành tiền',render:r=>money(r.lineTotal)}]}/><p>Tạm tính: {money(detail.subtotal)}</p><p>VAT ({Number(detail.vatRate)*100}%): {money(detail.vatAmount)}</p><h2>Tổng: {money(detail.total)}</h2></article>}</>;
}

