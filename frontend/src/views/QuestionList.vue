<template>
  <div class="question-list-page">
    <div class="container">
      <BackBar to="/" />
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">专家问答</h1>
        <p class="page-subtitle">向农业专家提问，获取专业解答</p>
      </div>

      <!-- 搜索和操作栏 -->
      <div class="action-bar">
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索问题..."
            :prefix-icon="'Search'"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="filter-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            class="filter-tab"
            :class="{ active: currentStatus === tab.value }"
            @click="currentStatus = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
        <el-button type="primary" @click="handleAsk" v-if="userStore.isLoggedIn()">
          <el-icon><Plus /></el-icon>
          我要提问
        </el-button>
        <el-button v-else @click="handleLogin">
          <el-icon><Plus /></el-icon>
          我要提问
        </el-button>
      </div>

      <!-- 加载状态 -->
      <el-skeleton v-if="loading" :rows="5" animated />

      <!-- 问题列表 -->
      <div v-else-if="questionList.length > 0" class="question-list">
        <div
          v-for="question in questionList"
          :key="question.id"
          class="question-item"
          @click="handleViewDetail(question)"
        >
          <div class="question-header">
            <div
              class="status-badge"
              :class="question.status === 1 ? 'status-answered' : 'status-pending'"
            >
              {{ getStatusText(question.status) }}
            </div>
            <h3 class="question-title">{{ question.title }}</h3>
          </div>
          <p class="question-content">{{ question.question }}</p>
          <div class="question-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ question.questionerRealName || question.questioner }}
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(question.createTime) }}
            </span>
            <span class="meta-item" v-if="question.plantName">
              <el-icon><ChatDotRound /></el-icon>
              {{ question.plantName }}
            </span>
          </div>
          <!-- 已回答时显示回答者 -->
          <div v-if="question.status === 1" class="answer-preview">
            <div class="answer-author">
              回答者：{{ question.expertRealName || question.expertName || '专家' }}
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-else description="暂无问题，快来提问吧！" />

      <!-- 分页 -->
      <div v-if="pagination.total > 0" class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadQuestions"
          @size-change="loadQuestions"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Plus, User, Calendar, ChatDotRound } from '@element-plus/icons-vue'
import { getQuestionList } from '@/api/question'
import BackBar from '@/components/BackBar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const searchKeyword = ref('')
const currentStatus = ref(-1)
const questionList = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

// 状态标签 - 使用整数状态码与后端保持一致
const statusTabs = [
  { value: -1, label: '全部' },
  { value: 0, label: '待回答' },
  { value: 1, label: '已回答' }
]

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    0: '待回答',
    1: '已回答'
  }
  return map[status] || '未知'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// 提问
const handleAsk = () => {
  router.push('/question/ask')
}

// 登录
const handleLogin = () => {
  router.push({ path: '/login', query: { redirect: '/question' } })
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadQuestions()
}

// 查看详情
const handleViewDetail = (question) => {
  router.push({ path: `/question/${question.id}`, query: { from: route.fullPath } })
}

// 加载问题列表
const loadQuestions = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }

    if (currentStatus.value !== -1) {
      params.status = currentStatus.value
    }

    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    const res = await getQuestionList(params)
    questionList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 监听状态变化
watch(currentStatus, () => {
  pagination.page = 1
  loadQuestions()
})

onMounted(() => {
  loadQuestions()
})
</script>

<style scoped>
.question-list-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
}

.page-header {
  text-align: center;
  margin-bottom: var(--spacing-6, 24px);
}

.page-title {
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: var(--font-size-4xl, 48px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.page-subtitle {
  font-size: var(--font-size-lg, 20px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

/* ===== 操作栏 ===== */
.action-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-4, 16px);
  margin-bottom: var(--spacing-6, 24px);
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 200px;
}

.filter-tabs {
  display: flex;
  gap: var(--spacing-2, 8px);
}

.filter-tab {
  padding: var(--spacing-2, 8px) var(--spacing-4, 16px);
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-secondary, #4a5249);
  background: transparent;
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-full, 9999px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.filter-tab:hover {
  color: var(--color-primary, #2d5a3d);
  border-color: var(--color-primary, #2d5a3d);
}

.filter-tab.active {
  color: var(--color-text-inverse, #ffffff);
  background: var(--color-primary, #2d5a3d);
  border-color: var(--color-primary, #2d5a3d);
}

/* ===== 问题列表 ===== */
.question-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
}

.question-item {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-5, 20px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.question-item:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(0,0,0,0.1));
  transform: translateY(-2px);
}

.question-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  margin-bottom: var(--spacing-3, 12px);
}

.status-badge {
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  border-radius: var(--radius-full, 9999px);
  white-space: nowrap;
}

.status-pending {
  color: #e6a23c;
  background: #fdf6ec;
}

.status-answered {
  color: #67c23a;
  background: #f0f9ff;
}

.status-closed {
  color: #909399;
  background: #f4f4f5;
}

.question-title {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-content {
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-secondary, #4a5249);
  line-height: 1.6;
  margin: 0 0 var(--spacing-3, 12px);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.question-meta {
  display: flex;
  gap: var(--spacing-4, 16px);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
}

.answer-preview {
  margin-top: var(--spacing-3, 12px);
  padding-top: var(--spacing-3, 12px);
  border-top: 1px solid var(--color-border, #e5e0d8);
}

.answer-author {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-primary, #2d5a3d);
}

/* ===== 分页 ===== */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-8, 32px);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    width: 100%;
  }

  .filter-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .question-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .question-title {
    white-space: normal;
  }
}
</style>
