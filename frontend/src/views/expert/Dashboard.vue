<template>
  <div class="expert-dashboard">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">{{ greeting }}，{{ userStore.displayName() }}专家</h1>
      <p class="page-subtitle">分享您的专业知识，帮助农户解决问题</p>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-grid" v-loading="loading">
      <div class="stat-card primary">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
            <circle cx="12" cy="12" r="2" fill="currentColor"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.knowledgeCount }}</div>
          <div class="stat-label">已发布知识</div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>持续更新</span>
          </div>
        </div>
      </div>

      <div class="stat-card success">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.answeredCount }}</div>
          <div class="stat-label">回答问题</div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>实时更新</span>
          </div>
        </div>
      </div>

      <div class="stat-card warning">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 10v6M2 10l2-4h16l2 4M12 2v3M12 10a3 3 0 100 6 3 3 0 000-6z"/>
            <path d="M12 16c-3 0-5 1.5-6 3v2h12v-2c-1-1.5-3-3-6-3z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.pendingReservations }}</div>
          <div class="stat-label">待处理预约</div>
          <div class="stat-action">
            <router-link to="/expert/reservations">查看预约</router-link>
          </div>
        </div>
      </div>

      <div class="stat-card info">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.unreadCount }}</div>
          <div class="stat-label">未读消息</div>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="content-grid">
      <!-- 待回答问题 -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">待回答问题</h3>
          <router-link to="/expert/questions" class="card-link">查看全部</router-link>
        </div>
        <div class="question-list">
          <div v-for="question in pendingQuestions" :key="question.id" class="question-item">
            <div class="question-avatar">
              <span>{{ question.author.charAt(0) }}</span>
            </div>
            <div class="question-content">
              <div class="question-title">{{ question.title }}</div>
              <div class="question-meta">
                <span class="question-author">{{ question.author }}</span>
                <span class="question-time">{{ question.time }}</span>
              </div>
            </div>
            <el-button type="primary" size="small" @click="answerQuestion(question.id)">
              回答
            </el-button>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">快捷操作</h3>
        </div>
        <div class="quick-actions">
          <router-link to="/expert/knowledge" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
            </div>
            <span>发布知识</span>
          </router-link>
          <router-link to="/expert/questions" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <span>回答问题</span>
          </router-link>
          <router-link to="/expert/reservations" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
            </div>
            <span>预约管理</span>
          </router-link>
          <router-link to="/expert/profile" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="8" r="3"/>
                <path d="M8 21v-5a4 4 0 014-4M8 21h8"/>
              </svg>
            </div>
            <span>个人中心</span>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 最新知识 -->
    <div class="content-card">
      <div class="card-header">
        <h3 class="card-title">我发布的知识</h3>
        <router-link to="/expert/knowledge" class="card-link">查看全部</router-link>
      </div>
      <div class="knowledge-list">
        <div v-for="knowledge in recentKnowledge" :key="knowledge.id" class="knowledge-item">
          <div class="knowledge-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
            </svg>
          </div>
          <div class="knowledge-content">
            <div class="knowledge-title">{{ knowledge.title }}</div>
            <div class="knowledge-meta">
              <span class="knowledge-comments">{{ knowledge.comments }} 评论</span>
              <span class="knowledge-time">{{ knowledge.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 最新动态（真实站内通知，来自 tb_message） -->
    <RecentNotifications title="最新动态" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Top } from '@element-plus/icons-vue'
import { getKnowledgeList } from '@/api/knowledge'
import { getQuestionList } from '@/api/question'
import { getExpertReserveList } from '@/api/expert'
import { getUnreadCount } from '@/api/message'
import RecentNotifications from '@/components/RecentNotifications.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 当前专家 userName（知识 own_name 与问答 expert_name 都用它）
const expertUserName = computed(() => userStore.userInfo?.userName || '')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 统计数据
const stats = ref({
  knowledgeCount: 0,
  answeredCount: 0,
  pendingReservations: 0,
  unreadCount: 0
})

// 待回答问题
const pendingQuestions = ref([])

// 最近发布的知识
const recentKnowledge = ref([])

const loading = ref(false)

// 加载Dashboard数据
const loadDashboardData = async () => {
  if (!expertUserName.value) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    // 我发布的知识（含草稿）
    const knowledgeRes = await getKnowledgeList({ page: 1, pageSize: 5, ownName: expertUserName.value })
    if (knowledgeRes.code === 200) {
      const knowledge = knowledgeRes.data.records || []
      stats.value.knowledgeCount = knowledgeRes.data.total || 0
      recentKnowledge.value = knowledge.map(k => ({
        id: k.knowledgeId,
        title: k.title,
        comments: k.commentCount || 0,
        time: formatTime(k.createTime)
      }))
    }

    // 已回答问题数（expert_name = 我，status=1）
    const answeredRes = await getQuestionList({ page: 1, pageSize: 1, expertName: expertUserName.value, status: 1 })
    if (answeredRes.code === 200) {
      stats.value.answeredCount = answeredRes.data.total || 0
    }

    // 待回答问题（expert_name = 我，status=0）
    const pendingRes = await getQuestionList({ page: 1, pageSize: 5, expertName: expertUserName.value, status: 0 })
    if (pendingRes.code === 200) {
      const questions = pendingRes.data.records || []
      pendingQuestions.value = questions.map(q => ({
        id: q.id,
        title: q.title,
        author: q.questioner || '匿名用户',
        time: formatTime(q.createTime)
      }))
    }

    // 待处理预约数（expert_name = 我，status=0）
    const reserveRes = await getExpertReserveList({ page: 1, pageSize: 1, status: 0 })
    if (reserveRes.code === 200) {
      stats.value.pendingReservations = reserveRes.data.total || 0
    }

    // 未读消息数（站内通知：新提问 / 新预约 / 回复等）
    const unreadRes = await getUnreadCount()
    if (unreadRes.code === 200) {
      stats.value.unreadCount = unreadRes.data?.count || 0
    }
  } catch (error) {
    console.error('加载Dashboard数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString()
}

const answerQuestion = (id) => {
  router.push({ path: `/question/${id}`, query: { from: route.fullPath } })
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.expert-dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

/* ===== 页面标题 ===== */
.page-header {
  margin-bottom: var(--spacing-2, 8px);
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

/* ===== 指标卡片 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-4, 16px);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-4, 16px);
  padding: var(--spacing-5, 20px);
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(31, 41, 35, 0.06));
  transition: all var(--transition-fast, 150ms ease);
}

.stat-card:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(31, 41, 35, 0.08));
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg, 12px);
  flex-shrink: 0;
}

.stat-card.primary .stat-icon {
  background: rgba(184, 92, 56, 0.1);
  color: var(--color-expert, #b85c38);
}

.stat-card.success .stat-icon {
  background: rgba(74, 124, 89, 0.1);
  color: var(--color-success, #4a7c59);
}

.stat-card.warning .stat-icon {
  background: rgba(201, 166, 97, 0.1);
  color: var(--color-warning, #c9a661);
}

.stat-card.info .stat-icon {
  background: rgba(45, 90, 61, 0.1);
  color: var(--color-primary, #2d5a3d);
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: var(--font-size-2xl, 30px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  line-height: var(--line-height-tight, 1.25);
}

.stat-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin-top: var(--spacing-1, 4px);
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  margin-top: var(--spacing-2, 8px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
}

.stat-trend.positive {
  color: var(--color-success, #4a7c59);
}

.stat-action {
  margin-top: var(--spacing-2, 8px);
}

.stat-action a {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-expert, #b85c38);
  text-decoration: none;
  font-weight: var(--font-weight-medium, 500);
}

.stat-action a:hover {
  text-decoration: underline;
}

/* ===== 内容网格 ===== */
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--spacing-4, 16px);
}

/* ===== 内容卡片 ===== */
.content-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(31, 41, 35, 0.06));
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-5, 20px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.card-title {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0;
}

.card-link {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-expert, #b85c38);
  text-decoration: none;
  font-weight: var(--font-weight-medium, 500);
}

.card-link:hover {
  text-decoration: underline;
}

/* ===== 问题列表 ===== */
.question-list {
  padding: var(--spacing-3, 12px);
}

.question-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.question-item:last-child {
  border-bottom: none;
}

.question-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
}

