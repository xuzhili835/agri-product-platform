/**
 * 地址 API 模块
 */
import request from '@/utils/request'

/**
 * 获取地址列表
 */
export function getAddressList() {
  return request({
    url: '/address/list',
    method: 'get'
  })
}

/**
 * 获取默认地址
 */
export function getDefaultAddress() {
  return request({
    url: '/address/default',
    method: 'get'
  })
}

/**
 * 添加地址
 * @param {Object} data - 地址信息
 */
export function addAddress(data) {
  // 转换前端数据格式到后端格式
  const backendData = {
    consignee: data.realName,
    phone: data.phone,
    province: data.province,
    city: data.city,
    area: data.area,
    addressDetail: data.detail,
    isDefault: data.isDefault ? 1 : 0
  }
  return request({
    url: '/address',
    method: 'post',
    data: backendData
  })
}

/**
 * 更新地址
 * @param {String|Number} id - 地址ID
 * @param {Object} data - 地址信息
 */
export function updateAddress(id, data) {
  // 转换前端数据格式到后端格式
  const backendData = {
    consignee: data.realName,
    phone: data.phone,
    province: data.province,
    city: data.city,
    area: data.area,
    addressDetail: data.detail,
    isDefault: data.isDefault ? 1 : 0
  }
  return request({
    url: `/address/${id}`,
    method: 'put',
    data: backendData
  })
}

/**
 * 删除地址
 * @param {String|Number} id - 地址ID
 */
export function deleteAddress(id) {
  return request({
    url: `/address/${id}`,
    method: 'delete'
  })
}

/**
 * 设置默认地址
 * @param {String|Number} id - 地址ID
 */
export function setDefaultAddress(id) {
  return request({
    url: `/address/${id}/default`,
    method: 'put'
  })
}
