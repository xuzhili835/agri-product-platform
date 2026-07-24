<template>
  <div class="question-ask-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2 class="page-title">向专家提问</h2>
        <p class="page-subtitle">描述您的农业问题，专家将为您解答</p>
      </div>

      <!-- 提问表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="ask-form"
      >
        <!-- 选择专家 -->
        <el-form-item label="咨询专家" prop="expertName">
          <el-select
            v-model="formData.expertName"
            placeholder="请选择要咨询的专家"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="expert in expertList"
              :key="expert.userName"
              :label="`${expert.realName}（${expert.profession || '农业专家'}）`"
              :value="expert.userName"
            />
          </el-select>
        </el-form-item>

        <!-- 农作物 -->
        <el-form-item label="农作物" prop="plantName">
          <el-input
            v-model="formData.plantName"
            placeholder="请输入农作物名称，如：水稻、玉米、番茄"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>

        <!-- 联系电话 -->
        <el-form-item label="联系电话" prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入联系电话，方便专家回访"
            maxlength="20"
          />
        </el-form-item>

        <!-- 问题标题 -->
        <el-form-item label="问题标题" prop="title">
          <el-input
            v-model="formData.title"
            placeholder="请简要描述您的问题，如：小麦叶片发黄是什么原因？"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <!-- 问题详情 -->
        <el-form-item label="问题描述" prop="question">
          <el-input
            v-model="formData.question"
            type="textarea"
            :rows="10"
            placeholder="请详细描述您的问题，包括：&#10;1. 具体情况（种植什么作物、面积、生长阶段等）&#10;2. 出现的问题（症状、发生时间等）&#10;3. 已尝试的解决方法&#10;4. 其他相关信息"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button @click="handleBack">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            提交问题
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 提示信息 -->
      <el-card class="tips-card">
        <template #header>
          <div class="tips-header">
            <el-icon><InfoFilled /></el-icon>
            提问须知
          </div>
        </template>
        <ul class="tips-list">
          <li>请详细描述您的问题，以便专家给出准确的解答</li>
          <li>专家将在24小时内回复您的问题</li>
          <li>提问后可在"我的问题"中查看回答状态</li>
          <li>请勿重复提交相同的问题</li>
        </ul>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { askQuestion } from '@/api/question'
import { getExpertList } from '@/api/expert'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)
const expertList = ref([])

// 表单数据（字段对齐后端 QuestionRequest）
const formData = reactive({
  expertName: '',
  plantName: '',
  phone: '',
  title: '',
  question: ''
})

// 表单验证规则
const formRules = {
  expertName: [
    { required: true, message: '请选择咨询专家', trigger: 'change' }
  ],
  plantName: [
    { required: true, message: '请输入农作物名称', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度应在5-100个字符之间', trigger: 'blur' }
  ],
  question: [
    { required: true, message: '请输入问题描述', trigger: 'blur' },
    { min: 5, max: 2000, message: '描述长度应在5-2000个字符之间', trigger: 'blur' }
  ]
}

// 加载专家列表
const loadExperts = async () => {
  try {
    const res = await getExpertList()
    expertList.value = res.data || []
  } catch (error) {
    console.error('加载专家列表失败:', error)
  }
}

// 返回
const handleBack = () => {
  router.back()
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    // 仅农户/买家可向专家提问
    if (userStore.isLoggedIn() && !['farmer', 'buyer'].includes(userStore.role)) {
      ElMessage.info('仅农户与买家可向专家提问')
      return
    }

    submitting.value = true

    await askQuestion({
      expertName: formData.expertName,
      plantName: formData.plantName,
      phone: formData.phone,
      title: formData.title,
      question: formData.question
    })
    ElMessage.success('提问成功，专家将尽快为您解答')
    router.push('/question')
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 检查登录
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录后提问')
    router.push({ path: '/login', query: { redirect: '/question/ask' } })
    return
  }
  // 仅农户/买家可提问；银行/管理员/专家无此入口，直接引导离开
  if (!['farmer', 'buyer'].includes(userStore.role)) {
    ElMessage.info('仅农户与买家可向专家提问')
    router.push('/')
    return
  }
  // 联系电话默认带入个人资料手机号（用户可自行修改）
  if (!formData.phone) {
    formData.phone = userStore.userInfo?.phone || ''
  }

  // 从路由参数预选专家
  if (route.query.expert) {
    formData.expertName = String(route.query.expert)
  }
  loadExperts()
})
</script>

<style scoped>
.question-ask-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

.page-header {
  text-align: center;
}

.page-title {
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.page-subtitle {
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

.ask-form {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-8, 32px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.ask-form :deep(.el-form-item__label) {
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
}

.ask-form :deep(.el-input__inner),
.ask-form :deep(.el-textarea__inner) {
  font-size: var(--font-size-base, 16px);
}

.ask-form :deep(.el-textarea__inner) {
  line-height: 1.6;
}

/* ===== 提示卡片 ===== */
.tips-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f4ea 100%);
  border: 1px solid #d4e7f0;
}

.tips-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-primary, #2d5a3d);
}

.tips-list {
  margin: 0;
  padding-left: var(--spacing-5, 20px);
  color: var(--color-text-secondary, #4a5249);
  line-height: 1.8;
}

.tips-list li {
  margin-bottom: var(--spacing-2, 8px);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .ask-form {
    padding: var(--spacing-5, 20px);
  }

  .ask-form :deep(.el-form-item__label) {
    width: 80px !important;
  }
}
</style>
