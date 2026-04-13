import request from './request'

/** 用户端分页（仅上架），管理端若需与用户一致可用 */
export function getNewsPage(params) {
  return request.get('/news/list', { params })
}

/** 管理端：未删除新闻中的地区去重；无数据时与后端省级后备一致 */
export function getNewsRegions() {
  return request.get('/admin/news/regions')
}

function buildAdminNewsPageQuery(params) {
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

/** 管理端分页（含下架；status 空=全部）；regions 多选时重复传参 */
export function getNewsAdminPage(params) {
  return request.get(`/admin/news/page${buildAdminNewsPageQuery(params)}`)
}

/** 管理端：新增新闻 */
export function addNews(data) {
  return request.post('/admin/news', data)
}

/** 管理端新闻详情（含下架，用于编辑） */
export function getNewsById(id) {
  return request.get(`/admin/news/${id}`)
}

/** 管理端：修改新闻 */
export function updateNews(id, data) {
  return request.put(`/admin/news/${id}`, data)
}

/** 管理端：删除新闻 */
export function deleteNews(id) {
  return request.delete(`/admin/news/${id}`)
}

/** 公开：新闻分类下拉（免登录路径） */
export function getNewsCategoryOptions() {
  return request.get('/news/category/list')
}

/** 管理端：新闻评论分页 */
export function getNewsCommentAdminPage(params) {
  return request.get('/admin/news/comment/page', { params })
}

/** 单条新闻评论审核上下文（完整新闻与父评论） */
export function getNewsCommentAuditDetail(commentId) {
  return request.get(`/admin/news/comment/${commentId}/audit-detail`)
}

/** 管理端：删除新闻评论 */
export function deleteNewsComment(commentId) {
  return request.delete(`/admin/news/comment/${commentId}`)
}

/** 管理端：显示/隐藏新闻评论 status: 1 显示 0 隐藏 */
export function updateNewsCommentStatus(commentId, data) {
  return request.put(`/admin/news/comment/${commentId}/status`, data)
}
