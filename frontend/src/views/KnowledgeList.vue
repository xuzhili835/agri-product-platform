<template>
  <div class="knowledge-list-page">
    <BackBar to="/" />
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">农业知识库</h1>
      <p class="page-subtitle">专业农业知识分享，助您科学种植</p>
    </div>

    <!-- 筛选区（仅保留搜索；原分类筛选无数据库支撑，已移除） -->
    <div class="filter-section">
      <div class="filter-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索知识文章..."
          :prefix-icon="'Search'"
          size="large"
          class="search-input"
        />
      </div>
    </div>

    <!-- 知识列表 -->
    <div class="knowledge-grid">
      <div v-for="knowledge in filteredKnowledge" :key="knowledge.id" class="knowledge-card" @click="handleViewKnowledge(knowledge)">
        <div class="knowledge-image">
          <img v-if="knowledge.image" :src="knowledge.image" :alt="knowledge.title">
          <div v-else class="image-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
            </svg>
          </div>
        </div>
        <div class="knowledge-content">
          <h3 class="knowledge-title">{{ knowledge.title }}</h3>
          <p class="knowledge-excerpt">{{ knowledge.excerpt }}</p>
          <div class="knowledge-meta">
            <span class="meta-author">{{ knowledge.author }}</span>
            <span class="meta-time">{{ knowledge.time }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div class="load-more" v-if="hasMore">
      <el-button @click="loadMore" :loading="loading">加载更多</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { getKnowledgeList } from '@/api/knowledge'
import BackBar from '@/components/BackBar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const searchKeyword = ref('')
const loading = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(9)

const knowledgeList = ref([])
const total = ref(0)

// 加载知识列表
const loadKnowledge = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    const res = await getKnowledgeList(params)
    if (res.code === 200) {
      const records = res.data.records || []
      knowledgeList.value = records.map(k => ({
        id: k.knowledgeId || k.id,
        title: k.title,
        excerpt: stripHtml(k.content).substring(0, 100),
        image: k.picPath || null,
        author: k.ownRealName || k.ownName || '专家',
        time: formatDate(k.createTime),
        status: k.status
      }))
      total.value = res.data.total || 0
      hasMore.value = knowledgeList.value.length >= pageSize.value
    }
  } catch (error) {
    console.error('加载知识列表失败:', error)
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

// 去除 HTML 标签，用于摘要纯文本展示（避免列表里出现 <p> 等标签）
const stripHtml = (html) => {
  if (!html) return ''
  return html
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const filteredKnowledge = computed(() => {
  let result = knowledgeList.value

  // 只显示已发布的知识
  result = result.filter(item => item.status === 1)

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(item =>
      item.title.toLowerCase().includes(keyword) ||
      item.excerpt.toLowerCase().includes(keyword)
    )
  }

  return result
})

const handleViewKnowledge = (knowledge) => {
  router.push({ path: `/knowledge/${knowledge.id}`, query: { from: route.fullPath } })
}

const loadMore = () => {
  currentPage.value++
  loadKnowledge()
}

// 监听搜索变化
watch(searchKeyword, () => {
  // 搜索是前端的，不需要重新加载
})

onMounted(() => {
  loadKnowledge()
})
</script>

<style scoped>
.knowledge-list-page {
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

/* ===== 筛选区 ===== */
.filter-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-6, 24px);
  gap: var(--spacing-4, 16px);
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: var(--spacing-2, 8px);
  flex-wrap: wrap;
}

.filter-tab {
  padding: var(--spacing-2, 8px) var(--spacing-4, 16px);
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-secondary, #4a5249);
  background: transparent;
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
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

.search-input {
  width: 300px;
}

/* ===== 知识网格 ===== */
.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-5, 20px);
}

.knowledge-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.knowledge-card:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(31, 41, 35, 0.08));
  transform: translateY(-4px);
}

.knowledge-image {
  position: relative;
  aspect-ratio: 16 / 10;
  background: var(--color-bg-secondary, #f7f5f0);
  overflow: hidden;
}

.knowledge-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-base, 250ms ease);
}

.knowledge-card:hover .knowledge-image img {
  transform: scale(1.05);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary, #6b7280);
}

.image-placeholder svg {
  width: 48px;
  height: 48px;
}

.knowledge-category {
  position: absolute;
  bottom: var(--spacing-3, 12px);
  left: var(--spacing-3, 12px);
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-inverse, #ffffff);
  background: rgba(45, 90, 61, 0.85);
  backdrop-filter: blur(4px);
  border-radius: var(--radius-full, 9999px);
}

.knowledge-content {
  padding: var(--spacing-4, 16px);
}

.knowledge-title {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.knowledge-excerpt {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
  line-height: 1.5;
  margin: 0 0 var(--spacing-3, 12px);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.knowledge-meta {
  display: flex;
  gap: var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.meta-author {
  color: var(--color-primary, #2d5a3d);
  font-weight: var(--font-weight-medium, 500);
}

/* ===== 加载更多 ===== */
.load-more {
  text-align: center;
  margin-top: var(--spacing-8, 32px);
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .knowledge-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .search-input {
    width: 240px;
  }
}

@media (max-width: 768px) {
  .knowledge-grid {
    grid-template-columns: 1fr;
  }

  .search-input {
    width: 100%;
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .page-title {
    font-size: var(--font-size-3xl, 38px);
  }
}
</style>
