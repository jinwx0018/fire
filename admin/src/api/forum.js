import request from './request'

/** 帖子分页列表（含待审核，可用 status 筛选：0 待审核 1 正常 -1 违规） */
export function getPostPage(params) {
  return request.get('/forum/post/list', { params })
}

/** 管理端：帖子审核（通过/驳回）status: 1 通过 -1 驳回；驳回可传 rejectReason */
export function auditPost(id, data) {
  return request.put(`/admin/forum/post/audit/${id}`, data)
}
