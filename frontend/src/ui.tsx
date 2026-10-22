import { useEffect, useState } from 'react';
import { api,asError } from './api';
import type { ReactNode } from 'react';
export interface FieldSpec {name:string;label:string;type?:string;required?:boolean;options?:{value:string|number;label:string}[];maxLength?:number;minLength?:number;min?:number|string;max?:number|string;step?:number|string;onValueChange?:(value:string)=>void}
export interface Column<T> {label:string;key?:keyof T;render?:(row:T)=>ReactNode}
export interface Resource<T> {data:T|null;error:string;loading:boolean;status?:number;reload:()=>void}
export const scalar=(value:unknown):string|number|undefined=>typeof value==='string'||typeof value==='number'?value:undefined;
interface Action {busy:boolean;error:string;success:string;run:(fn:()=>Promise<unknown>,message?:string)=>Promise<boolean>}
export const labels:Record<string,string> = {ADMIN:'Quản trị viên',VET:'Bác sĩ',RECEPTIONIST:'Lễ tân',SCHEDULED:'Đã đặt lịch',CHECKED_IN:'Đã tiếp nhận',IN_EXAM:'Đang khám',COMPLETED:'Hoàn thành',CANCELLED:'Đã hủy',DRAFT:'Nháp',SENT:'Chờ thanh toán',PAID:'Đã thanh toán',OPEN:'Mới',IN_PROGRESS:'Đang xử lý',RESOLVED:'Đã giải quyết',CONSULTATION:'Khám',VACCINATION:'Tiêm chủng',SURGERY:'Phẫu thuật',HOSPITAL:'Lưu viện',MEDICATION:'Thuốc',OTHER:'Khác'};
export const money = (n:number|null|undefined) => new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(n || 0);
export const dateTime = (value:string|null|undefined) => value ? new Date(value).toLocaleString('vi-VN') : '—';
export const localDate = () => {const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;};
export function useData<T>(path:string|null):Resource<T> {
 const [state,setState]=useState<{data:T|null;error:string;loading:boolean;path:string|null;status?:number}>({data:null,error:'',loading:!!path,path}),[version,setVersion]=useState(0);
 useEffect(()=>{const c=new AbortController();setState({data:null,error:'',loading:!!path,path});if(!path)return;
 api<T>(path,{signal:c.signal}).then(data=>{if(!c.signal.aborted)setState({data,error:'',loading:false,path});}).catch(error=>{const e=asError(error);if(!c.signal.aborted&&e.name!=='AbortError')setState({data:null,error:e.message,status:e.status,loading:false,path});});return()=>c.abort();
 },[path,version]);return {...(state.path===path?state:{data:null,error:'',loading:!!path}),reload:()=>setVersion(v=>v+1)};
}
export function useAction():Action{const[busy,setBusy]=useState(false),[error,setError]=useState(''),[success,setSuccess]=useState('');return{busy,error,success,run:async(fn:()=>Promise<unknown>,message='Đã lưu thành công.')=>{setBusy(true);setError('');setSuccess('');try{await fn();setSuccess(message);return true;}catch(error){setError(asError(error).message);return false;}finally{setBusy(false);}}};}
export function Notice({action}:{action:Action}){return <><ErrorNotice error={action.error}/>{action.success&&<p className="success" role="status">{action.success}</p>}</>;}
export function ErrorNotice({error}:{error:string|null|undefined}){return error?<p className="error" role="alert">{error}</p>:null;}
export function LoadState<T>({resource,children}:{resource:Resource<T>;children?:ReactNode}){return resource.loading?<p role="status" className="panel">Đang tải…</p>:resource.error?<div className="panel"><ErrorNotice error={resource.error}/><button onClick={resource.reload}>Thử lại</button></div>:children;}
export function Badge({value}:{value:string}){return <span className={`badge status-${value}`}>{labels[value]||value}</span>;}
export function Heading({title,subtitle,children}:{title:string;subtitle?:string;children?:ReactNode}){return <header><div><span className="eyebrow">VETCLINIC · QUẢN LÝ PHÒNG KHÁM</span><h1>{title}</h1>{subtitle&&<p className="muted">{subtitle}</p>}</div>{children}</header>;}
export function Field({name,label,type='text',required=false,value,options,maxLength,min,max,step,minLength,onValueChange}:FieldSpec&{value?:string|number|null}){
  if(type==='datetime-local'&&value)value=String(value).slice(0,16);
  return <label>{label}{required?' *':''}{options?<select onChange={onValueChange?e=>onValueChange(e.target.value):undefined} name={name} required={required} defaultValue={value??''}><option value="">Chọn…</option>{options.map(o=><option key={o.value} value={o.value}>{o.label}</option>)}</select>:type==='textarea'?<textarea name={name} defaultValue={value??''} required={required} maxLength={maxLength} rows={4}/>:<input name={name} type={type} defaultValue={value??''} required={required} maxLength={maxLength} minLength={minLength} min={min} max={max} step={step}/>}</label>;
}
export function Table<T extends {id?:number|string}>({rows,columns,empty='Chưa có dữ liệu.',paginate=true}:{rows:T[];columns:Column<T>[];empty?:string;paginate?:boolean}){
  const[page,setPage]=useState(0);const size=10,maxPage=Math.max(0,Math.ceil(rows.length/size)-1),safePage=Math.min(page,maxPage);
  return rows.length?<><div className="table-wrap"><table><thead><tr>{columns.map(c=><th key={c.label}>{c.label}</th>)}</tr></thead><tbody>{(paginate?rows.slice(safePage*size,(safePage+1)*size):rows).map((r,i)=><tr key={r.id??i}>{columns.map(c=><td key={c.label}>{c.render?c.render(r):c.key?String(r[c.key]??'—'):'—'}</td>)}</tr>)}</tbody></table></div>{paginate&&<div className="pagination"><span>{rows.length} kết quả · Trang {safePage+1}/{maxPage+1}</span><button disabled={safePage===0} onClick={()=>setPage(safePage-1)}>Trước</button><button disabled={safePage===maxPage} onClick={()=>setPage(safePage+1)}>Sau</button></div>}</>:<p className="empty">{empty}</p>;
}
export function formValues(form:HTMLFormElement,fields:FieldSpec[]){const raw=new FormData(form),out:Record<string,string|number|null>={};for(const field of fields){let v=raw.get(field.name);if(typeof v==='string')v=v.trim();if(field.required&&!v)throw new Error(`Vui lòng nhập ${field.label}`);out[field.name]=v===null||v===''?null:field.type==='number'?Number(v):String(v);}return out;}
