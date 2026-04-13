/** 与客户端知识/论坛评论展示一致的 XSS 粗过滤，用于 v-html */
export function sanitizeHtml(rawHtml) {
  const parser = new DOMParser()
  const doc = parser.parseFromString(String(rawHtml || ''), 'text/html')
  doc.querySelectorAll('script,iframe,object,embed,link,meta,style').forEach((el) => el.remove())
  doc.querySelectorAll('*').forEach((el) => {
    Array.from(el.attributes).forEach((attr) => {
      const name = attr.name.toLowerCase()
      const value = String(attr.value || '')
      if (name.startsWith('on')) {
        el.removeAttribute(attr.name)
      }
      if ((name === 'href' || name === 'src') && /^javascript:/i.test(value.trim())) {
        el.removeAttribute(attr.name)
      }
    })
  })
  return doc.body.innerHTML
}

/** 已是 HTML 则 sanitize；否则转义并保留换行 */
export function getPostDisplayHtml(raw) {
  const s = String(raw ?? '')
  if (!s.trim()) return ''
  if (/<[a-z][\s\S]*>/i.test(s.trim())) return sanitizeHtml(s)
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/\n/g, '<br/>')
}
