<template>
  <div class="question-detail-page">
    <div class="container">
      <!-- 返回列表 -->
      <BackBar to="/question" label="返回列表" />

      <!-- 加载状态 -->
      <el-skeleton v-if="loading" :rows="10" animated />

      <!-- 问题详情 -->
      <div v-else-if="question.id" class="question-detail">
        <!-- 问题内容 -->
        <article class="question-article">
          <header class="question-header">
            <div class="status-badge" :class="`status-${question.status}`">
              {{ getStatusText(question.status) }}
            </div>
            <h1 class="question-title">{{ question.title }}</h1>
            <div class="question-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                提问者：{{ displayName(question.questionerRealName, question.questioner) }}
              </span>
              <span class="meta-item" v-if="question.expertRealName || question.expertName">
                <el-icon><ChatDotRound /></el-icon>
                专家：{{ displayName(question.expertRealName, question.expertName) }}
              </span>
              <span class="meta-item" v-if="question.plantName">
                <el-icon><ChatDotRound /></el-icon>
                农作物：{{ question.plantName }}
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
          </header>

          <div class="question-content">
            {{ question.question }}
          </div>

          <!-- 操作按钮：结束对话 / 删除问题（仅提问者本人） -->
          <div class="question-actions" v-if="canClose || canDelete">
            <el-button
              v-if="canClose"
              type="warning"
              plain
              @click="handleCloseQuestion"
              :disabled="question.status === 2"
            >
              <el-icon><Close /></el-icon>
              {{ question.status === 2 ? '对话已结束' : '结束对话' }}
            </el-button>
            <el-button v-if="canDelete" type="danger" plain @click="handleDeleteQuestion">
              <el-icon><Delete /></el-icon>
              删除问题
            </el-button>
          </div>
        </article>

        <!-- 对话记录（多轮追问） -->
        <div class="conversation">
          <h3 class="section-title">
            对话记录 <span class="reply-count">（{{ replies.length }} 条）</span>
          </h3>

          <div v-if="replies.length > 0" class="reply-list">
            <div
              v-for="reply in replies"
              :key="reply.id"
              class="reply-item"
              :class="`role-${reply.authorRole}`"
            >
              <div class="reply-avatar">
                {{ (reply.authorRealName || reply.authorUserName || '?').charAt(0) }}
              </div>
              <div class="reply-body">
                <div class="reply-head">
                  <span class="reply-author">
                    {{ displayName(reply.authorRealName, reply.authorUserName) }}
                  </span>
                  <el-tag size="small" :type="reply.authorRole === 'expert' ? 'success' : 'warning'">
                    {{ reply.authorRole === 'expert' ? '专家回答' : '追问' }}
                  </el-tag>
                  <span class="reply-time">{{ formatDate(reply.createTime) }}</span>
                </div>
                <div class="reply-content">{{ reply.content }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else :description="canReply ? '还没有回复，快来开始对话吧' : '暂无回复'" />

          <!-- 回复输入框 -->
          <div v-if="canReply" class="reply-form">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="3"
              :placeholder="replyPlaceholder"
              maxlength="1000"
              show-word-limit
            />
            <el-button
              type="primary"
              @click="handleSubmitReply"
              :loading="submitting"
              :disabled="!replyContent.trim()"
            >
              {{ isExpert ? '提交回答' : '提交追问' }}
            </el-button>
          </div>
          <div v-else-if="question.status === 2" class="closed-tip">
            <el-alert title="对话已结束，无法继续回复" type="info" :closable="false" />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-else description="问题不存在或已被删除" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Calendar, Close, Delete, Phone, ChatDotRound } from '@element-plus/icons-vue'
import BackBar from '@/components/BackBar.vue'
import {
  getQuestionDetail,
  closeQuestion,
  deleteQuestion,
  getQuestionReplies,
  addQuestionReply
} from '@/api/question'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const question = ref({})
const replies = ref([])
const replyContent = ref('')

const currentUserName = computed(() => userStore.userInfo?.userName || '')

const isQuestioner = computed(() => question.value.questioner === currentUserName.value)
const isExpert = computed(
  () => !!question.value.expertName && question.value.expertName === currentUserName.value
)
const isAdmin = computed(
  () => !!userStore.userInfo && userStore.hasRole(['admin'])
)

