import DOMPurify from 'dompurify'

/** 与后端 OWASP 策略大致对齐的二次净化，用于新闻详情 v-html */
const CONFIG = {
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'em', 'b', 'i', 'u', 's', 'sub', 'sup',
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'ul', 'ol', 'li',
    'blockquote', 'pre', 'code', 'span', 'div', 'hr',
    'a', 'img',
    'table', 'thead', 'tbody', 'tfoot', 'tr', 'th', 'td', 'caption', 'col', 'colgroup',
  ],
  ALLOWED_ATTR: ['href', 'target', 'rel', 'src', 'alt', 'title', 'width', 'height', 'colspan', 'rowspan'],
  ALLOW_DATA_ATTR: false,
  /** 允许 http(s)、mailto 及本站相对路径图片/链接 */
  ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|\/[^/])/i,
}

export function sanitizeNewsHtml(dirty) {
  if (dirty == null || dirty === '') return ''
  return DOMPurify.sanitize(String(dirty), CONFIG)
}
