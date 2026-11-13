import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api, asError } from './api';
import { Heading, LoadState, ErrorNotice, useData } from './ui';
import type { Pet } from './types';

interface Answer { mode:string;answer:string;insufficientEvidence:boolean;sources:{id:string;title:string;text:string;href:string;updatedAt:string}[] }
export default function Assistant(){
 const pets=useData<Pet[]>('/pets');
 const [petId,setPetId]=useState(''),[question,setQuestion]=useState(''),[generate,setGenerate]=useState(false);
 const [aiUnavailable,setAiUnavailable]=useState(false);
 const [answer,setAnswer]=useState<Answer|null>(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
 async function ask(useAi:boolean){
  setBusy(true);setError('');setAnswer(null);setAiUnavailable(false);
  try {
   setAnswer(await api<Answer>('/assistant/ask',{method:'POST',body:{petId:Number(petId),question:question.trim(),generate:useAi},timeoutMs:195000}));
  } catch(error) {
   const failure=asError(error);
   setError(failure.message);
   setAiUnavailable(useAi&&failure.status===503);
  } finally {
   setBusy(false);
  }
 }
 function submit(e:FormEvent){e.preventDefault();void ask(generate);}
  return <><Heading title="Trợ lý hồ sơ bệnh án" subtitle="Tra cứu theo từng thú cưng, kèm nguồn để kiểm tra lại."/>
 <LoadState resource={pets}><form className="panel" onSubmit={submit}><fieldset disabled={busy}>
 <label>Thú cưng<select required value={petId} onChange={e=>{setPetId(e.target.value);setAnswer(null);setError('');setAiUnavailable(false);}}><option value="">Chọn hồ sơ</option>{pets.data?.map(p=><option key={p.id} value={p.id}>{p.name} — {p.ownerName}</option>)}</select></label>
 <label>Câu hỏi<textarea required minLength={2} maxLength={1000} value={question} onChange={e=>{setQuestion(e.target.value);setAiUnavailable(false);setError('');}} placeholder="Ví dụ: Lịch sử chẩn đoán và điều trị? Vaccine đã tiêm?"/></label>
 <label className="checkbox"><input type="checkbox" checked={generate} onChange={e=>setGenerate(e.target.checked)}/>Tổng hợp bằng AI cục bộ</label>
 <p className="muted">Bỏ chọn để trích dữ liệu trực tiếp. Câu trả lời AI cần được đối chiếu với nguồn và bác sĩ kiểm tra.</p>
 <button className="primary" disabled={busy||!petId||question.trim().length<2||!pets.data?.length}>{busy?'Đang tra cứu…':'Tra cứu'}</button></fieldset></form></LoadState>
 <ErrorNotice error={error}/>{aiUnavailable&&<button type="button" disabled={busy} onClick={()=>{setGenerate(false);void ask(false);}}>Trích hồ sơ cho câu hỏi này</button>}{answer&&<section className="panel" aria-live="polite"><h2>{answer.mode==='LOCAL_RAG'?'Tổng hợp AI':answer.insufficientEvidence?'Thiếu bằng chứng':'Trích hồ sơ'}</h2><p className="pre-wrap">{answer.answer}</p>
 {!!answer.sources.length&&<h3>Nguồn đối chiếu</h3>}{answer.sources.map(s=><details key={s.id}><summary>{s.title} [{s.id}]</summary><p className="pre-wrap">{s.text}</p><p>Cập nhật: {s.updatedAt}</p><Link to={s.href}>Mở nguồn</Link></details>)}</section>}</>;
}
