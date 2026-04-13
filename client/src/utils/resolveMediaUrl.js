/**
 * 将后端返回的相对资源路径拼成可请求的 URL（封面图等）
 */
export function resolveMediaUrl(path) {
  if (path == null || path === '') return ''
  const s = String(path).trim()
  if (/^https?:\/\//i.test(s)) return s
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  const p = s.startsWith('/') ? s : `/${s}`
  if (base.startsWith('http')) {
    return base.replace(/\/?$/, '') + p
  }
  return base.replace(/\/?$/, '') + p
}
