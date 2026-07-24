import request from '@/utils/request'

/**
 * 用户注册
 */
export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

/**
 * 修改用户信息
 */
export function updateUserInfo(data) {
  return request({
    url: '/user/info',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 */
export function updatePassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

/**
 * 提交角色升级申请（农户/买家 → 专家/银行）
 */
export function applyRole(data) {
  return request({
    url: '/user/apply-role',
    method: 'post',
    data
  })
}

/**
 * 查看我的角色申请记录
 */
export function getMyApplications() {
  return request({
    url: '/user/my/applications',
    method: 'get'
  })
}

/**
 * 联系人列表（供联合贷款人等场景选择，已排除自己；仅农户/买家/专家）
 * @param {Object} params - 可选 { role: 'farmer' } 限定角色
 */
export function getContacts(params) {
  return request({
    url: '/user/contacts',
    method: 'get',
    params
  })
}

