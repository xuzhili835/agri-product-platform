/**
 * API接口模块
 * 按照模块分类导出所有API接口
 */

import request from '@/utils/request'

/**
 * 用户模块
 */
export const userApi = {
  // 登录
  login(data) {
    return request({
      url: '/user/login',
      method: 'POST',
      data
    })
  },
  // 注册
  register(data) {
    return request({
      url: '/user/register',
      method: 'POST',
      data
    })
  },
  // 获取信息
  getInfo() {
    return request({
      url: '/user/info',
      method: 'GET'
    })
  },
  // 修改信息
  updateInfo(data) {
    return request({
      url: '/user/info',
      method: 'PUT',
      data
    })
  },
  // 修改密码
  updatePassword(data) {
    return request({
      url: '/user/password',
      method: 'PUT',
      data
    })
  }
}

/**
 * 地址模块
 */
export const addressApi = {
  // 添加地址
  add(data) {
    return request({
      url: '/address',
      method: 'POST',
      data
    })
  },
  // 地址列表
  list() {
    return request({
      url: '/address/list',
      method: 'GET'
    })
  },
  // 修改地址
  update(id, data) {
    return request({
      url: `/address/${id}`,
      method: 'PUT',
      data
    })
  },
  // 删除地址
  delete(id) {
    return request({
      url: `/address/${id}`,
      method: 'DELETE'
    })
  }
}

/**
 * 商品模块
 */
export const productApi = {
  // 发布商品
  publish(data) {
    return request({
      url: '/product',
      method: 'POST',
      data
    })
  },
  // 分页查询
  getPage(params) {
    return request({
      url: '/product/page',
      method: 'GET',
      params
    })
  },
  // 商品详情
  getDetail(orderId) {
    return request({
      url: `/product/${orderId}`,
      method: 'GET'
    })
  },
  // 修改商品
  update(orderId, data) {
    return request({
      url: `/product/${orderId}`,
      method: 'PUT',
      data
    })
  },
  // 下架商品
  delete(orderId) {
    return request({
      url: `/product/${orderId}`,
      method: 'DELETE'
    })
  },
  // 我的发布
  getMyProducts() {
    return request({
      url: '/product/user',
      method: 'GET'
    })
  }
}

/**
 * 购物车模块
 */
export const cartApi = {
  // 加入购物车
  add(data) {
    return request({
      url: '/cart',
      method: 'POST',
      data
    })
  },
  // 购物车列表
  list() {
    return request({
      url: '/cart/list',
      method: 'GET'
    })
  },
  // 修改数量
  updateQuantity(shoppingId, data) {
    return request({
      url: `/cart/${shoppingId}`,
      method: 'PUT',
      data
    })
  },
  // 删除
  delete(shoppingId) {
    return request({
      url: `/cart/${shoppingId}`,
      method: 'DELETE'
    })
  }
}

/**
 * 订单模块
 */
export const orderApi = {
  // 提交订单
  submit(data) {
    return request({
      url: '/purchase',
      method: 'POST',
      data
    })
  },
  // 我的订单
  list() {
    return request({
      url: '/purchase/list',
      method: 'GET'
    })
  },
  // 订单详情
  getDetail(purchaseId) {
    return request({
      url: `/purchase/${purchaseId}`,
      method: 'GET'
    })
  },
  // 更新状态
  updateStatus(purchaseId, data) {
    return request({
      url: `/purchase/${purchaseId}/status`,
      method: 'PUT',
      data
    })
  },
  // 取消订单
  cancel(purchaseId) {
    return request({
      url: `/purchase/${purchaseId}`,
      method: 'DELETE'
    })
  }
}

/**
 * 融资模块
 */
export const financeApi = {
  // 融资产品列表
  productList() {
    return request({
      url: '/finance/product/list',
      method: 'GET'
    })
  },
  // 产品详情
  getProductDetail(productId) {
    return request({
      url: `/finance/product/${productId}`,
      method: 'GET'
    })
  },
  // 申请融资
  apply(data) {
    return request({
      url: '/finance/apply',
      method: 'POST',
      data
    })
  },
  // 我的申请
  myApplyList() {
    return request({
      url: '/finance/apply/list',
      method: 'GET'
    })
  },
  // 待审批列表（银行）
  bankApplyList() {
    return request({
      url: '/finance/apply/bank',
      method: 'GET'
    })
  },
  // 审批（银行）
  approve(financeId, data) {
    return request({
      url: `/finance/apply/${financeId}`,
      method: 'PUT',
      data
    })
  },
  // 提交意向
  submitIntention(data) {
    return request({
      url: '/finance/intention',
      method: 'POST',
      data
    })
  },
  // 智能匹配（银行）
  matchIntention() {
    return request({
      url: '/finance/intention/match',
      method: 'GET'
    })
  }
}

/**
 * 知识模块
 */
