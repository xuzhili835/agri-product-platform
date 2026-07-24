/**
 * 用户状态管理
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, register } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const role = ref(localStorage.getItem('role') || '')

  // 登录
  const loginUser = async (loginForm) => {
    try {
      const res = await login(loginForm)
      if (res.code === 200) {
        token.value = res.data.token
        userInfo.value = res.data.user
        role.value = res.data.user.role
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('userInfo', JSON.stringify(res.data.user))
        localStorage.setItem('role', res.data.user.role)
        return { success: true }
      } else {
        return { success: false, message: res.message }
      }
    } catch (error) {
      return { success: false, message: error.message || '登录失败' }
    }
  }

  // 注册
  const registerUser = async (registerForm) => {
    try {
      const res = await register(registerForm)
      if (res.code === 200) {
        // 后端 UserController 用 Result.success(message) 返回，提示语落在 data 字段
        // （专家/银行为「申请已提交…」，农户/买家为「注册成功」）
        return { success: true, message: res.data || res.message }
      } else {
        return { success: false, message: res.message }
      }
    } catch (error) {
      return { success: false, message: error.message || '注册失败' }
    }
  }

  // 设置token
  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info
    role.value = info.role
    localStorage.setItem('userInfo', JSON.stringify(info))
    localStorage.setItem('role', info.role)
  }

  // 设置角色
  const setRole = (newRole) => {
    role.value = newRole
    localStorage.setItem('role', newRole)
  }

  // 登出
  const logout = () => {
    token.value = ''
    userInfo.value = null
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('role')
  }

  // 是否已登录
  const isLoggedIn = () => {
    return !!token.value
  }

  // 检查角色权限
  const hasRole = (allowedRoles) => {
    if (!role.value) return false
    if (Array.isArray(allowedRoles)) {
      return allowedRoles.includes(role.value)
    }
    return role.value === allowedRoles
  }

  // 获取用户显示名称
  const displayName = () => {
    if (!userInfo.value) return ''
    return userInfo.value.realName || userInfo.value.userName
  }

  // 获取用户头像
  const userAvatar = () => {
    return userInfo.value?.avatar || ''
  }

  // 获取Dashboard路径
  const getDashboardPath = () => {
    if (!userInfo.value) return '/login'
    const roleMap = {
      farmer: '/farmer/dashboard',
      buyer: '/buyer/dashboard',
      expert: '/expert/dashboard',
      bank: '/bank/dashboard',
      admin: '/admin/dashboard'
    }
    return roleMap[role.value] || '/'
  }

  return {
    token,
    userInfo,
    role,
    loginUser,
    registerUser,
    setToken,
    setUserInfo,
    setRole,
    logout,
    isLoggedIn,
    hasRole,
    displayName,
    userAvatar,
    getDashboardPath
  }
})
