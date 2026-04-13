/** 从富文本评论中抽纯文本（与后端摘要展示一致，仅兜底用） */
function stripRichToPlain(html) {
  if (html == null) return ''
  const raw = String(html)
  const plain = raw
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  if (plain) {
    return plain.length > 280 ? plain.slice(0, 280) + '…' : plain
  }
  if (/<\s*img\b/i.test(raw)) return '[图片]'
  return ''
}

function findParentInFlat(c, flatComments) {
  if (!flatComments?.length || c?.parentId == null) return null
  return flatComments.find((x) => String(x?.id) === String(c.parentId)) ?? null
}

/**
 * 被回复评论的引用正文：优先接口 parentContentPreview；
 * 若缺省但同页列表里已有父评论（树扁平后常如此），从父节点正文兜底。
 */
export function replyQuoteText(c, flatComments) {
  const t = c?.parentContentPreview
  if (t != null && String(t).trim() !== '') return String(t).trim()
  const p = findParentInFlat(c, flatComments)
  if (p?.content != null) {
    const plain = stripRichToPlain(p.content)
    if (plain) return plain
  }
  return '（原评论已删除或暂时不可查看）'
}

/** 被回复用户展示名：优先接口 parentUserName，否则从同页父评论取 username / userName */
export function replyAtUsername(c, flatComments) {
  const n = c?.parentUserName
  if (n != null && String(n).trim() !== '') return String(n).trim()
  const p = findParentInFlat(c, flatComments)
  const u = p?.username ?? p?.userName
  if (u != null && String(u).trim() !== '') return String(u).trim()
  return '用户'
}
