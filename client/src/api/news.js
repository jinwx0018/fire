import request from './request'

/** Spring 绑定 List 需 regions=a&regions=b */
function buildQueryWithRegions(params) {
  if (!params) return ''
  const { regions, ...rest } = params
  const usp = new URLSearchParams()
  Object.entries(rest).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return
    usp.append(k, String(v))
  })
  if (regions?.length) {
    regions.forEach((r) => {
      if (r != null && String(r).trim() !== '') usp.append('regions', String(r).trim())
    })
  }
  const s = usp.toString()
  return s ? `?${s}` : ''
}

export function getNewsList(params) {
  return request.get(`/news/list${buildQueryWithRegions(params)}`)
}

/** 已上架新闻中出现过的地区（去重） */
export function getNewsRegions() {
  return request.get('/news/regions')
}

/** @param {{ recordView?: boolean }} [opts] 默认 true；false 时不计浏览量 */
export function getNewsDetail(id, opts = {}) {
  const params = {}
  if (opts.recordView === false) {
    params.recordView = false
  }
  return request.get(`/news/${id}`, { params })
}

/** 新闻分类字典（免登录） */
export function getNewsCategories() {
  return request.get('/news/category/list')
}

/** 新闻评论分页（免登录） */
export function getNewsComments(newsId, params) {
  return request.get(`/news/${newsId}/comments`, { params })
}

/** 发表评论（需登录） */
export function postNewsComment(newsId, body) {
  return request.post(`/news/${newsId}/comments`, body)
}

/** 点赞/取消点赞（需登录） */
export function toggleNewsLike(newsId) {
  return request.post(`/news/${newsId}/like`)
}

export function collectNews(newsId) {
  return request.post(`/news/${newsId}/collect`)
}

export function uncollectNews(newsId) {
  return request.delete(`/news/${newsId}/collect`)
}

/** 新闻评论点赞/取消（需登录） */
export function likeNewsComment(commentId) {
  return request.post(`/news/comment/like/${commentId}`)
}

/** 删除新闻评论（需登录，仅本人） */
export function deleteNewsComment(commentId) {
  return request.delete(`/news/comment/${commentId}`)
}

/**
 * 浏览器可打开的 RSS 绝对地址。
 * 后端 context-path 为 /api 时，开发环境代理下一般为 origin + /api/news/rss。
 */
export function getNewsRssAbsoluteUrl() {
  let base = import.meta.env.VITE_API_BASE_URL || '/api'
  base = base.replace(/\/$/, '')
  const suffix = '/news/rss'
  if (/^https?:\/\//i.test(base)) {
    return `${base}${suffix}`
  }
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  const path = base.startsWith('/') ? base : `/${base}`
  return `${origin}${path}${suffix}`
}
