import request from './request'

export function getMonitorOverview() {
  return request.get('/admin/monitor/overview')
}

export function getLogTail(lines = 200) {
  return request.get('/admin/monitor/logs/tail', { params: { lines } })
}
