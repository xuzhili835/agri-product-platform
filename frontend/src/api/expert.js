/**
 * 专家 API 模块
 */
import request from '@/utils/request'

/**
 * 获取专家列表
 * @param {Object} params - 查询参数 { page, pageSize, specialty }
 */
export function getExpertList(params) {
  return request({
    url: '/expert/list',
    method: 'get',
    params
  })
}

/**
 * 获取专家详情
 * @param {String|Number} expertId - 专家ID
 */
export function getExpertDetail(expertId) {
  return request({
    url: `/expert/${expertId}`,
    method: 'get'
  })
}

/**
 * 获取当前专家信息
 */
export function getExpertInfo() {
  return request({
    url: '/expert/my/info',
    method: 'get'
  })
}

/**
 * 更新专家信息
 * @param {Object} data - 专家信息数据
 */
export function updateExpertInfo(data) {
  return request({
    url: '/expert/my/info',
    method: 'put',
    data
  })
}

/**
 * 获取专家的预约列表
 * @param {Object} params - 查询参数 { page, pageSize, status }
 */
export function getExpertReserveList(params) {
  return request({
    url: '/expert/reserve/list',
    method: 'get',
    params
  })
}

/**
 * 更新预约状态
 * @param {String|Number} reserveId - 预约ID
 * @param {Number} status - 状态 0待处理 1已完成 2已拒绝
 * @param {String} answer - 回复内容（可选）
 */
export function updateReserveStatus(reserveId, status, answer = '') {
  return request({
    url: `/expert/reserve/${reserveId}/status`,
    method: 'put',
    data: { status, answer }
  })
}
