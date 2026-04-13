import request from './request'

/** 待审核作者申请列表 */
export function listPending() {
  return request.get('/admin/author-applications/pending')
}

/** 已处理（已通过/已驳回）分页，按审核时间倒序 */
export function listProcessedPage(params) {
  return request.get('/admin/author-applications/processed/page', { params })
}

/** 通过作者申请 */
export function approve(id, reviewRemark) {
  return request.put(`/admin/author-applications/${id}/approve`, { reviewRemark: reviewRemark || undefined })
}

/** 驳回作者申请 */
export function reject(id, rejectReason) {
  return request.put(`/admin/author-applications/${id}/reject`, { rejectReason: rejectReason || undefined })
}
