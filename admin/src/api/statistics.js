import request from './request'

/** 内容维度统计 */
export function getContentStats() {
  return request.get('/admin/statistics/content')
}

/** 用户维度统计 */
export function getUserStats() {
  return request.get('/admin/statistics/user')
}

/** 互动维度统计 */
export function getInteractionStats() {
  return request.get('/admin/statistics/interaction')
}

/** 分类知识发布量（饼图） */
export function getCategoryPie() {
  return request.get('/admin/statistics/chart/categoryPie')
}

/** 月度浏览量（折线图） */
export function getViewTrend(params) {
  return request.get('/admin/statistics/chart/viewTrend', { params })
}

/** 导出 Excel */
export function exportExcel(params) {
  return request.get('/admin/statistics/export', { params, responseType: 'blob' })
}
