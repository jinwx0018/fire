import request from './request'

export function getAuditLogPage(params) {
  return request.get('/admin/audit-logs', { params })
}
