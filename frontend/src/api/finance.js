/**
 * 融资 API 模块
 *
 * 端点契约（与后端 FinanceController 对齐）：
 *  - GET  /finance/product/list            公开，分页融资产品
 *  - GET  /finance/product/{productId}     公开，产品详情
 *  - POST /finance/product/publish         银行发布产品（role=bank）
 *  - GET  /finance/apply/list              我的申请（农户）
 *  - GET  /finance/apply/bank              待审批列表（仅 status=0）
 *  - POST /finance/apply                   提交申请（农户）
 *  - PUT  /finance/apply/{financeId}       审批 {status, remark}
 *  - DELETE /finance/apply/{financeId}     撤销申请（物理删除）
 *  - GET  /finance/intention/match         融资意向匹配
 *  银行工作台（需登录）：
 *  - GET  /finance/bank/applications       全状态申请列表（筛选）
 *  - GET  /finance/bank/stats              数据概览聚合统计
 *  - GET  /finance/bank/match              智能匹配（真实申请，按金额/期限/作物）
 *  - PUT  /finance/bank/product/{id}       更新产品（role=bank）
 *  - DELETE /finance/bank/product/{id}     删除产品（role=bank）
 */
import request from '@/utils/request'

/**
 * 获取融资产品列表
 * @param {Object} params - 查询参数 { page, pageSize }
 */
export function getFinanceProducts(params) {
  return request({
    url: '/finance/product/list',
    method: 'get',
    params
  })
}

/**
 * 获取融资产品详情
 * @param {String|Number} productId - 产品ID
 */
export function getFinanceProductDetail(productId) {
  return request({
    url: `/finance/product/${productId}`,
    method: 'get'
  })
}

/**
 * 发布融资产品（银行）
 * @param {Object} data - { bankName, introduce, bankPhone, money, rate, repayment }
 */
export function publishFinanceProduct(data) {
  return request({
    url: '/finance/product/publish',
    method: 'post',
    data
  })
}

/**
 * 申请融资
 * @param {Object} data - 申请数据
 */
export function applyFinance(data) {
  return request({
    url: '/finance/apply',
    method: 'post',
    data
  })
}

/**
 * 撤销融资申请（物理删除）
 * @param {String|Number} financeId - 申请ID
 */
export function cancelFinance(financeId) {
  return request({
    url: `/finance/apply/${financeId}`,
    method: 'delete'
  })
}

/**
 * 获取我的融资申请列表
 * @param {Object} params - 查询参数
 */
export function getMyFinanceApplications(params) {
  return request({
    url: '/finance/apply/list',
    method: 'get',
    params
  })
}

/**
 * 获取待审批列表（仅 status=0）
 * @param {Object} params - 查询参数
 */
export function getBankApprovalList(params) {
  return request({
    url: '/finance/apply/bank',
    method: 'get',
    params
  })
}

/**
 * 审核融资申请（通过/拒绝）
 * @param {String|Number} financeId - 申请ID
 * @param {Object} data - { status: 1已通过|2已拒绝, remark }
 */
export function approveFinanceApplication(financeId, data) {
  return request({
    url: `/finance/apply/${financeId}`,
    method: 'put',
    data
  })
}

/**
 * 获取融资意向匹配列表
 * @param {Object} params - { minAmount, maxAmount, item, page, pageSize }
 */
export function getFinanceIntentionMatch(params) {
  return request({
    url: '/finance/intention/match',
    method: 'get',
    params
  })
}

// ==================== 银行工作台专用 ====================

/**
 * 银行工作台 - 融资申请列表（全状态，支持筛选）
 * @param {Object} params - { status, keyword, startDate, endDate, page, pageSize }
 */
export function getBankApplications(params) {
  return request({
    url: '/finance/bank/applications',
    method: 'get',
    params
  })
}

/**
 * 银行工作台 - 数据概览聚合统计
 */
export function getBankStats() {
  return request({
    url: '/finance/bank/stats',
    method: 'get'
  })
}

