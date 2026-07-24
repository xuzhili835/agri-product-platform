<template>
  <div class="register-page">
    <!-- 品牌展示区 -->
    <div class="brand-section">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2L12 12M12 12L8 8M12 12L16 8M12 12L12 22M12 22L8 18M12 22L16 18" />
            <circle cx="12" cy="3" r="1.5" fill="currentColor"/>
          </svg>
        </div>

        <!-- 返回首页按钮 -->
        <button class="back-home-btn" @click="goTo('/home')">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 8H2M2 8l5-5M2 8l5 5"/>
          </svg>
          返回首页
        </button>
        <h1 class="brand-title">加入农融汇</h1>
        <p class="brand-subtitle">开启您的农业数字化之旅</p>

        <div class="brand-benefits">
          <div class="benefit-card">
            <div class="benefit-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
              </svg>
            </div>
            <h3 class="benefit-title">便捷融资</h3>
            <p class="benefit-desc">快速审批，灵活还款</p>
          </div>

          <div class="benefit-card">
            <div class="benefit-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="8" r="4"/>
                <path d="M6 21v-2a4 4 0 014-4h4a4 4 0 014 4v2"/>
              </svg>
            </div>
            <h3 class="benefit-title">专家指导</h3>
            <p class="benefit-desc">专业农业技术支持</p>
          </div>

          <div class="benefit-card">
            <div class="benefit-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M20 9v11a2 2 0 01-2 2H6a2 2 0 01-2-2V9"/>
                <path d="M9 22V12h6v10M2 10.6L12 2l10 8.6"/>
              </svg>
            </div>
            <h3 class="benefit-title">直达市场</h3>
            <p class="benefit-desc">农户与买家直接对接</p>
          </div>
        </div>

      </div>
    </div>

    <!-- 注册表单区 -->
    <div class="form-section">
      <div class="form-card">
        <div class="form-header">
          <h2 class="form-title">创建账号</h2>
          <p class="form-subtitle">填写信息，开始您的农业之旅</p>
        </div>

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="rules"
          class="register-form"
        >
          <el-form-item prop="userName">
            <label class="form-label">用户名</label>
            <el-input
              v-model="registerForm.userName"
              placeholder="请输入用户名"
              size="large"
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码（至少6位）"
              size="large"
              show-password
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <label class="form-label">确认密码</label>
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              show-password
              class="form-input"
            />
          </el-form-item>

          <el-form-item v-if="registerForm.role !== 'bank'" prop="realName">
            <label class="form-label">真实姓名</label>
            <el-input
              v-model="registerForm.realName"
              placeholder="请输入真实姓名"
              size="large"
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="phone">
            <label class="form-label">手机号码</label>
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号码"
              size="large"
              class="form-input"
            />
          </el-form-item>

          <el-form-item prop="role">
            <label class="form-label">角色类型</label>
            <el-select
              v-model="registerForm.role"
              placeholder="请选择您的角色"
              size="large"
              class="form-input"
            >
              <el-option label="农户" value="farmer">
                <div class="role-option">
                  <span class="role-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="12" cy="8" r="3"/>
                      <path d="M8 21v-5a4 4 0 014-4M8 21h8M12 12v5M8 10l2-2M16 10l-2-2"/>
                    </svg>
                  </span>
                  <span>农户</span>
                  <span class="role-desc">销售农产品，申请融资</span>
                </div>
              </el-option>
              <el-option label="买家" value="buyer">
                <div class="role-option">
                  <span class="role-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <circle cx="9" cy="21" r="1"/>
                      <circle cx="20" cy="21" r="1"/>
                      <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/>
                    </svg>
                  </span>
                  <span>买家</span>
                  <span class="role-desc">采购优质农产品</span>
                </div>
              </el-option>
              <el-option label="专家" value="expert">
                <div class="role-option">
                  <span class="role-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M12 14l9-5-9-5-9 5 9 5z"/>
                      <path d="M12 14v7"/>
                      <path d="M5 10v6c0 1 3 3 7 3s7-2 7-3v-6"/>
                    </svg>
                  </span>
                  <span>专家</span>
                  <span class="role-desc">在线答疑，知识分享</span>
                </div>
              </el-option>
              <el-option label="银行" value="bank">
                <div class="role-option">
                  <span class="role-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <path d="M3 21h18"/>
                      <path d="M3 10h18"/>
                      <path d="M5 10l7-6 7 6"/>
                      <path d="M6 10v8M10 10v8M14 10v8M18 10v8"/>
                    </svg>
                  </span>
                  <span>银行</span>
                  <span class="role-desc">提供融资产品</span>
                </div>
              </el-option>
            </el-select>
            <p class="role-tip">选择<span>专家</span>或<span>银行</span>需上传资质材料，提交后由管理员审核，通过后方可登录。</p>
          </el-form-item>

          <!-- 专家/银行「注册即申请」附加资质信息（仅选中专家/银行时显示） -->
          <template v-if="registerForm.role === 'expert' || registerForm.role === 'bank'">
            <el-form-item v-if="registerForm.role === 'bank'" prop="belong">
              <label class="form-label">银行名称</label>
              <el-input v-model="registerForm.belong" placeholder="如：青岛银行" size="large" class="form-input" />
              <p class="field-tip">请填写银行的官方全称，该名称即为本行在平台的展示名（发布融资产品、审批、首页展示等均以此为准，不可冒用他行名称）。</p>
            </el-form-item>
            <template v-if="registerForm.role === 'expert'">
              <el-form-item prop="profession">
                <label class="form-label">专业方向</label>
                <el-input v-model="registerForm.profession" placeholder="如：农学、植保" size="large" class="form-input" />
              </el-form-item>
              <el-form-item prop="position">
                <label class="form-label">职位</label>
                <el-input v-model="registerForm.position" placeholder="如：教授、农艺师" size="large" class="form-input" />
              </el-form-item>
              <el-form-item prop="belong">
                <label class="form-label">所属单位</label>
                <el-input v-model="registerForm.belong" placeholder="如：山东省农科院" size="large" class="form-input" />
              </el-form-item>
            </template>
            <el-form-item prop="reason">
              <label class="form-label">申请理由</label>
              <el-input v-model="registerForm.reason" type="textarea" :rows="3" placeholder="简要说明您的资质情况与申请理由" class="form-input" />
            </el-form-item>
            <el-form-item>
              <label class="form-label">资质材料（可选）</label>
              <el-upload
                action="/api/upload"
                :on-success="onMaterialUploaded"
                :on-remove="onMaterialRemoved"
                :file-list="materialFileList"
                :limit="3"
                accept="image/*,.pdf"
              >
                <el-button type="primary" plain size="small">上传材料</el-button>
                <template #tip>
                  <div class="upload-tip">资质 / 执业证明等，最多 3 个文件（图片或 PDF）</div>
                </template>
              </el-upload>
            </el-form-item>
          </template>

          <el-form-item>
            <button
              type="button"
              class="btn-submit"
              :disabled="loading"
              @click="handleRegister"
            >
              <span v-if="!loading">创建账号</span>
              <span v-else class="loading-text">
                <svg class="loading-icon" viewBox="0 0 24 24" fill="none">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" opacity="0.3"/>
                  <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
                </svg>
                注册中...
              </span>
            </button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          <span class="footer-text">已有账号？</span>
          <router-link to="/login" class="footer-link">立即登录</router-link>
        </div>

        <div class="form-terms">
          <p>注册即表示您同意我们的
            <a href="#" class="terms-link">服务条款</a>
            和
            <a href="#" class="terms-link">隐私政策</a>
          </p>
        </div>
      </div>
    </div>

    <!-- 人机验证弹窗：点击「创建账号」且表单校验通过后出现，验证通过自动提交注册 -->
    <el-dialog
      v-model="turnstileDialogVisible"
      title="安全验证"
      width="380px"
      align-center
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div style="text-align:center; padding:8px 0 4px;">
        <p style="font-size:13px;color:#6B7280;margin:0 0 16px;line-height:1.6;">
          为确认是您本人操作，请完成下方人机验证后自动提交注册
        </p>
        <Turnstile
          ref="turnstileRef"
          @verified="onVerified"
          @expired="onExpired"
          @error="onError"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import Turnstile from '@/components/Turnstile.vue'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  userName: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  role: '',
  // 专家/银行「注册即申请」附加字段
  profession: '',
  position: '',
  belong: '',
  reason: '',
  materials: ''
})
const materialFileList = ref([])

