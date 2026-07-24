<template>
  <div class="knowledge-detail-page">
    <div class="container">
      <!-- 返回列表 -->
      <BackBar to="/knowledge" label="返回列表" />

      <!-- 加载状态 -->
      <el-skeleton v-if="loading" :rows="10" animated />

      <!-- 知识详情 -->
      <div v-else-if="knowledge.knowledgeId" class="knowledge-detail">
        <article class="knowledge-article">
          <!-- 标题区 -->
          <header class="article-header">
            <div class="article-category">{{ knowledge.categoryName || '农业知识' }}</div>
            <h1 class="article-title">{{ knowledge.title }}</h1>
            <div class="article-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ knowledge.ownRealName || knowledge.ownName || '专家' }}
              </span>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(knowledge.createTime) }}
              </span>
            </div>
          </header>

          <!-- 文章图片 -->
          <div v-if="knowledge.picPath" class="article-image">
            <img :src="knowledge.picPath" :alt="knowledge.title">
          </div>

          <!-- 文章内容 -->
          <div class="article-content" v-html="knowledge.content"></div>

          <!-- 操作按钮 -->
          <div class="article-actions" v-if="canEdit">
            <el-button type="primary" @click="handleEdit">
              <el-icon><Edit /></el-icon>
              编辑文章
            </el-button>
            <el-button type="danger" @click="handleDelete">
              <el-icon><Delete /></el-icon>
              删除文章
            </el-button>
          </div>
        </article>

        <!-- 评论区 -->
        <div class="comments-section">
          <h3 class="section-title">评论 ({{ comments.length }})</h3>

          <!-- 发表评论 -->
          <div v-if="userStore.isLoggedIn()" class="comment-form">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="发表您的看法..."
              maxlength="500"
              show-word-limit
            />
            <el-button
              type="primary"
              @click="handleSubmitComment"
              :loading="submitting"
              :disabled="!commentContent.trim()"
            >
              发表评论
            </el-button>
          </div>
          <div v-else class="login-tip">
            请 <el-button link type="primary" @click="handleLogin">登录</el-button> 后发表评论
          </div>

          <!-- 评论列表 -->
          <div v-if="comments.length > 0" class="comment-list">
            <div v-for="comment in comments" :key="comment.discussId" class="comment-item">
              <div class="comment-header">
                <span class="comment-author">{{ comment.ownRealName || comment.ownName }}</span>
                <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <el-button
                v-if="canDeleteComment(comment)"
                link
                type="danger"
                size="small"
                @click="handleDeleteComment(comment)"
              >
                删除
              </el-button>
            </div>
          </div>
          <el-empty v-else description="暂无评论，快来发表第一条吧！" />
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-else description="文章不存在或已被删除" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Calendar, View, Edit, Delete } from '@element-plus/icons-vue'
import BackBar from '@/components/BackBar.vue'
import {
  getKnowledgeDetail,
  getKnowledgeComments,
  addKnowledgeComment,
  deleteKnowledgeComment,
  deleteKnowledge
} from '@/api/knowledge'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const knowledge = ref({})
const comments = ref([])
const commentContent = ref('')

// 是否可以编辑/删除文章：仅文章作者本人或管理员可见操作按钮
// 此前仅按角色(expert/admin)判断 → 任意专家都能看到并尝试删除/编辑他人文章（越权）
const canEdit = computed(() => {
  if (!userStore.isLoggedIn()) return false
  if (userStore.hasRole(['admin'])) return true
  return knowledge.value.ownName === userStore.userInfo?.userName
})

// 是否可以删除评论
const canDeleteComment = (comment) => {
  if (!userStore.isLoggedIn()) return false
  // 评论作者或知识发布者可以删除
  return comment.ownName === userStore.userInfo?.userName ||
         knowledge.value.ownName === userStore.userInfo?.userName ||
         userStore.hasRole(['admin'])
}

// 格式化日期
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

// 登录
const handleLogin = () => {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

// 编辑文章
const handleEdit = () => {
  router.push(`/knowledge/publish?id=${knowledge.value.knowledgeId}`)
}

// 删除文章
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这篇文章吗？删除后无法恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteKnowledge(knowledge.value.knowledgeId)
    ElMessage.success('删除成功')
    router.push('/knowledge')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 发表评论
const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await addKnowledgeComment(knowledge.value.knowledgeId, {
      content: commentContent.value.trim()
    })
    ElMessage.success('评论成功')
    commentContent.value = ''
    await loadComments()
  } catch (error) {
    ElMessage.error(error.message || '评论失败')
  } finally {
    submitting.value = false
  }
}

// 删除评论
const handleDeleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteKnowledgeComment(comment.discussId)
    ElMessage.success('删除成功')
    await loadComments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 加载知识详情
const loadKnowledge = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeDetail(route.params.id)
    knowledge.value = res.data || {}
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载评论列表
const loadComments = async () => {
  try {
    const res = await getKnowledgeComments(route.params.id)
    comments.value = res.data || []
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

onMounted(async () => {
  await Promise.all([loadKnowledge(), loadComments()])
})
</script>

<style scoped>
.knowledge-detail-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
}

.back-btn {
  margin-bottom: var(--spacing-4, 16px);
  font-size: var(--font-size-base, 16px);
}

/* ===== 文章内容 ===== */
.knowledge-detail {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-8, 32px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.article-header {
  margin-bottom: var(--spacing-6, 24px);
  padding-bottom: var(--spacing-6, 24px);
  border-bottom: 1px solid var(--color-border, #e5e0d8);
}

.article-category {
  display: inline-block;
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-primary, #2d5a3d);
  background: rgba(45, 90, 61, 0.1);
  border-radius: var(--radius-full, 9999px);
  margin-bottom: var(--spacing-3, 12px);
}

.article-title {
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-4, 16px);
  line-height: 1.4;
}

.article-meta {
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

.article-image {
  margin-bottom: var(--spacing-6, 24px);
  border-radius: var(--radius-base, 8px);
  overflow: hidden;
}

.article-image img {
  width: 100%;
  height: auto;
  display: block;
}

.article-content {
  font-size: var(--font-size-base, 16px);
  line-height: 1.8;
  color: var(--color-text-primary, #1f2923);
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-base, 8px);
  margin: var(--spacing-4, 16px) 0;
}

.article-content :deep(p) {
  margin: var(--spacing-3, 12px) 0;
}

.article-actions {
  margin-top: var(--spacing-6, 24px);
  padding-top: var(--spacing-6, 24px);
  border-top: 1px solid var(--color-border, #e5e0d8);
  display: flex;
  gap: var(--spacing-3, 12px);
}

/* ===== 评论区 ===== */
.comments-section {
  margin-top: var(--spacing-6, 24px);
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-6, 24px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.section-title {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-4, 16px);
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3, 12px);
  margin-bottom: var(--spacing-6, 24px);
}

.login-tip {
  color: var(--color-text-tertiary, #6b7280);
  margin-bottom: var(--spacing-4, 16px);
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
}

.comment-item {
  padding: var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2, 8px);
}

.comment-author {
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-primary, #2d5a3d);
}

.comment-time {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.comment-content {
  color: var(--color-text-primary, #1f2923);
  line-height: 1.6;
  white-space: pre-wrap;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .article-title {
    font-size: var(--font-size-2xl, 28px);
  }

  .knowledge-detail,
  .comments-section {
    padding: var(--spacing-5, 20px);
  }
}
</style>