/**
 * 银行工作台 - 智能匹配（基于真实融资申请 tb_finance，status=申请中）
 * 与 getFinanceIntentionMatch（融资意向表，无数据来源）不同，此处返回真实申请。
 * @param {Object} params - { minMoney, maxMoney, repayment, keyword, page, pageSize }（金额单位：元）
 */
export function getBankMatch(params) {
  return request({
    url: '/finance/bank/match',
    method: 'get',
    params
  })
}

/**
 * 银行工作台 - 本行全部融资产品（含已暂停），供产品管理页展示
 * @param {Object} params - { page, pageSize }
 */
export function getBankProducts(params) {
  return request({
    url: '/finance/bank/product/list',
    method: 'get',
    params
  })
}

/**
 * 银行工作台 - 更新融资产品（核心条款发布后锁定，仅可改介绍与电话）
 * @param {String|Number} productId - 产品ID
 * @param {Object} data - { introduce, bankPhone }
 */
export function updateBankProduct(productId, data) {
  return request({
    url: `/finance/bank/product/${productId}`,
    method: 'put',
    data
  })
}

/**
 * 银行工作台 - 切换融资产品上下架状态（0在售 / 1暂停供应）。产品不支持删除。
 * @param {String|Number} productId - 产品ID
 * @param {Number} status - 0在售 1暂停供应
 */
export function setBankProductStatus(productId, status) {
  return request({
    url: `/finance/bank/product/${productId}/status`,
    method: 'put',
    params: { status }
  })
}

// ==================== 联合贷款人邀请 ====================

/**
 * 我收到的联合贷款邀请（作为被邀请人）
 */
export function getMyJointInvitations() {
  return request({
    url: '/joint-invitation/mine',
    method: 'get'
  })
}

/**
 * 某笔融资申请下的全部邀请（农户查看各联合人确认状态）
 * @param {String|Number} financeId - 融资申请ID
 */
export function getFinanceJointInvitations(financeId) {
  return request({
    url: `/joint-invitation/finance/${financeId}`,
    method: 'get'
  })
}

/**
 * 同意联合贷款邀请
 * @param {String|Number} id - 邀请ID
 */
export function acceptJointInvitation(id) {
  return request({
    url: `/joint-invitation/${id}/accept`,
    method: 'post'
  })
}

/**
 * 拒绝联合贷款邀请
 * @param {String|Number} id - 邀请ID
 */
export function declineJointInvitation(id) {
  return request({
    url: `/joint-invitation/${id}/decline`,
    method: 'post'
  })
}

// ==================== 还款计划 ====================

/**
 * 某笔融资的还款计划（按期数升序，含逾期动态判定）
 * @param {String|Number} financeId - 融资申请ID
 */
export function getFinanceRepayments(financeId) {
  return request({
    url: `/repayment/finance/${financeId}`,
    method: 'get'
  })
}

/**
 * 农户提交某期还款（填流水号 + 凭证）→ 进入「待确认」
 * @param {String|Number} repaymentId - 还款记录ID
 * @param {Object} data - { transactionNo, payProof }
 */
export function submitRepayment(repaymentId, data) {
  return request({
    url: `/repayment/${repaymentId}/pay`,
    method: 'post',
    data
  })
}

/**
 * 银行还款审核列表（默认「待确认」队列）
 * @param {Object} params - { status, page, pageSize }
 */
export function getBankRepayments(params) {
  return request({
    url: '/repayment/bank/list',
    method: 'get',
    params
  })
}

/**
 * 银行确认还款（置已还，农户信用 +1）
 * @param {String|Number} repaymentId - 还款记录ID
 */
export function confirmRepayment(repaymentId) {
  return request({
    url: `/repayment/bank/${repaymentId}/confirm`,
    method: 'post'
  })
}

/**
 * 银行驳回还款（置已驳回，通知农户重交）
 * @param {String|Number} repaymentId - 还款记录ID
 * @param {Object} data - { reason }
 */
export function rejectRepayment(repaymentId, data) {
  return request({
    url: `/repayment/bank/${repaymentId}/reject`,
    method: 'post',
    data
  })
}
