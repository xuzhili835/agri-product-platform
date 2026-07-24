/**
 * 问答 API 模块
 */
import request from '@/utils/request'

/**
 * 获取问题列表
 * @param {Object} params - 查询参数 { page, pageSize, status, keyword }
 */
export function getQuestionList(params) {
  return request({
    url: '/question/list',
    method: 'get',
    params
  })
}

/**
 * 获取问题详情
 * @param {String|Number} questionId - 问题ID
 */
export function getQuestionDetail(questionId) {
  return request({
    url: `/question/${questionId}`,
    method: 'get'
  })
}

/**
 * 提交问题
 * @param {Object} data - 问题数据
 */
export function askQuestion(data) {
  return request({
    url: '/question',
    method: 'post',
    data
  })
}

/**
 * 回答问题
 * @param {String|Number} questionId - 问题ID
 * @param {Object} data - 回答数据
 */
export function answerQuestion(questionId, data) {
  return request({
    url: `/question/${questionId}/answer`,
    method: 'put',
    data
  })
}

/**
 * 关闭问题
 * @param {String|Number} questionId - 问题ID
 */
export function closeQuestion(questionId) {
  return request({
    url: `/question/${questionId}/close`,
    method: 'put'
  })
}

/**
 * 删除问题
 * @param {String|Number} questionId - 问题ID
 */
export function deleteQuestion(questionId) {
  return request({
    url: `/question/${questionId}`,
    method: 'delete'
  })
}

/**
 * 获取追问回复列表（多轮对话，按时间正序）
 * @param {String|Number} questionId - 问题ID
 */
export function getQuestionReplies(questionId) {
  return request({
    url: `/question/${questionId}/reply/list`,
    method: 'get'
  })
}

/**
 * 发布追问/回答（多轮对话）
 * @param {String|Number} questionId - 问题ID
 * @param {Object} data - { content }
 */
export function addQuestionReply(questionId, data) {
  return request({
    url: `/question/${questionId}/reply`,
    method: 'post',
    data
  })
}