// Cloudflare Turnstile 人机验证（点击「创建账号」且表单校验通过后弹窗）
const turnstileDialogVisible = ref(false)
const turnstileToken = ref('')
const turnstileRef = ref(null)
// 弹窗内验证通过 → 拿到 token → 关闭弹窗 → 自动提交注册
const onVerified = (token) => {
  turnstileToken.value = token
  turnstileDialogVisible.value = false
  submitRegister()
}
// 验证过期：重置组件让用户重试（token 一次性）
const onExpired = () => {
  turnstileToken.value = ''
  turnstileRef.value?.reset()
  ElMessage.warning('人机验证已过期，请重新完成验证')
}
// 验证失败：重置组件让用户重试
const onError = () => {
  turnstileToken.value = ''
  turnstileRef.value?.reset()
  ElMessage.error('人机验证失败，请重试')
}

// 页面跳转
const goTo = (path) => {
  router.push(path)
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 真实姓名：银行角色以「银行名称」作为名称（提交时同步写入 realName），故此处不再单独校验；
// 其余角色（农户/买家/专家）仍要求填写真实姓名。
const validateRealName = (rule, value, callback) => {
  if (registerForm.role === 'bank') {
    callback()
  } else if (!value || !value.trim()) {
    callback(new Error('请输入真实姓名'))
  } else {
    callback()
  }
}

// 银行注册必填银行名称（农户/买家/专家不触发该校验）
const validateBelong = (rule, value, callback) => {
  if (registerForm.role === 'bank' && (!value || !value.trim())) {
    callback(new Error('请填写银行名称'))
  } else {
    callback()
  }
}

// 资质材料上传：/upload 返回 { data: { url } }，多个用逗号拼接
const onMaterialUploaded = (res) => {
  const url = res?.data?.url
  if (url) {
    registerForm.materials = registerForm.materials ? registerForm.materials + ',' + url : url
  }
}
const onMaterialRemoved = (_file, fileList) => {
  registerForm.materials = fileList
    .map(f => f?.response?.data?.url || f?.url)
    .filter(Boolean)
    .join(',')
}

const rules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: ['blur', 'change'] },
    { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['blur', 'change'] },
    { min: 6, message: '密码长度不能少于6位', trigger: ['blur', 'change'] }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: ['blur', 'change'] },
    { validator: validateConfirmPassword, trigger: ['blur', 'change'] }
  ],
  realName: [
    { validator: validateRealName, trigger: ['blur', 'change'] }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: ['blur', 'change'] },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: ['blur', 'change'] }
  ],
  role: [
    { required: true, message: '请选择角色类型', trigger: 'change' }
  ],
  belong: [
    { validator: validateBelong, trigger: 'blur' }
  ]
}

