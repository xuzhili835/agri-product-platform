/**
 * 预约 API 模块
 */
import request from '@/utils/request'

/**
 * 创建预约
 * @param {Object} data - 预约数据
 */
export function makeReservation(data) {
  return request({
    url: '/reserve',
    method: 'post',
    data
  })
}

/**
 * 获取我的预约列表
 * @param {Object} params - 查询参数 { page, pageSize, status }
 */
export function getMyReserves(params) {
  return request({
    url: '/reserve/my',
    method: 'get',
    params
  })
}

/**
 * 获取我的预约列表（别名，保持兼容性）
 * @param {Object} params - 查询参数 { page, pageSize, status }
 */
export function getMyReserveList(params) {
  return getMyReserves(params)
}

/**
 * 取消预约
 * @param {String|Number} reserveId - 预约ID
 */
export function cancelReserve(reserveId) {
  return request({
    url: `/reserve/${reserveId}`,
    method: 'delete'
  })
}

/**
 * 获取预约详情
 * @param {String|Number} reserveId - 预约ID
 */
export function getReserveDetail(reserveId) {
  return request({
    url: `/reserve/${reserveId}`,
    method: 'get'
  })
}
