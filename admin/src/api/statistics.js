import request from './request'

/** 内容维度统计；可选 params: { startDate, endDate } yyyy-MM-dd */
export function getContentStats(params) {
  return request.get('/admin/statistics/content', { params })
}

/** 用户维度统计 */
export function getUserStats(params) {
  return request.get('/admin/statistics/user', { params })
}

/** 互动维度统计 */
export function getInteractionStats(params) {
  return request.get('/admin/statistics/interaction', { params })
}

/** 分类知识发布量（饼图） */
export function getCategoryPie(params) {
  return request.get('/admin/statistics/chart/categoryPie', { params })
}

/** 月度浏览量（折线图，按年） */
export function getViewTrend(params) {
  return request.get('/admin/statistics/chart/viewTrend', { params })
}

/** 区间内按月趋势（与 startDate/endDate 一致；未选区间为当年） */
export function getViewTrendRange(params) {
  return request.get('/admin/statistics/chart/viewTrendRange', { params })
}

/** 全站运营大盘 */
export function getDashboard(params) {
  return request.get('/admin/statistics/dashboard', { params })
}

/** 导出 Excel；可传 type、startDate、endDate */
export function exportExcel(params) {
  return request.get('/admin/statistics/export', { params, responseType: 'blob' })
}
