<template>
  <div class="login-page">
    <!-- 品牌展示区 -->
    <div class="brand-section">
      <div class="brand-content">
        <!-- 返回首页按钮 - 精致的幽灵按钮 -->
        <button class="back-home-btn" @click="goTo('/home')">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 8H2M2 8l5-5M2 8l5 5"/>
          </svg>
          返回首页
        </button>

        <!-- 精简的品牌图标 -->
        <div class="brand-logo">
          <svg viewBox="0 0 80 80" fill="none" stroke="currentColor" stroke-width="1.5">
            <!-- 主茎 -->
            <path d="M40 12v40" stroke-linecap="round"/>
            <!-- 根部 -->
            <path d="M40 52l-6 6M40 52l6 6M40 48l-8 4M40 48l8 4" stroke-linecap="round" opacity="0.6"/>
            <!-- 左侧叶片 -->
            <path d="M40 20c-8 4-16 2-20-4" stroke-linecap="round"/>
            <path d="M40 28c-10 4-18 0-22-8" stroke-linecap="round"/>
            <path d="M40 36c-8 2-14 0-16-6" stroke-linecap="round"/>
            <!-- 右侧叶片 -->
            <path d="M40 20c8 4 16 2 20-4" stroke-linecap="round"/>
            <path d="M40 28c10 4 18 0 22-8" stroke-linecap="round"/>
            <path d="M40 36c8 2 14 0 16-6" stroke-linecap="round"/>
            <!-- 顶端装饰 -->
            <circle cx="40" cy="10" r="3" fill="currentColor" stroke="none"/>
          </svg>
        </div>

        <h1 class="brand-title">欢迎回来</h1>
        <p class="brand-subtitle">继续您的农业数字化之旅</p>

        <!-- 数据统计 - 展示平台价值 -->
        <div class="platform-stats">
          <div class="stat-item">
            <div class="stat-number">10,000+</div>
            <div class="stat-label">注册农户</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">¥500M</div>
            <div class="stat-label">融资金额</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-number">500+</div>
            <div class="stat-label">农业专家</div>
          </div>
        </div>

        <!-- 功能预览 -->
        <div class="feature-preview">
          <div class="feature-item" v-for="feature in features" :key="feature.title">
            <div class="feature-icon" :style="{ color: feature.color }">
              <!-- 快速融资图标 -->
              <svg v-if="feature.title === '快速融资'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
                <circle cx="12" cy="12" r="2" fill="currentColor"/>
              </svg>
              <!-- 农产品交易图标 -->
              <svg v-else-if="feature.title === '农产品交易'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="9" cy="21" r="1"/>
                <circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/>
              </svg>
              <!-- 专家咨询图标 -->
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="8" r="3"/>
                <path d="M8 21v-5a4 4 0 014-4M8 21h8M12 12v5M8 10l2-2M16 10l-2-2"/>
                <path d="M12 16a3 3 0 00-3 3v2h6v-2a3 3 0 00-3-3z"/>
              </svg>
            </div>
            <div class="feature-info">
              <div class="feature-title">{{ feature.title }}</div>
              <div class="feature-desc">{{ feature.desc }}</div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- 登录表单区 -->
    <div class="form-section">
      <div class="form-card">
        <div class="form-header">
          <h2 class="form-title">账号登录</h2>
          <p class="form-subtitle">选择您的角色并登录</p>
        </div>

        <!-- 角色选择 - 下拉框 -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
        >
          <el-form-item prop="role">
            <label class="form-label">登录角色</label>
            <el-select
              v-model="loginForm.role"
              placeholder="请选择您的角色"
              size="large"
              class="form-input role-select"
            >
              <el-option label="农户" value="farmer">
                <div class="role-option">
                  <span class="role-option-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="12" cy="8" r="3"/>
                      <path d="M8 21v-5a4 4 0 014-4M8 21h8M12 12v5M8 10l2-2M16 10l-2-2"/>
                    </svg>
                  </span>
                  <span class="role-option-text">农户</span>
                  <span class="role-option-desc">销售农产品，申请融资</span>
                </div>
              </el-option>
              <el-option label="买家" value="buyer">
                <div class="role-option">
                  <span class="role-option-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="9" cy="21" r="1"/>
                      <circle cx="20" cy="21" r="1"/>
                      <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/>
                    </svg>
                  </span>
                  <span class="role-option-text">买家</span>
                  <span class="role-option-desc">采购优质农产品</span>
                </div>
              </el-option>
              <el-option label="专家" value="expert">
                <div class="role-option">
                  <span class="role-option-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M22 10v6M2 10l2-4h16l2 4M12 2v3M12 10a3 3 0 100 6 3 3 0 000-6z"/>
                      <path d="M12 16c-3 0-5 1.5-6 3v2h12v-2c-1-1.5-3-3-6-3z"/>
                    </svg>
                  </span>
                  <span class="role-option-text">专家</span>
                  <span class="role-option-desc">发布知识，解答问题</span>
                </div>
              </el-option>
              <el-option label="银行" value="bank">
                <div class="role-option">
                  <span class="role-option-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M3 21h18M5 21V7l8-4 8 4v14M8 21v-2a2 2 0 012-2h4a2 2 0 012 2v2"/>
                      <path d="M10 9l4 4"/>
                      <circle cx="10" cy="9" r="2"/>
                      <path d="M14 13l4 4"/>
                    </svg>
                  </span>
                  <span class="role-option-text">银行</span>
                  <span class="role-option-desc">审批融资，智能匹配</span>
                </div>
              </el-option>
              <el-option label="管理员" value="admin">
                <div class="role-option">
                  <span class="role-option-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="12" cy="12" r="3"/>
                      <path d="M12 1v6m0 6v6"/>
                      <path d="m5 12 6-6m0 12 6-6"/>
                      <circle cx="12" cy="12" r="8"/>
                    </svg>
                  </span>
                  <span class="role-option-text">管理员</span>
                  <span class="role-option-desc">平台管理与维护</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item prop="userName">
            <label class="form-label">用户名</label>
            <el-input
              v-model="loginForm.userName"
              placeholder="请输入用户名"
              size="large"
              class="form-input"
              :prefix-icon="'User'"
            />
          </el-form-item>

          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              class="form-input"
              :prefix-icon="'Lock'"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe" class="remember-checkbox">
              记住我
            </el-checkbox>
            <a href="#" class="forgot-link">忘记密码？</a>
          </div>

          <el-form-item>
            <button
              type="button"
              class="btn-submit"
              :disabled="loading"
              @click="handleLogin"
            >
              <span v-if="!loading">登录</span>
              <span v-else class="loading-text">
                <svg class="loading-icon" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" opacity="0.3"/>
                  <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
                </svg>
                登录中...
              </span>
            </button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span class="footer-text">还没有账号？</span>
          <router-link to="/register" class="footer-link">立即注册</router-link>
        </div>

        <!-- 平台保障 -->
        <div class="platform-assurance">
          <div class="assurance-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
            <span>数据安全加密</span>
          </div>
          <div class="assurance-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 6v6l4 2"/>
            </svg>
            <span>24小时客服</span>
          </div>
          <div class="assurance-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <path d="M22 4L12 14.01l-3-3"/>
            </svg>
            <span>实名认证保障</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  userName: '',
  password: '',
  role: 'farmer' // 默认农户角色
})

