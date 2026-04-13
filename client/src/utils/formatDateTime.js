/**
 * 全站统一日期时间展示：YYYY-MM-DD HH:mm（无 ISO 的 T、不显示秒），与产品图2一致。
 * 兼容后端 LocalDateTime 序列化、带毫秒、Z/时区后缀等。
 */
export function formatDateTime(value) {
  if (value == null || value === '') return ''
  if (typeof value === 'number' && Number.isFinite(value)) {
    return formatFromDate(new Date(value))
  }
  if (value instanceof Date) {
    return formatFromDate(value)
  }
  const s = String(value).trim()
  if (!s) return ''

  const isoLike = s.match(
    /^(\d{4}-\d{2}-\d{2})[T\s](\d{1,2}):(\d{2})(?::\d{2})?(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$/i,
  )
  if (isoLike) {
    const hh = String(isoLike[2]).padStart(2, '0')
    return `${isoLike[1]} ${hh}:${isoLike[3]}`
  }

  const dOnly = s.match(/^(\d{4}-\d{2}-\d{2})$/)
  if (dOnly) {
    return `${dOnly[1]} 00:00`
  }

  const d = new Date(s)
  if (!Number.isNaN(d.getTime())) {
    return formatFromDate(d)
  }
  return s
}

function pad2(n) {
  return n < 10 ? `0${n}` : String(n)
}

function formatFromDate(d) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}
