import request from './request'

export function getPostList(params) {
  return request.get('/forum/post/list', { params })
}

export function getPostDetail(id) {
  return request.get(`/forum/post/${id}`)
}

export function publishPost(data) {
  return request.post('/forum/post', data)
}

export function likePost(postId) {
  return request.post(`/forum/post/like/${postId}`)
}

export function getCommentList(postId, params) {
  return request.get('/forum/comment/list', { params: { postId, ...params } })
}

export function submitComment(data) {
  return request.post('/forum/comment', data)
}
