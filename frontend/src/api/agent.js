/**
 * AI 智能助手 API
 */
import request from '@/utils/request'

/**
 * 获取助手状态(总开关 + 当前角色)
 * @returns {Promise} { enabled: boolean, role: string }
 */
export function getAgentStatus() {
  return request({
    url: '/agent/status',
    method: 'get'
  })
}

/**
 * 发送对话消息
 * @param {Object} data - { message: string, sessionId?: string }
 * @returns {Promise} { sessionId, reply, pendingId, needsConfirm }
 */
export function agentChat(data) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data,
    timeout: 60000  // LLM 调用慢,覆盖全局 10s 默认
  })
}

/**
 * 确认/取消写操作
 * @param {Object} data - { pendingId: string, accept: boolean, sessionId: string }
 * @returns {Promise} { reply: string }
 */
export function agentConfirm(data) {
  return request({
    url: '/agent/confirm',
    method: 'post',
    data,
    timeout: 60000
  })
}

/**
 * 获取会话历史消息
 * @param {string} sessionId
 * @returns {Promise} 消息列表
 */
export function getAgentHistory(sessionId) {
  return request({
    url: `/agent/history/${sessionId}`,
    method: 'get'
  })
}

/**
 * 管理员切换助手总开关
 * @param {boolean} enabled
 * @returns {Promise} { enabled: boolean }
 */
export function toggleAgent(enabled) {
  return request({
    url: '/agent/admin/toggle',
    method: 'post',
    params: { enabled }
  })
}
