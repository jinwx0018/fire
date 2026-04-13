import request from './request'

/** 器材类型列表 */
export function getEquipmentTypeList() {
  return request.get('/equipment/type/list')
}

/** 管理端器材类型分页 */
export function getEquipmentTypeAdminPage(params) {
  return request.get('/admin/equipment/type/page', { params })
}

/** 用户端器材分页（仅上架） */
export function getEquipmentPage(params) {
  return request.get('/equipment/list', { params })
}

/** 管理端器材分页（含下架；status 空=全部，0=下架，1=上架） */
export function getEquipmentAdminPage(params) {
  return request.get('/admin/equipment/page', { params })
}

/** 管理端：新增器材 */
export function addEquipment(data) {
  return request.post('/admin/equipment', data)
}

/** 管理端器材详情（含未上架，用于编辑） */
export function getEquipmentById(id) {
  return request.get(`/admin/equipment/${id}`)
}

/** 管理端：修改器材 */
export function updateEquipment(id, data) {
  return request.put(`/admin/equipment/${id}`, data)
}

/** 管理端：删除器材 */
export function deleteEquipment(id) {
  return request.delete(`/admin/equipment/${id}`)
}

/** 管理端：Excel 批量导入（multipart file 字段名 file） */
export function importEquipmentExcel(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/equipment/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 管理端：新增器材类型 */
export function addEquipmentType(data) {
  return request.post('/admin/equipment/type', data)
}

/** 管理端：修改器材类型 */
export function updateEquipmentType(id, data) {
  return request.put(`/admin/equipment/type/${id}`, data)
}

/** 管理端：删除器材类型 */
export function deleteEquipmentType(id) {
  return request.delete(`/admin/equipment/type/${id}`)
}

/** 器材图片上传（复用知识封面上传接口） */
export function uploadEquipmentImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/content/cover/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
