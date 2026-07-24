<template>
  <div class="expert-help-page">
    <!-- 统一返回栏（左上，与全站其它页一致） -->
    <BackBar to="/" />
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="title-section">
        <h1 class="page-title">专家助力</h1>
        <p class="page-subtitle">农业专家在线，为您答疑解惑</p>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content-grid">
      <!-- 专家列表 -->
      <div class="experts-section">
        <h2 class="section-title">认证专家</h2>
        <div class="experts-list">
          <div v-for="expert in experts" :key="expert.id" class="expert-card" @click="goToExpertDetail(expert.id)">
            <div class="expert-avatar">
              <img v-if="expert.avatar" :src="expert.avatar" :alt="expert.name">
              <span v-else>{{ expert.name?.charAt(0) }}</span>
            </div>
            <div class="expert-info">
              <div class="expert-name">{{ expert.name }}</div>
              <div class="expert-title">{{ expert.title }}</div>
              <div class="expert-specialty">{{ expert.specialty }}</div>
              <div class="expert-stats">
                <span class="stat-item">{{ expert.belong || '农业专家' }}</span>
              </div>
            </div>
            <div class="expert-actions">
              <el-button v-if="canConsultEntry" type="primary" size="small" @click.stop="handleAskExpert(expert)">提问</el-button>
              <span v-else class="consult-disabled-tip">仅农户/买家可提问</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 知识推荐（问答为农户与专家的私下咨询，不对外公开；此处仅推荐公开的知识库内容） -->
      <div class="questions-section">
        <h2 class="section-title">知识推荐</h2>
        <div class="knowledge-list">
          <div v-for="knowledge in knowledgeList" :key="knowledge.id" class="knowledge-item" @click="goToKnowledge(knowledge.id)">
            <div class="knowledge-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
              </svg>
            </div>
            <div class="knowledge-content">
              <div class="knowledge-title">{{ knowledge.title }}</div>
              <div class="knowledge-author">{{ knowledge.author }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import BackBar from '@/components/BackBar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)

// 仅农户/买家可向专家提问；未登录时展示按钮并引导登录
const canConsultEntry = computed(() => !userStore.isLoggedIn() || ['farmer', 'buyer'].includes(userStore.role))

// 处理向专家提问
const handleAskExpert = (expert) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录后向专家提问')
    router.push({ path: '/login', query: { redirect: `/question/ask?expert=${expert.id}` } })
    return
  }
  if (!['farmer', 'buyer'].includes(userStore.role)) {
    ElMessage.info('仅农户与买家可向专家提问')
    return
  }
  router.push(`/question/ask?expert=${expert.id}`)
}

// 跳转专家详情
const goToExpertDetail = (userName) => {
  if (userName) router.push({ path: `/expert/${userName}`, query: { from: route.fullPath } })
}

// 跳转知识详情
const goToKnowledge = (id) => {
  router.push({ path: `/knowledge/${id}`, query: { from: route.fullPath } })
}

// 加载专家数据
const loadExperts = async () => {
  loading.value = true
  try {
    const { getExpertList } = await import('@/api/expert')
    const { getKnowledgeList } = await import('@/api/knowledge')

    // 获取专家列表（/expert/list 返回的是数组，不是分页对象）
    const expertRes = await getExpertList()
    if (expertRes.code === 200) {
      const list = expertRes.data || []
      // 拿到真实数据时填充列表
      if (list.length > 0) {
        experts.value = list.map(e => ({
          id: e.userName,
          name: e.realName,
          title: e.position || '农业专家',
          specialty: e.profession || '农业技术',
          belong: e.belong || '',
          avatar: ''
        }))
      }
    }

    // 获取知识推荐
    const knowledgeRes = await getKnowledgeList({ page: 1, pageSize: 5 })
    if (knowledgeRes.code === 200) {
      const kList = knowledgeRes.data.records || []
      if (kList.length > 0) {
        knowledgeList.value = kList.map(k => ({
          id: k.knowledgeId,
          title: k.title,
          author: k.ownRealName || k.ownName || '专家'
        }))
      }
    }
  } catch (error) {
    console.error('加载专家数据失败:', error)
    // API 返回空时各列表保持空状态（不再使用兜底假数据）
  } finally {
    loading.value = false
  }
}

const experts = ref([])

const knowledgeList = ref([])

onMounted(() => {
  loadExperts()
})
</script>

<style scoped>
.expert-help-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-8, 32px) var(--spacing-6, 24px);
}

.page-header {
  text-align: center;
  margin-bottom: var(--spacing-8, 32px);
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

/* ===== 内容网格 ===== */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-6, 24px);
}

.section-title {
  font-size: var(--font-size-xl, 24px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-4, 16px);
}

/* ===== 专家列表 ===== */
.experts-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3, 12px);
}

.expert-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px);
  background: var(--color-bg-primary, #ffffff);
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-lg, 12px);
  transition: all var(--transition-fast, 150ms ease);
}

.expert-card:hover {
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(31, 41, 35, 0.06));
  border-color: var(--color-primary, #2d5a3d);
}

.expert-avatar {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-expert, #b85c38);
  color: var(--color-text-inverse, #ffffff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xl, 24px);
  font-weight: var(--font-weight-bold, 700);
  flex-shrink: 0;
  overflow: hidden;
}

.expert-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.expert-info {
  flex: 1;
  min-width: 0;
}

.expert-name {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
}

.expert-title {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-expert, #b85c38);
  margin-bottom: var(--spacing-1, 4px);
}

.expert-specialty {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
  margin-bottom: var(--spacing-2, 8px);
}

.expert-stats {
  display: flex;
  gap: var(--spacing-3, 12px);
}

.stat-item {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.consult-disabled-tip {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  white-space: nowrap;
}

/* ===== 知识列表 ===== */
.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2, 8px);
}

.knowledge-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-3, 12px);
  background: var(--color-bg-primary, #ffffff);
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.knowledge-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
  border-color: var(--color-expert, #b85c38);
}

.knowledge-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45, 90, 61, 0.1);
  border-radius: var(--radius-base, 8px);
  color: var(--color-primary, #2d5a3d);
  flex-shrink: 0;
}

.knowledge-icon svg {
  width: 18px;
  height: 18px;
}

.knowledge-content {
  flex: 1;
  min-width: 0;
}

.knowledge-title {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.knowledge-author {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: var(--font-size-3xl, 38px);
  }
}
</style>
