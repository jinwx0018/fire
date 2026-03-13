import request from './request'

export function getNewsList(params) {
  return request.get('/news/list', { params })
}

export function getNewsDetail(id) {
  return request.get(`/news/${id}`)
}
