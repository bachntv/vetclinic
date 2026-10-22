const KEY = 'vetclinic.accessToken';
export const tokenStore = { get: () => sessionStorage.getItem(KEY), set: (value: string) => sessionStorage.setItem(KEY, value), clear: () => sessionStorage.removeItem(KEY) };
const messages: Record<number,string> = { 400: 'Dữ liệu không hợp lệ. Hãy kiểm tra lại các trường.', 401: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', 403: 'Bạn không có quyền thực hiện thao tác này.', 404: 'Không tìm thấy dữ liệu. Có thể dữ liệu đã được lưu trữ.', 409: 'Thao tác xung đột với dữ liệu hiện tại.', 429: 'Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau một phút.' };
export class ApiError extends Error { status: number; constructor(status: number, message: string) { super(message); this.status = status; } }
export interface ApiOptions { method?: string; body?: unknown; signal?: AbortSignal; token?: string | null; timeoutMs?: number }
export function asError(value: unknown): Error & {status?: number} { return value instanceof Error ? value : new Error(String(value)); }
export async function api<T = unknown>(path: string, { method = 'GET', body, signal, token = tokenStore.get(), timeoutMs = 15000 }: ApiOptions = {}): Promise<T> {
  let response;
  const timeout=AbortSignal.timeout(timeoutMs); const requestSignal=signal?AbortSignal.any([signal,timeout]):timeout;
  try { response = await fetch(`/api${path}`, { method, signal:requestSignal, headers: { Accept: 'application/json', ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) }, ...(body !== undefined ? { body: JSON.stringify(body) } : {}) }); }
  catch (error) { const e=asError(error); if (e.name === 'AbortError') throw e; if(e.name==='TimeoutError')throw new ApiError(0,'Máy chủ phản hồi quá lâu. Vui lòng thử lại.'); throw new ApiError(0, 'Không thể kết nối máy chủ. Kiểm tra backend và kết nối mạng.'); }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401 && token && token === tokenStore.get()) { tokenStore.clear(); window.dispatchEvent(new Event('vetclinic:unauthorized')); }
    throw new ApiError(response.status, data?.detail || messages[response.status] || 'Máy chủ không xử lý được yêu cầu. Vui lòng thử lại.');
  }
  if(response.status!==204 && data===null)throw new ApiError(502,'Máy chủ trả về dữ liệu không hợp lệ. Kiểm tra cấu hình API/proxy.');
  return data as T;
}

