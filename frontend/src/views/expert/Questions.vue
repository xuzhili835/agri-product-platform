<template>
  <div class="expert-questions-page">
    <div class="page-header">
      <h1>问答管理</h1>
      <p class="subtitle">回答农户和买家的问题，支持多轮追问</p>
    </div>

    <!-- 筛选区 -->
    <div class="filter-section">
      <el-radio-group v-model="filterStatus" @change="loadQuestions">
        <el-radio-button :value="-1">全部</el-radio-button>
        <el-radio-button :value="0">待回答</el-radio-button>
        <el-radio-button :value="1">已回答</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 问题列表 -->
    <div class="questions-list" v-loading="loading">
      <div v-for="question in questionList" :key="question.id" class="question-card">
        <div class="question-header">
          <div class="question-info">
            <h3 class="question-title">{{ question.title }}</h3>
            <div class="question-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                提问者：{{ question.questionerRealName || question.questioner }}
              </span>
              <span class="meta-item" v-if="question.phone">
                <el-icon><Phone /></el-icon>
                {{ question.phone }}
              </span>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(question.createTime) }}
              </span>
            </div>
          </div>
          <el-tag :type="question.status === 1 ? 'success' : 'warning'" size="large">
            {{ question.status === 1 ? '已回答' : '待回答' }}
          </el-tag>
        </div>

        <div class="question-content">
          <div class="content-section">
            <h4>问题描述：</h4>
            <p>{{ question.question }}</p>
          </div>

          <div class="content-section answer-section" v-if="question.status === 1 && question.answer">
            <h4>最近回答：</h4>
            <p class="answer-text">{{ question.answer }}</p>
          </div>
        </div>

        <div class="question-actions">
          <el-button type="primary" @click="goToConversation(question)">
            <el-icon><ChatDotRound /></el-icon>
            {{ question.status === 0 ? '去回答' : '查看 / 回复' }}
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loading && questionList.length === 0" description="暂无问题" />
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadQuestions"
        @size-change="loadQuestions"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Phone, Calendar, ChatDotRound } from '@element-plus/icons-vue'
import { getQuestionList } from '@/api/question'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const filterStatus = ref(-1)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const questionList = ref([])

// 进入问题对话页（在对话流中回答 / 回复追问）
const goToConversation = (question) => {
  router.push({ path: `/question/${question.id}`, query: { from: route.fullPath } })
}

// 加载问题列表
const loadQuestions = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      // 仅展示分配给当前专家的问题（expert_name = 我的 userName）
      expertName: userStore.userInfo?.userName || ''
    }

    if (filterStatus.value !== -1) {
      params.status = filterStatus.value
    }

    const res = await getQuestionList(params)
    if (res.code === 200) {
      questionList.value = (res.data.records || []).map(q => ({
        id: q.id,
        title: q.title,
        question: q.question,
        answer: q.answer,
        questioner: q.questioner,
        questionerRealName: q.questionerRealName,
        phone: q.phone,
        status: q.status,
        createTime: q.createTime
      }))
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadQuestions()
})
</script>

<style scoped>
.expert-questions-page {
  padding: var(--spacing-6, 24px);
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--spacing-6, 24px);
}

.page-header h1 {
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.subtitle {
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

.filter-section {
  margin-bottom: var(--spacing-4, 16px);
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
}

.question-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-5, 20px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-4, 16px);
  padding-bottom: var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.question-info {
  flex: 1;
}

.question-title {
  font-size: var(--font-size-xl, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-3, 12px);
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
  color: var(--color-text-secondary, #4a5249);
}

.question-content {
  margin-bottom: var(--spacing-4, 16px);
}

.content-section {
  margin-bottom: var(--spacing-3, 12px);
}

.content-section h4 {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-secondary, #4a5249);
  margin: 0 0 var(--spacing-2, 8px);
}

.content-section p {
  font-size: var(--font-size-base, 16px);
  line-height: 1.6;
  color: var(--color-text-primary, #1f2923);
  margin: 0;
  white-space: pre-wrap;
}

.answer-section {
  background: var(--color-bg-secondary, #f7f5f0);
  padding: var(--spacing-3, 12px);
  border-radius: var(--radius-base, 8px);
}

.answer-text {
  color: var(--color-primary, #2d5a3d);
  font-weight: var(--font-weight-medium, 500);
}

.question-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
  justify-content: flex-end;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-8, 32px);
}
</style>