// 结束对话：仅提问者本人
const canClose = computed(() => isQuestioner.value)
// 删除：提问者本人或管理员
const canDelete = computed(() => isQuestioner.value || isAdmin.value)
// 可回复：登录 且 (提问者或被指派专家) 且 未关闭
const canReply = computed(() => {
  if (!userStore.isLoggedIn()) return false
  if (question.value.status === 2) return false
  return isQuestioner.value || isExpert.value
})
const replyPlaceholder = computed(() =>
  isExpert.value ? '请输入专业回答…' : '请输入追问内容，向专家继续请教…'
)

// 优先显示真实姓名，缺省回退到用户名
const displayName = (realName, userName) => realName || userName || ''

// 状态文本（后端整数：0未回答 1已回答 2已关闭）
const getStatusText = (status) => {
  const map = { 0: '待回答', 1: '已回答', 2: '已关闭' }
  return map[status] || '未知'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 同时加载问题与对话回复
const loadAll = async () => {
  loading.value = true
  try {
    const [qRes, rRes] = await Promise.all([
      getQuestionDetail(route.params.id),
      getQuestionReplies(route.params.id)
    ])
    question.value = qRes.data || {}
    replies.value = rRes.data || []
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 结束对话（原"关闭问题"）
const handleCloseQuestion = async () => {
  try {
    await ElMessageBox.confirm('结束对话后将无法继续回复，确定吗？', '结束对话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await closeQuestion(question.value.id)
    ElMessage.success('对话已结束')
    await loadAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除问题
const handleDeleteQuestion = async () => {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确定删除这个问题吗？', '删除问题', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteQuestion(question.value.id)
    ElMessage.success('删除成功')
    router.push('/question')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 提交追问/回答
const handleSubmitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  submitting.value = true
  try {
    await addQuestionReply(question.value.id, { content: replyContent.value.trim() })
    replyContent.value = ''
    await loadAll()
  } catch (error) {
    ElMessage.error(error.message || '回复失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.question-detail-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
}

/* ===== 问题内容 ===== */
.question-detail {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

.question-article {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-8, 32px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.question-header {
  margin-bottom: var(--spacing-6, 24px);
  padding-bottom: var(--spacing-6, 24px);
  border-bottom: 1px solid var(--color-border, #e5e0d8);
}

.status-badge {
  display: inline-block;
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  border-radius: var(--radius-full, 9999px);
  margin-bottom: var(--spacing-3, 12px);
}

.status-0 {
  color: #e6a23c;
  background: #fdf6ec;
}

.status-1 {
  color: #67c23a;
  background: #f0f9ff;
}

.status-2 {
  color: #909399;
  background: #f4f4f5;
}

.question-title {
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-4, 16px);
  line-height: 1.4;
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

.question-content {
  font-size: var(--font-size-lg, 20px);
  line-height: 1.8;
  color: var(--color-text-primary, #1f2923);
  white-space: pre-wrap;
}

.question-actions {
  margin-top: var(--spacing-6, 24px);
  padding-top: var(--spacing-6, 24px);
  border-top: 1px solid var(--color-border, #e5e0d8);
  display: flex;
  gap: var(--spacing-3, 12px);
}

/* ===== 对话记录 ===== */
.conversation {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-6, 24px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.section-title {
  font-size: var(--font-size-xl, 24px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-4, 16px);
}

.reply-count {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-normal, 400);
  color: var(--color-text-tertiary, #6b7280);
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
  margin-bottom: var(--spacing-6, 24px);
}

.reply-item {
  display: flex;
  gap: var(--spacing-3, 12px);
}

.reply-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full, 9999px);
  background: var(--color-primary, #2d5a3d);
  color: var(--color-text-inverse, #ffffff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  flex-shrink: 0;
}

.role-expert .reply-avatar {
  background: var(--color-expert, #b85c38);
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-head {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
  margin-bottom: var(--spacing-1, 4px);
}

.reply-author {
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
}

.reply-time {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  margin-left: auto;
}

.reply-content {
  font-size: var(--font-size-base, 16px);
  line-height: 1.6;
  color: var(--color-text-primary, #1f2923);
  white-space: pre-wrap;
  background: var(--color-bg-secondary, #f7f5f0);
  padding: var(--spacing-3, 12px);
  border-radius: var(--radius-base, 8px);
}

.reply-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3, 12px);
  padding-top: var(--spacing-4, 16px);
  border-top: 1px solid var(--color-border, #e5e0d8);
}

.closed-tip {
  margin-top: var(--spacing-4, 16px);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .question-title {
    font-size: var(--font-size-2xl, 28px);
  }

  .question-article,
  .conversation {
    padding: var(--spacing-5, 20px);
  }
}
</style>
