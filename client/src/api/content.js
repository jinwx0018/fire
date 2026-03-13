import request from './request'

export function getContentList(params) {
  return request.get('/content/list', { params })
}

export function getContentDetail(id) {
  return request.get(`/content/${id}`)
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
