import request from './request'

export function getContentList(params) {
  return request.get('/content/list', { params })
}

export function getMyDrafts(params) {
  return request.get('/content/my/drafts', { params })
}

/** @param {{ recordView?: boolean }} [opts] 默认 true；false 时不计浏览量 */
export function getContentDetail(id, opts = {}) {
  const params = {}
  if (opts.recordView === false) {
    params.recordView = false
  }
  return request.get(`/content/${id}`, { params })
}

/** 知识评论分页（公开） */
export function getContentComments(id, params) {
  return request.get(`/content/${id}/comments`, { params })
}

/** 发表评论（需登录） body: { content, parentId? } */
export function postContentComment(id, data) {
  return request.post(`/content/${id}/comments`, data)
}

/** 知识评论点赞/取消（需登录） */
export function likeKnowledgeComment(commentId) {
  return request.post(`/content/comment/like/${commentId}`)
}

/** 删除知识评论（需登录，仅本人） */
export function deleteKnowledgeComment(commentId) {
  return request.delete(`/content/comment/${commentId}`)
}

export function getCategoryList() {
  return request.get('/content/category/list')
}

export function collect(contentId) {
  return request.post(`/content/collect/${contentId}`)
}

export function cancelCollect(contentId) {
  return request.delete(`/content/collect/${contentId}`)
}

export function getCollectList(params) {
  return request.get('/content/collect/list', { params })
}

/** 生成分享链接 */
export function getShareUrl(contentId) {
  return request.get(`/content/share/${contentId}`)
}

export function publishContent(id) {
  return request.post(`/content/publish/${id}`)
}

/** 作者下架内容（status=2），走 PUT /content/{id} 与旧版后端兼容，无需单独 offline 路由 */
export function offlineContent(id) {
  return updateContent(id, { status: 2 })
}

/** 新增知识（作者/管理员），body: title, categoryId, content, summary?, cover?, status 默认 0 草稿 */
export function saveContent(data) {
  return request.post('/content', data)
}

/** 更新知识 */
export function updateContent(id, data) {
  return request.put(`/content/${id}`, data)
}

/** 删除知识 */
export function deleteContent(id) {
  return request.delete(`/content/${id}`)
}

/** 点赞/取消点赞 */
export function toggleContentLike(contentId) {
  return request.post(`/content/like/${contentId}`)
}

/** 富文本编辑器插入图片（需登录） */
export function uploadEditorImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/content/editor/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 知识封面图上传（与头像规则一致：≤2MB） */
export function uploadCoverImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/content/cover/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
