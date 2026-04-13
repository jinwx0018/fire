import request from './request'

/** 管理端帖子分页（含待审核，status：0 待审核 1 通过 -1 驳回） */
export function getPostPage(params) {
  return request.get('/admin/forum/post/list', { params })
}

/** 管理端：帖子审核（通过/驳回）status: 1 通过 -1 驳回；驳回可传 rejectReason */
export function auditPost(id, data) {
  return request.put(`/admin/forum/post/audit/${id}`, data)
}

export function deletePost(id) {
  return request.delete(`/admin/forum/post/${id}`)
}

export function deleteComment(id) {
  return request.delete(`/admin/forum/comment/${id}`)
}

/** 管理端：全站论坛评论分页 */
export function getForumCommentAdminPage(params) {
  return request.get('/admin/forum/comment/page', { params })
}

/** 管理端：论坛评论显示/隐藏 status: 1 显示 0 隐藏 */
export function updateForumCommentStatus(commentId, data) {
  return request.put(`/admin/forum/comment/${commentId}/status`, data)
}

/** 管理端帖子详情（完整正文，不增浏览量） */
export function getAdminForumPostDetail(postId) {
  return request.get(`/admin/forum/post/detail/${postId}`)
}

/** 单条论坛评论审核上下文（完整帖子与父评论） */
export function getForumCommentAuditDetail(commentId) {
  return request.get(`/admin/forum/comment/${commentId}/audit-detail`)
}