export const knowledgeApi = {
  // 知识列表
  list() {
    return request({
      url: '/knowledge/list',
      method: 'GET'
    })
  },
  // 知识详情
  getDetail(knowledgeId) {
    return request({
      url: `/knowledge/${knowledgeId}`,
      method: 'GET'
    })
  },
  // 发布知识
  publish(data) {
    return request({
      url: '/knowledge',
      method: 'POST',
      data
    })
  },
  // 修改知识
  update(knowledgeId, data) {
    return request({
      url: `/knowledge/${knowledgeId}`,
      method: 'PUT',
      data
    })
  },
  // 删除知识
  delete(knowledgeId) {
    return request({
      url: `/knowledge/${knowledgeId}`,
      method: 'DELETE'
    })
  },
  // 评论
  comment(knowledgeId, data) {
    return request({
      url: `/knowledge/${knowledgeId}/discuss`,
      method: 'POST',
      data
    })
  },
  // 评论列表
  commentList(knowledgeId) {
    return request({
      url: `/knowledge/${knowledgeId}/discuss/list`,
      method: 'GET'
    })
  },
  // 删除评论
  deleteComment(discussId) {
    return request({
      url: `/discuss/${discussId}`,
      method: 'DELETE'
    })
  }
}

/**
 * 问答模块
 */
export const questionApi = {
  // 提问
  ask(data) {
    return request({
      url: '/question',
      method: 'POST',
      data
    })
  },
  // 问答列表
  list() {
    return request({
      url: '/question/list',
      method: 'GET'
    })
  },
  // 详情
  getDetail(id) {
    return request({
      url: `/question/${id}`,
      method: 'GET'
    })
  },
  // 回答
  answer(id, data) {
    return request({
      url: `/question/${id}/answer`,
      method: 'PUT',
      data
    })
  },
  // 删除
  delete(id) {
    return request({
      url: `/question/${id}`,
      method: 'DELETE'
    })
  }
}

/**
 * 专家模块
 */
export const expertApi = {
  // 专家列表
  list() {
    return request({
      url: '/expert/list',
      method: 'GET'
    })
  },
  // 专家详情
  getDetail(userName) {
    return request({
      url: `/expert/${userName}`,
      method: 'GET'
    })
  },
  // 预约
  reserve(data) {
    return request({
      url: '/reserve',
      method: 'POST',
      data
    })
  },
  // 我的预约
  myReserveList() {
    return request({
      url: '/reserve/list',
      method: 'GET'
    })
  },
  // 专家的预约
  expertReserveList() {
    return request({
      url: '/reserve/expert/list',
      method: 'GET'
    })
  },
  // 处理预约
  handleReserve(id, data) {
    return request({
      url: `/reserve/${id}/status`,
      method: 'PUT',
      data
    })
  }
}

/**
 * 轮播图和首页模块
 */
export const homeApi = {
  // 首页聚合数据
  getHomeData() {
    return request({
      url: '/home',
      method: 'GET'
    })
  },
  // 轮播图列表
  bannerList() {
    return request({
      url: '/banner/list',
      method: 'GET'
    })
  }
}

/**
 * 后台管理模块
 */
export const adminApi = {
  // 轮播图管理
  addBanner(data) {
    return request({
      url: '/admin/banner',
      method: 'POST',
      data
    })
  },
  updateBanner(bannerId, data) {
    return request({
      url: `/admin/banner/${bannerId}`,
      method: 'PUT',
      data
    })
  },
  deleteBanner(bannerId) {
    return request({
      url: `/admin/banner/${bannerId}`,
      method: 'DELETE'
    })
  },
  // 用户管理
  getUserList() {
    return request({
      url: '/admin/users',
      method: 'GET'
    })
  },
  updateUser(userName, data) {
    return request({
      url: `/admin/user/${userName}`,
      method: 'PUT',
      data
    })
  },
  deleteUser(userName) {
    return request({
      url: `/admin/user/${userName}`,
      method: 'DELETE'
    })
  },
  // 商品管理
  getProductList() {
    return request({
      url: '/admin/products',
      method: 'GET'
    })
  },
  updateProduct(orderId, data) {
    return request({
      url: `/admin/product/${orderId}`,
      method: 'PUT',
      data
    })
  },
  deleteProduct(orderId) {
    return request({
      url: `/admin/product/${orderId}`,
      method: 'DELETE'
    })
  },
  // 融资管理
  getFinanceList() {
    return request({
      url: '/admin/finances',
      method: 'GET'
    })
  },
  updateFinance(financeId, data) {
    return request({
      url: `/admin/finance/${financeId}`,
      method: 'PUT',
      data
    })
  },
  // 专家管理
  getExpertList() {
    return request({
      url: '/admin/experts',
      method: 'GET'
    })
  },
  updateExpert(userName, data) {
    return request({
      url: `/admin/expert/${userName}`,
      method: 'PUT',
      data
    })
  },
  deleteExpert(userName) {
    return request({
      url: `/admin/expert/${userName}`,
      method: 'DELETE'
    })
  }
}