// 点击「创建账号」：先做表单校验，通过后弹人机验证窗口（不在此处提交）
const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请填写完整且正确的注册信息')
    return
  }
  turnstileToken.value = ''
  turnstileDialogVisible.value = true
}

// 人机验证通过后真正提交注册（token 一次性，由弹窗验证通过触发）
const submitRegister = async () => {
  loading.value = true
  try {
    const payload = {
      userName: registerForm.userName,
      password: registerForm.password,
      // 银行：以「银行名称」作为 realName（即展示名），杜绝真实姓名与银行名两字段并存且不一致
      realName: registerForm.role === 'bank' ? registerForm.belong : registerForm.realName,
      phone: registerForm.phone,
      role: registerForm.role,
      cfTurnstileToken: turnstileToken.value
    }
    // 专家/银行「注册即申请」：附带资质材料等信息
    if (registerForm.role === 'expert' || registerForm.role === 'bank') {
      payload.profession = registerForm.profession
      payload.position = registerForm.position
      payload.belong = registerForm.belong
      payload.reason = registerForm.reason
      payload.materials = registerForm.materials
    }

    const result = await userStore.registerUser(payload)

    if (result.success) {
      // 专家/银行：注册即申请，账号需管理员审核通过方可登录，明确提示「等待审核」
      const needsReview = registerForm.role === 'expert' || registerForm.role === 'bank'
      if (needsReview) {
        await ElMessageBox.alert(
          '您的账号已创建，请等待管理员审核通过后即可登录。',
          '申请已提交',
          { confirmButtonText: '我知道了', type: 'success' }
        )
      } else {
        ElMessage.success(result.message || '注册成功')
      }
      router.push('/login')
    } else {
      ElMessage.error(result.message || '注册失败')
      turnstileToken.value = '' // token 已消耗；用户改完信息重新点「创建账号」会再次弹验证
    }
  } catch (error) {
    ElMessage.error(error.message || '注册失败，请稍后重试')
    turnstileToken.value = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ===== 全局变量 ===== */
.register-page {
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
.register-page {
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

.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.brand-logo svg {
  width: 64px;
  height: 64px;
  color: rgba(255, 255, 255, 0.9);
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
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 32px;
}

/* 优势卡片 */
.brand-benefits {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 32px;
}

.benefit-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  text-align: left;
}

.benefit-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--color-secondary);
  background: rgba(201, 166, 97, 0.2);
  border-radius: 10px;
  flex-shrink: 0;
}

.benefit-icon svg {
  width: 20px;
  height: 20px;
}

.benefit-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 2px;
}