.question-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-expert, #b85c38);
  color: var(--color-text-inverse, #ffffff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-semibold, 600);
  flex-shrink: 0;
}

.question-content {
  flex: 1;
  min-width: 0;
}

.question-title {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.question-meta {
  display: flex;
  gap: var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

/* ===== 快捷操作 ===== */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px);
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-2, 8px);
  padding: var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-lg, 12px);
  text-decoration: none;
  transition: all var(--transition-fast, 150ms ease);
}

.quick-action:hover {
  background: rgba(184, 92, 56, 0.08);
  transform: translateY(-2px);
}

.action-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-base, 8px);
  color: var(--color-expert, #b85c38);
}

.action-icon svg {
  width: 20px;
  height: 20px;
}

.quick-action span {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
}

/* ===== 知识列表 ===== */
.knowledge-list {
  padding: var(--spacing-3, 12px);
}

.knowledge-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.knowledge-item:last-child {
  border-bottom: none;
}

.knowledge-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
}

.knowledge-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-base, 8px);
  background: rgba(184, 92, 56, 0.1);
  color: var(--color-expert, #b85c38);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.knowledge-icon svg {
  width: 20px;
  height: 20px;
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

.knowledge-meta {
  display: flex;
  gap: var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: var(--font-size-2xl, 30px);
  }
}
</style>
