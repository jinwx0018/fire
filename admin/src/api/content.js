import request from './request'

/** 内容分页（管理端专用，可传 status；未传 status 时返回全部状态） */
export function getContentPage(params) {
  return request.get('/admin/content/page', { params })
}

/** 新增内容（作者/管理员） */
export function addContent(data) {
  return request.post('/content', data)
}

/** 内容详情 */
export function getContentById(id) {
  return request.get(`/content/${id}`)
}

/** 修改内容 */
export function updateContent(id, data) {
  return request.put(`/content/${id}`, data)
}

/** 删除内容（逻辑删除） */
export function deleteContent(id) {
  return request.delete(`/content/${id}`)
}

/** 分类列表（树形或平铺） */
export function getCategoryList() {
  return request.get('/content/category/list')
}

/** 管理端：新增分类 */
export function addCategory(data) {
  return request.post('/admin/content/category', data)
}

/** 管理端：修改分类 */
export function updateCategory(id, data) {
  return request.put(`/admin/content/category/${id}`, data)
}

/** 管理端：删除分类 */
export function deleteCategory(id) {
  return request.delete(`/admin/content/category/${id}`)
}

/** 管理端：审核知识内容。status 1=通过 0=驳回；可选 rejectReason */
export function auditContent(id, status, rejectReason) {
  return request.put(`/admin/content/audit/${id}`, { status, rejectReason })
}

/** 管理端：切换内容状态（1发布/2下架） */
export function changeContentStatus(id, status) {
  return request.put(`/admin/content/status/${id}`, { status })
}

/** 管理端：回收站分页 */
export function getRecyclePage(params) {
  return request.get('/admin/content/recycle/page', { params })
}

/** 管理端：恢复回收站内容。module：KNOWLEDGE | NEWS | FORUM */
export function restoreContent(id, module = 'KNOWLEDGE') {
  return request.put(`/admin/content/restore/${id}`, {}, { params: { module } })
}

/** 回收站：彻底删除（物理删除）。module 同上 */
export function permanentDeleteRecycle(id, module = 'KNOWLEDGE') {
  return request.delete(`/admin/content/recycle/${id}`, { params: { module } })
}

/** 清空回收站（物理删除全部） */
export function purgeRecycleAll() {
  return request.delete('/admin/content/recycle/all')
}

/** 批量逻辑删除 */
export function batchDeleteContent(ids) {
  return request.post('/admin/content/batch/delete', { ids })
}

/** 批量下架（仅对已发布生效） */
export function batchOfflineContent(ids) {
  return request.post('/admin/content/batch/offline', { ids })
}

/** 批量恢复发布（仅对已下架生效） */
export function batchPublishContent(ids) {
  return request.post('/admin/content/batch/publish', { ids })
}

/** 富文本编辑器插入图片（管理端，需登录） */
export function uploadEditorImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/admin/content/editor/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 管理端：知识评论分页 */
export function getContentCommentAdminPage(params) {
  return request.get('/admin/content/comment/page', { params })
}

/** 单条知识评论审核上下文 */
export function getContentCommentAuditDetail(commentId) {
  return request.get(`/admin/content/comment/${commentId}/audit-detail`)
}

/** 管理端：删除知识评论 */
export function deleteContentComment(commentId) {
  return request.delete(`/admin/content/comment/${commentId}`)
}

/** 管理端：显示/隐藏知识评论 status: 1 显示 0 隐藏 */
export function updateContentCommentStatus(commentId, data) {
  return request.put(`/admin/content/comment/${commentId}/status`, data)
}

/** 知识封面图上传（与头像规则一致：≤2MB，需登录；作者/管理员写知识时用） */
export function uploadCoverImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/content/cover/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
