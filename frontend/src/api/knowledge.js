/**
 * 知识 API 模块
 */
import request from '@/utils/request'

/**
 * 获取知识文章列表
 * @param {Object} params - 查询参数 { page, pageSize, category, keyword }
 */
export function getKnowledgeList(params) {
  return request({
    url: '/knowledge/list',
    method: 'get',
    params
  })
}

/**
 * 获取知识文章详情
 * @param {String|Number} knowledgeId - 文章ID
 */
export function getKnowledgeDetail(knowledgeId) {
  return request({
    url: `/knowledge/${knowledgeId}`,
    method: 'get'
  })
}

/**
 * 发布知识文章
 * @param {Object} data - 文章数据
 */
export function publishKnowledge(data) {
  return request({
    url: '/knowledge',
    method: 'post',
    data
  })
}

/**
 * 更新知识文章
 * @param {String|Number} knowledgeId - 文章ID
 * @param {Object} data - 更新数据
 */
export function updateKnowledge(knowledgeId, data) {
  return request({
    url: `/knowledge/${knowledgeId}`,
    method: 'put',
    data
  })
}

/**
 * 删除知识文章
 * @param {String|Number} knowledgeId - 文章ID
 */
export function deleteKnowledge(knowledgeId) {
  return request({
    url: `/knowledge/${knowledgeId}`,
    method: 'delete'
  })
}

/**
 * 获取知识文章评论列表
 * @param {String|Number} knowledgeId - 文章ID
 */
export function getKnowledgeComments(knowledgeId) {
  return request({
    url: `/knowledge/${knowledgeId}/discuss/list`,
    method: 'get'
  })
}

/**
 * 添加知识文章评论
 * @param {String|Number} knowledgeId - 文章ID
 * @param {Object} data - 评论数据
 */
export function addKnowledgeComment(knowledgeId, data) {
  return request({
    url: `/knowledge/${knowledgeId}/discuss`,
    method: 'post',
    data
  })
}

/**
 * 删除知识文章评论
 * @param {String|Number} discussId - 评论ID
 */
export function deleteKnowledgeComment(discussId) {
  return request({
    url: `/knowledge/discuss/${discussId}`,
    method: 'delete'
  })
}
