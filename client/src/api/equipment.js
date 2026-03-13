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
