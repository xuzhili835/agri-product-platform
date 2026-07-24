<template>
  <div class="expert-detail-page">
    <div class="container">
      <BackBar to="/expert-help" />
      <!-- 专家基本信息卡片 -->
      <el-card class="expert-info-card" v-if="expert" v-loading="loading">
        <div class="expert-header">
          <div class="expert-avatar">
            <img v-if="expert.avatar" :src="expert.avatar" :alt="expert.realName">
            <span v-else>{{ expert.realName?.charAt(0) || '专' }}</span>
          </div>
          <div class="expert-basic">
            <h1 class="expert-name">{{ expert.realName }}</h1>
            <div class="expert-meta">
              <span class="meta-item">
                <el-icon><OfficeBuilding /></el-icon>
                {{ expert.belong || '暂无单位' }}
              </span>
              <span class="meta-item">
                <el-icon><Medal /></el-icon>
                {{ expert.position || '农业专家' }}
              </span>
            </div>
            <div class="expert-tags">
              <el-tag type="success" size="large">{{ expert.profession || '农业技术' }}</el-tag>
            </div>
          </div>
          <div class="expert-actions">
            <template v-if="canConsultEntry">
              <el-button type="primary" size="large" @click="handleAsk">
                <el-icon><ChatDotRound /></el-icon>
                向TA提问
              </el-button>
              <el-button type="primary" size="large" @click="handleReserve">
                <el-icon><Calendar /></el-icon>
                预约专家
              </el-button>
            </template>
            <span v-else class="consult-disabled-tip">仅农户与买家可咨询/预约专家</span>
          </div>
        </div>
      </el-card>

      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" class="content-tabs">
        <el-tab-pane label="发布知识" name="knowledge">
          <div class="knowledge-list" v-if="knowledgeList.length > 0" v-loading="knowledgeLoading">
            <div v-for="item in knowledgeList" :key="item.knowledgeId" class="knowledge-item" @click="goToKnowledge(item.knowledgeId)">
              <h3>{{ item.title }}</h3>
              <p class="summary">{{ item.content?.substring(0, 100) }}...</p>
              <div class="meta">
                <span>{{ formatDate(item.createTime) }}</span>
                <span v-if="item.status === 1">已发布</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无发布知识" />
        </el-tab-pane>

        <el-tab-pane label="回答问题" name="questions">
          <div class="question-list" v-if="questionList.length > 0" v-loading="questionsLoading">
            <div v-for="item in questionList" :key="item.id" class="question-item" @click="goToQuestion(item.id)">
              <div class="question-header">
                <h3>{{ item.title }}</h3>
                <el-tag type="success" size="small">已回答</el-tag>
              </div>
              <p class="question-preview">{{ item.question?.substring(0, 80) }}...</p>
              <p class="answer-preview" v-if="item.answer">
                <el-icon><Select /></el-icon>
                {{ item.answer?.substring(0, 100) }}...
              </p>
              <div class="meta">
                <span>{{ formatDate(item.createTime) }}</span>
                <span>提问者：{{ item.questionerRealName || item.questioner }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无回答问题" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Calendar, OfficeBuilding, Medal, Select } from '@element-plus/icons-vue'
import { getExpertDetail } from '@/api/expert'
import { getKnowledgeList } from '@/api/knowledge'
import { getQuestionList } from '@/api/question'
import { useUserStore } from '@/stores/user'
import BackBar from '@/components/BackBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 仅农户/买家可咨询/预约专家；未登录时展示按钮并引导登录
const canConsultEntry = computed(() => !userStore.isLoggedIn() || ['farmer', 'buyer'].includes(userStore.role))

const expert = ref(null)
const activeTab = ref('knowledge')
const knowledgeList = ref([])
const questionList = ref([])
const loading = ref(false)
const knowledgeLoading = ref(false)
const questionsLoading = ref(false)

const loadExpertDetail = async () => {
  loading.value = true
  try {
    const userName = route.params.userName
    const res = await getExpertDetail(userName)
    if (res.code === 200) {
      expert.value = res.data
      // 知识 own_name 与问答 expert_name 都存储专家 userName
      if (expert.value.userName) {
        loadExpertKnowledge(expert.value.userName)
        loadExpertQuestions(expert.value.userName)
      }
    } else {
      ElMessage.error('加载专家信息失败')
    }
  } catch (error) {
    ElMessage.error('加载专家信息失败')
  } finally {
    loading.value = false
  }
}

const loadExpertKnowledge = async (expertName) => {
  knowledgeLoading.value = true
  try {
    const res = await getKnowledgeList({ page: 1, pageSize: 10, ownName: expertName, status: 1 })
    if (res.code === 200) {
      knowledgeList.value = (res.data.records || []).map(k => ({
        knowledgeId: k.knowledgeId || k.id,
        title: k.title,
        content: k.content,
        createTime: k.createTime,
        status: k.status
      }))
    }
  } catch (error) {
    console.error('加载知识失败:', error)
  } finally {
    knowledgeLoading.value = false
  }
}

const loadExpertQuestions = async (expertName) => {
  questionsLoading.value = true
  try {
    const res = await getQuestionList({ page: 1, pageSize: 10, expertName, status: 1 })
    if (res.code === 200) {
      questionList.value = (res.data.records || []).map(q => ({
        id: q.questionId || q.id,
        title: q.title,
        question: q.question,
        answer: q.answer,
        questioner: q.questioner,
        questionerRealName: q.questionerRealName,
        createTime: q.createTime
      }))
    }
  } catch (error) {
    console.error('加载问题失败:', error)
  } finally {
    questionsLoading.value = false
  }
}

const handleAsk = () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!['farmer', 'buyer'].includes(userStore.role)) {
    ElMessage.info('仅农户与买家可向专家提问')
    return
  }
  router.push(`/question/ask?expert=${expert.value?.userName}`)
}

