import request from './request'

export function getRecommendList(params) {
  return request.get('/recommend/list', { params })
}

export function reportBehavior(data) {
  return request.post('/recommend/behavior', data)
}

/** 推荐位埋点（无需登录）：离线 CTR / AB；actionType: EXPOSE | CLICK */
export function postRecommendFeedback(data) {
  return request.post('/recommend/feedback', data)
}