// 角色选项 - SVG 图标组件
const RoleIcons = {
  farmer: () => '🌾',
  buyer: () => '🛒',
  expert: () => '🎓',
  bank: () => '🏦'
}

const roles = [
  {
    label: '农户',
    value: 'farmer',
    icon: 'User'
  },
  {
    label: '买家',
    value: 'buyer',
    icon: 'ShoppingCart'
  },
  {
    label: '专家',
    value: 'expert',
    icon: 'User'
  },
  {
    label: '银行',
    value: 'bank',
    icon: 'Office'
  },
  {
    label: '管理员',
    value: 'admin',
    icon: 'Setting'
  }
]

// 功能预览数据
const features = [
  {
    icon: 'TrendingUp',
    title: '快速融资',
    desc: '低利率审批快',
    color: '#C9A661'
  },
  {
    icon: 'ShoppingCart',
    title: '农产品交易',
    desc: '直接对接买家',
    color: '#4A7C59'
  },
  {
    icon: 'UserExpert',
    title: '专家咨询',
    desc: '专业技术支持',
    color: '#B85C38'
  }
]

const selectRole = (role) => {
  loginForm.role = role
}

// 页面跳转
const goTo = (path) => {
  router.push(path)
}

const rules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['blur', 'change'] },
    { min: 6, message: '密码长度不能少于6位', trigger: ['blur', 'change'] }
  ]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请填写完整且正确的登录信息')
    return
  }

  loading.value = true

  try {
    const result = await userStore.loginUser({
      userName: loginForm.userName,
      password: loginForm.password,
      role: loginForm.role
    })

    if (result.success) {
      ElMessage.success('登录成功')
      // 使用服务器返回的角色跳转到对应的Dashboard
      const actualRole = userStore.role
      const roleMap = {
        farmer: '/farmer/dashboard',
        buyer: '/buyer/dashboard',
        expert: '/expert/dashboard',
        bank: '/bank/dashboard',
        admin: '/admin/dashboard'
      }
      router.push(roleMap[actualRole] || '/home')
    } else {
      ElMessage.error(result.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 组件挂载完成
onMounted(() => {
  // 可以在这里添加其他初始化逻辑
})
</script>

<style scoped>
/* ===== 全局变量 ===== */
.login-page {
  --color-primary: #2D5A3D;
  --color-primary-light: #4A7C59;
  --color-primary-dark: #1F4229;
  --color-secondary: #C9A661;
  --color-accent: #B85C38;
  --color-bg: #F7F5F0;
  --color-text: #1F2923;
  --color-text-light: #6B7280;
  --color-border: #E5E0D8;
  --color-white: #FFFFFF;
  --font-display: 'Noto Serif SC', serif;
  --font-body: 'Noto Sans SC', sans-serif;
}

/* ===== 页面布局 ===== */
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

/* ===== 品牌区域 ===== */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  position: relative;
  overflow: hidden;
}

.brand-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 30% 20%, rgba(201, 166, 97, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 70% 80%, rgba(255, 255, 255, 0.08) 0%, transparent 50%);
}

.brand-content {
  position: relative;
  text-align: center;
  color: var(--color-white);
  z-index: 1;
  max-width: 480px;
}

/* 品牌图标 - 简洁的麦穗 */
.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.brand-logo svg {
  width: 48px;
  height: 48px;
  color: rgba(255, 255, 255, 0.85);
  animation: subtleFloat 3s ease-in-out infinite;
}

@keyframes subtleFloat {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

/* 返回首页按钮 - 精致幽灵按钮 */
.back-home-btn {
  position: fixed;
  top: 24px;
  left: 24px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(8px);
}

.back-home-btn:hover {
  color: rgba(255, 255, 255, 0.95);
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

.back-home-btn svg {
  width: 16px;
  height: 16px;
}

.brand-title {
  font-family: var(--font-display);
  font-size: 48px;
  font-weight: 700;
  margin: 0 0 16px;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  font-size: 18px;
  opacity: 0.9;
  margin: 0 0 48px;
}

/* 平台统计 */
.platform-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 48px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.stat-item {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  opacity: 0.8;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
}

/* 功能预览 */
.feature-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  text-align: left;
  transition: background 0.2s;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  flex-shrink: 0;
}

.feature-icon svg {
  width: 22px;
  height: 22px;
}

.feature-info {
  flex: 1;
}

.feature-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 2px;
}

.feature-desc {
  font-size: 13px;
  opacity: 0.75;
}

/* ===== 表单区域 ===== */
.form-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--color-bg);
  overflow-y: auto;
}

