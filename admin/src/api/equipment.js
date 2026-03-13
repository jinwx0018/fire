import request from './request'

/** 器材类型列表 */
export function getEquipmentTypeList() {
  return request.get('/equipment/type/list')
}

/** 器材分页（参数：pageNum, pageSize, keyword, typeId） */
export function getEquipmentPage(params) {
  return request.get('/equipment/list', { params })
}

/** 管理端：新增器材 */
export function addEquipment(data) {
  return request.post('/admin/equipment', data)
}

/** 器材详情 */
export function getEquipmentById(id) {
  return request.get(`/equipment/${id}`)
}

/** 管理端：修改器材 */
export function updateEquipment(id, data) {
  return request.put(`/admin/equipment/${id}`, data)
}

/** 管理端：删除器材 */
export function deleteEquipment(id) {
  return request.delete(`/admin/equipment/${id}`)
}
