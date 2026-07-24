/**
 * 支付宝支付 API 模块
 */
import request from '@/utils/request'

/**
 * 发起扫码支付：返回支付宝二维码链接（qrCode），前端渲染成二维码。
 * @param {String|Number} purchaseId - 订单ID
 */
export function payOrder(purchaseId) {
  return request({
    url: '/alipay/pay',
    method: 'post',
    data: { purchaseId }
  })
}

/**
 * 主动查询支付结果（后端会自动把已支付订单标记为已付款）。
 * @param {String|Number} purchaseId - 订单ID
 */
export function queryPay(purchaseId) {
  return request({
    url: '/alipay/query',
    method: 'get',
    params: { purchaseId }
  })
}
