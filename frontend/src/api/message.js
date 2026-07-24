/**
 * 站内消息 API 模块
 */
import request from '@/utils/request'

/**
 * 分页获取我的消息
 * @param {Object} params - { page, pageSize, isRead? }
 */
export function getMessages(params) {
  return request({
    url: '/message/list',
    method: 'get',
    params
  })
}

/**
 * 获取未读消息数（用于铃铛徽标）
 */
export function getUnreadCount() {
  return request({
    url: '/message/unread/count',
    method: 'get'
  })
}

/**
 * 标记单条为已读
 * @param {Number|String} id
 */
export function markMessageRead(id) {
  return request({
    url: `/message/${id}/read`,
    method: 'put'
  })
}

/**
 * 全部标记已读
 */
export function markAllMessagesRead() {
  return request({
    url: '/message/read/all',
    method: 'put'
  })
}

/**
 * 删除一条消息
 * @param {Number|String} id
 */
export function deleteMessage(id) {
  return request({
    url: `/message/${id}`,
    method: 'delete'
  })
}
