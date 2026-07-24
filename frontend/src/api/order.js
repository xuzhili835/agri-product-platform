/**
 * 订单 API 模块
 */
import request from '@/utils/request'

/**
 * 创建订单
 * @param {Object} data - { items: [{ productId, quantity }] }
 */
export function createOrder(data) {
  return request({
    url: '/purchase',
    method: 'post',
    data
  })
}

/**
 * 获取订单列表（包含商品详情，带分页信息）
 * @param {Object} params - { page, pageSize }
 */
export function getOrders(params) {
  return request({
    url: '/purchase/list/paged',
    method: 'get',
    params
  })
}

/**
 * 获取农户收到的订单列表（别人购买农户商品的订单）
 * @param {Object} params - { page, pageSize }
 */
export function getReceivedOrders(params) {
  return request({
    url: '/purchase/received/paged',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 * @param {String|Number} id - 订单ID
 */
export function getOrderDetail(id) {
  return request({
    url: `/purchase/${id}`,
    method: 'get'
  })
}

/**
 * 取消订单
 * @param {String|Number} id - 订单ID
 */
export function cancelOrder(id) {
  return request({
    url: `/purchase/${id}`,
    method: 'delete'
  })
}

/**
 * 更新订单状态
 * @param {String|Number} id - 订单ID
 * @param {Object} data - { status }
 */
export function updateOrderStatus(id, data) {
  return request({
    url: `/purchase/${id}/status`,
    method: 'put',
    data
  })
}
