import {test,afterEach} from 'node:test';
import assert from 'node:assert/strict';
import {api,tokenStore} from './api.ts';
const original=globalThis.fetch,storage=new Map();
globalThis.sessionStorage={getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v),removeItem:k=>storage.delete(k)};
globalThis.window=new EventTarget();
afterEach(()=>{globalThis.fetch=original;storage.clear();});
test('sends Bearer token and JSON body; handles 204',async()=>{
 tokenStore.set('session-token');let captured;
 globalThis.fetch=async(url,options)=>{captured={url,...options};return new Response(null,{status:204});};
 assert.equal(await api('/pets/5',{method:'DELETE'}),null);
 assert.equal(captured.url,'/api/pets/5');assert.equal(captured.headers.Authorization,'Bearer session-token');
 await api('/owners',{method:'POST',body:{firstName:'An'}});assert.equal(captured.body,'{"firstName":"An"}');
});
test('login never sends an existing Bearer token',async()=>{
 tokenStore.set('old-token');globalThis.fetch=async(url,options)=>{assert.equal(options.headers.Authorization,undefined);return Response.json({accessToken:'new-token'});};
 assert.deepEqual(await api('/auth/login',{method:'POST',token:null,body:{email:'a@b.c',password:'test'}}),{accessToken:'new-token'});
});
test('preserves backend ProblemDetail for validation, missing and conflict errors',async()=>{
 for(const status of [400,403,404,409,429]){globalThis.fetch=async()=>Response.json({detail:'Chi tiết lỗi'},{status});await assert.rejects(api('/owners'),e=>e.status===status&&e.message==='Chi tiết lỗi');}
});
test('401 clears only current session and emits expiration',async()=>{
 tokenStore.set('current');let emitted=false;const listener=()=>{emitted=true;};window.addEventListener('vetclinic:unauthorized',listener);
 globalThis.fetch=async()=>new Response(null,{status:401});await assert.rejects(api('/auth/me'),e=>e.status===401);assert.equal(tokenStore.get(),null);assert.equal(emitted,true);window.removeEventListener('vetclinic:unauthorized',listener);
});
test('old request cannot remove a newer token',async()=>{
 tokenStore.set('new');globalThis.fetch=async()=>new Response(null,{status:401});await assert.rejects(api('/auth/me',{token:'old'}));assert.equal(tokenStore.get(),'new');
});
test('network and abort failures remain distinguishable',async()=>{
 globalThis.fetch=async()=>{throw new TypeError('network');};await assert.rejects(api('/owners'),e=>e.status===0);
 globalThis.fetch=async()=>{throw new DOMException('Cancelled','AbortError');};await assert.rejects(api('/owners'),e=>e.name==='AbortError');
});
test('rejects HTML returned by an incorrectly configured API proxy',async()=>{
 globalThis.fetch=async()=>new Response('<html>wrong server</html>',{status:200});await assert.rejects(api('/owners'),e=>e.status===502);
});
test('timeouts produce a retryable error rather than an endless loading state',async()=>{
 globalThis.fetch=async()=>{throw new DOMException('Timeout','TimeoutError');};await assert.rejects(api('/owners'),e=>e.status===0&&e.message.includes('quá lâu'));
});