.form-card {
  width: 100%;
  max-width: 440px;
  padding: 40px;
  background: var(--color-white);
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(31, 41, 35, 0.08);
}

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-title {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 12px;
}

.form-subtitle {
  font-size: 15px;
  color: var(--color-text-light);
  margin: 0;
}

/* 角色选择下拉框 */
.role-select :deep(.el-select__wrapper) {
  padding: 12px 16px;
  border-radius: 10px;
  border: 1.5px solid var(--color-border);
  box-shadow: none;
  transition: all 0.2s;
}

.role-select :deep(.el-select__wrapper:hover) {
  border-color: var(--color-primary-light);
}

.role-select :deep(.el-select__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(45, 90, 61, 0.1);
}

/* 角色选项样式 */
.role-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.role-option-icon {
  font-size: 20px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
}

.role-option-icon svg {
  width: 20px;
  height: 20px;
}

.role-option-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
}

.role-option-desc {
  font-size: 12px;
  color: var(--color-text-light);
  margin-left: auto;
}

/* ===== 表单样式 ===== */
.login-form {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 8px;
}

:deep(.form-input .el-input__wrapper) {
  padding: 12px 16px;
  border-radius: 10px;
  border: 1.5px solid var(--color-border);
  box-shadow: none;
  transition: all 0.2s;
}