.benefit-desc {
  font-size: 12px;
  opacity: 0.8;
  margin: 0;
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

/* ===== 表单样式 ===== */
.register-form {
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

:deep(.el-select .el-select__wrapper) {
  padding: 12px 16px;
  border-radius: 10px;
  border: 1.5px solid var(--color-border);
  box-shadow: none;
}

:deep(.el-select .el-select__wrapper:hover) {
  border-color: var(--color-primary-light);
}

:deep(.el-select .el-select__wrapper.is-focus) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(45, 90, 61, 0.1);
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__error) {
  font-size: 13px;
  color: var(--color-accent);
}

/* 角色选项样式 */
.role-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.role-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
}

.role-icon svg {
  width: 20px;
  height: 20px;
}

.role-desc {
  font-size: 12px;
  color: var(--color-text-light);
  margin-left: auto;
}

.role-tip {
  margin: 10px 2px 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-light);
  background: var(--color-bg);
  padding: 10px 12px;
  border-radius: 8px;
  border-left: 3px solid var(--color-secondary);
}

.role-tip span {
  color: var(--color-primary);
  font-weight: 600;
}

.upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-light);
  line-height: 1.5;
}

.field-tip {
  margin: 6px 2px 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-light);
  background: var(--color-bg);
  padding: 8px 12px;
  border-radius: 8px;
  border-left: 3px solid var(--color-secondary);
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

.form-terms {
  text-align: center;
  padding: 8px 12px;
  background: var(--color-bg);
  border-radius: 8px;
  margin-bottom: 16px;
}

.form-terms p {
  font-size: 11px;
  color: var(--color-text-light);
  margin: 0;
}

.form-terms p {
  font-size: 12px;
  color: var(--color-text-light);
  margin: 0;
}

.terms-link {
  color: var(--color-primary);
  text-decoration: none;
}

.terms-link:hover {
  text-decoration: underline;
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

  .brand-title {
    font-size: 36px;
  }
}
</style>