const handleReserve = () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!['farmer', 'buyer'].includes(userStore.role)) {
    ElMessage.info('仅农户与买家可预约专家')
    return
  }
  router.push({ path: '/reserve', query: { expert: expert.value?.userName, from: route.fullPath } })
}

const goToKnowledge = (id) => {
  router.push({ path: `/knowledge/${id}`, query: { from: route.fullPath } })
}

const goToQuestion = (id) => {
  router.push({ path: `/question/${id}`, query: { from: route.fullPath } })
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString()
}

onMounted(() => {
  loadExpertDetail()
})
</script>

<style scoped>
.expert-detail-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}

.container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

.expert-info-card {
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.expert-header {
  display: flex;
  gap: var(--spacing-5, 20px);
  align-items: flex-start;
}

.expert-avatar {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-expert, #b85c38);
  color: var(--color-text-inverse, #ffffff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  flex-shrink: 0;
  overflow: hidden;
}

.expert-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.expert-basic {
  flex: 1;
  min-width: 0;
}

.expert-name {
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-3, 12px);
}

.expert-meta {
  display: flex;
  gap: var(--spacing-4, 16px);
  margin-bottom: var(--spacing-3, 12px);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  color: var(--color-text-secondary, #4a5249);
  font-size: var(--font-size-base, 16px);
}

.expert-tags {
  display: flex;
  gap: var(--spacing-2, 8px);
  flex-wrap: wrap;
}

.expert-actions {
  display: flex;
  gap: var(--spacing-3, 12px);
  flex-direction: column;
}

.consult-disabled-tip {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  white-space: nowrap;
}

.content-tabs {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-6, 24px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.knowledge-list,
.question-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3, 12px);
}

.knowledge-item,
.question-item {
  padding: var(--spacing-4, 16px);
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.knowledge-item:hover,
.question-item:hover {
  border-color: var(--color-primary, #2d5a3d);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.knowledge-item h3,
.question-item h3 {
  font-size: var(--font-size-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-2, 8px);
}

.question-header h3 {
  flex: 1;
  margin-right: var(--spacing-2, 8px);
}

.summary,
.question-preview {
  color: var(--color-text-secondary, #4a5249);
  line-height: 1.6;
  margin-bottom: var(--spacing-2, 8px);
}

.answer-preview {
  color: var(--color-success, #4a7c59);
  line-height: 1.6;
  background: rgba(74, 124, 89, 0.1);
  padding: var(--spacing-2, 8px);
  border-radius: var(--radius-base, 8px);
  margin-bottom: var(--spacing-2, 8px);
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-1, 4px);
}

.meta {
  color: var(--color-text-tertiary, #6b7280);
  font-size: var(--font-size-sm, 14px);
  display: flex;
  gap: var(--spacing-3, 12px);
}

@media (max-width: 768px) {
  .expert-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .expert-meta {
    justify-content: center;
  }

  .expert-actions {
    flex-direction: row;
    width: 100%;
  }

  .expert-actions button {
    flex: 1;
  }

  .expert-name {
    font-size: var(--font-size-2xl, 30px);
  }
}
</style>