:deep(.form-input .el-input__wrapper:hover) {
  border-color: var(--color-primary-light);
}

:deep(.form-input .el-input__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(45, 90, 61, 0.1);
}

:deep(.form-input .el-input__inner) {
  font-size: 15px;
  color: var(--color-text);
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__error) {
  font-size: 13px;
  color: var(--color-accent);
}

/* 表单选项 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.remember-checkbox {
  font-size: 14px;
  color: var(--color-text-light);
}

:deep(.remember-checkbox .el-checkbox__label) {
  color: var(--color-text-light);
}

.forgot-link {
  font-size: 14px;
  color: var(--color-primary);
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}

/* 提交按钮 */
.btn-submit {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 24px;
  font-size: 16px;
  font-weight: 500;
  color: var(--color-white);
  background: var(--color-primary);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.btn-submit:hover:not(:disabled) {
  background: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(45, 90, 61, 0.3);
}

.btn-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-icon {
  width: 18px;
  height: 18px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 表单底部 ===== */
.form-footer {
  text-align: center;
  margin-bottom: 24px;
}

.footer-text {
  font-size: 14px;
  color: var(--color-text-light);
}

.footer-link {
  margin-left: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-primary);
  text-decoration: none;
  transition: color 0.2s;
}

.footer-link:hover {
  color: var(--color-primary-dark);
}

/* 平台保障 */
.platform-assurance {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 20px;
  background: var(--color-bg);
  border-radius: 12px;
  gap: 12px;
}

.assurance-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.assurance-item svg {
  width: 20px;
  height: 20px;
  color: var(--color-primary-light);
}

.assurance-item span {
  font-size: 12px;
  color: var(--color-text-light);
  text-align: center;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .brand-section {
    display: none;
  }

  .form-section {
    padding: 24px;
  }

  .form-card {
    padding: 28px;
  }
}

@media (max-width: 480px) {
  .form-card {
    padding: 20px;
  }

  .form-title {
    font-size: 26px;
  }

  .back-home-btn {
    padding: 8px 14px;
    font-size: 12px;
  }

  .platform-assurance {
    flex-wrap: wrap;
  }

  .assurance-item {
    flex: 1 1 30%;
  }

  .role-option-text {
    font-size: 14px;
  }

  .role-option-desc {
    display: none;
  }
}
</style>
