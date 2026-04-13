/**
 * contenteditable 常见为每行一个 &lt;div&gt;…&lt;/div&gt;，后端 OWASP 若未放行 div 会整段挤成一行。
 * 提交前转为 &lt;p&gt;，与论坛/知识/新闻评论存储结构一致。
 */
export function normalizeRichCommentHtml(rawHtml) {
  const raw = String(rawHtml ?? '')
  if (!raw.trim()) return raw
  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(raw, 'text/html')
    const body = doc.body
    let guard = 0
    while (body.querySelector('div') && guard++ < 800) {
      const div = body.querySelector('div')
      const p = doc.createElement('p')
      while (div.firstChild) {
        p.appendChild(div.firstChild)
      }
      div.parentNode.replaceChild(p, div)
    }
    return body.innerHTML
  } catch {
    return raw
  }
}

/** 与知识详情页一致的 XSS 粗过滤，用于 v-html */
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

/** 富文本是否视为空（与知识编辑校验一致；仅含图片也算有内容） */
export function isEmptyHtml(html) {
  if (html == null || !String(html).trim()) return true
  const raw = String(html)
  if (/<img[^>]+>/i.test(raw)) return false
  const stripped = raw
    .replace(/<p><br\s*\/?><\/p>/gi, '')
    .replace(/<p>\s*<\/p>/gi, '')
    .replace(/<br\s*\/?>/gi, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return !stripped
}

/** 论坛帖子展示：已是 HTML 则 sanitize；否则转义并保留换行（兼容历史纯文本帖） */
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

/** 编辑区：历史纯文本帖转为简单 HTML，便于富文本编辑 */
export function plainToEditorHtml(raw) {
  const s = String(raw ?? '')
  if (!s.trim()) return ''
  if (/<[a-z][\s\S]*>/i.test(s.trim())) return s
  const esc = s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  return '<p>' + esc.replace(/\n/g, '<br/>') + '</p>'
}
