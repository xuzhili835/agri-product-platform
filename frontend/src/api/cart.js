/**
 * 购物车 API 模块
 */
import request from '@/utils/request'

/**
 * 添加商品到购物车
 * @param {Object} data - { productId, count }
 */
export function addToCart(data) {
  return request({
    url: '/cart',
    method: 'post',
    data
  })
}

/**
 * 获取购物车列表
 */
export function getCartList() {
  return request({
    url: '/cart/list',
    method: 'get'
  })
}

/**
 * 更新购物车商品数量
 * @param {String|Number} id - 购物车项ID
 * @param {Number} count - 数量
 */
export function updateCartItem(id, count) {
  return request({
    url: `/cart/${id}`,
    method: 'put',
    data: { count }
  })
}

/**
 * 删除购物车商品
 * @param {String|Number} id - 购物车项ID
 */
export function deleteCartItem(id) {
  return request({
    url: `/cart/${id}`,
    method: 'delete'
  })
}

/**
 * 清空购物车
 */
export function clearCart() {
  return request({
    url: '/cart/clear',
    method: 'delete'
  })
}
