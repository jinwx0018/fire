import request from './request'

export function getEquipmentTypeList() {
  return request.get('/equipment/type/list')
}

export function getEquipmentList(params) {
  return request.get('/equipment/list', { params })
}

export function getEquipmentDetail(id) {
  return request.get(`/equipment/${id}`)
}

export function collectEquipment(id) {
  return request.post(`/equipment/${id}/collect`)
}

export function uncollectEquipment(id) {
  return request.delete(`/equipment/${id}/collect`)
}
