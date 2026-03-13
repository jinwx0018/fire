import request from './request'

export function getRecommendList(params) {
  return request.get('/recommend/list', { params })
}

export function reportBehavior(data) {
  return request.post('/recommend/behavior', data)
}
