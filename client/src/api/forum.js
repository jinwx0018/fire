import request from './request'

export function getPostList(params) {
  return request.get('/forum/post/list', { params })
}

/** 当前用户发布的帖子（需登录；含待审核/驳回/通过） */
export function getMyPostList(params) {
  return request.get('/forum/post/mine', { params })
}

/** @param {{ recordView?: boolean }} [opts] recordView 默认 true；传 false 时不计浏览量 */
export function getPostDetail(id, opts = {}) {
  const params = {}
  if (opts.recordView === false) {
    params.recordView = false
  }
  return request.get(`/forum/post/${id}`, { params })
}

export function publishPost(data) {
  return request.post('/forum/post', data)
}

export function likePost(postId) {
  return request.post(`/forum/post/like/${postId}`)
}

export function collectForumPost(postId) {
  return request.post(`/forum/post/collect/${postId}`)
}

export function uncollectForumPost(postId) {
  return request.delete(`/forum/post/collect/${postId}`)
}

export function getCommentList(postId, params) {
  return request.get('/forum/comment/list', { params: { postId, ...params } })
}

export function submitComment(data) {
  return request.post('/forum/comment', data)
}

export function deleteComment(commentId) {
  return request.delete(`/forum/comment/${commentId}`)
}

/** 评论点赞/取消（需登录） */
export function likeForumComment(commentId) {
  return request.post(`/forum/comment/like/${commentId}`)
}

export function updatePost(id, data) {
  return request.put(`/forum/post/${id}`, data)
}

export function deletePost(id) {
  return request.delete(`/forum/post/${id}`)
}
