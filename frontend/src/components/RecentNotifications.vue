<template>
  <div class="content-card recent-notifications">
    <div class="card-header">
      <h3 class="card-title">{{ title }}</h3>
      <span v-if="unreadCount > 0" class="unread-tip">{{ unreadCount }} 条未读</span>
    </div>

    <div v-loading="loading" class="notif-body">
      <div v-if="!loading && messages.length === 0" class="empty-state">
        <el-empty :image-size="64" description="暂无新动态" />
      </div>

      <div v-else class="notif-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="notif-item"
          :class="{ unread: msg.isRead === 0, clickable: msg.linkUrl }"
          @click="handleClick(msg)"
        >
          <div class="notif-item-main">
            <div class="notif-item-top">
              <el-tag size="small" :type="tagType(msg.category)" effect="light" class="notif-tag">
                {{ tagLabel(msg.category) }}
              </el-tag>
              <span v-if="msg.isRead === 0" class="unread-dot"></span>
            </div>
            <div class="notif-item-title">{{ msg.title }}</div>
            <div v-if="msg.content" class="notif-item-content">{{ msg.content }}</div>
          </div>
          <div class="notif-item-time">{{ formatTime(msg.createTime) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMessages, getUnreadCount } from '@/api/message'

const props = defineProps({
  title: { type: String, default: '最新动态' },
  limit: { type: Number, default: 5 }
})

const router = useRouter()
const messages = ref([])
const unreadCount = ref(0)
const loading = ref(false)

const tagLabel = (cat) => {
  const map = { order: '订单', reserve: '预约', finance: '融资', question: '问答', system: '系统' }
  return map[cat] || '通知'
}
const tagType = (cat) => {
  const map = { order: 'warning', reserve: 'success', finance: 'primary', question: 'info', system: '' }
  return map[cat] || ''
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return d.toLocaleDateString('zh-CN')
}

const handleClick = (msg) => {
  if (msg.linkUrl) router.push(msg.linkUrl)
}

const load = async () => {
  loading.value = true
  try {
    const [listRes, countRes] = await Promise.all([
      getMessages({ page: 1, pageSize: props.limit }),
      getUnreadCount()
    ])
    if (listRes.code === 200) messages.value = listRes.data?.records || []
    if (countRes.code === 200) unreadCount.value = countRes.data?.count || 0
  } catch (e) {
    // 静默失败，不阻塞工作台
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.recent-notifications {
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

.unread-tip {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-error, #b85c38);
  font-weight: var(--font-weight-medium, 500);
}

.notif-body {
  min-height: 120px;
}

.empty-state {
  padding: var(--spacing-6, 24px) 0;
}

.notif-list {
  padding: var(--spacing-3, 12px);
}

.notif-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border-radius: var(--radius-base, 8px);
  transition: background var(--transition-fast, 150ms ease);
}

.notif-item + .notif-item {
  border-top: 1px solid var(--color-divider, #edebe6);
  border-radius: 0;
}

.notif-item.clickable {
  cursor: pointer;
}

.notif-item.clickable:hover {
  background: var(--color-bg-secondary, #f7f5f0);
}

.notif-item.unread {
  background: rgba(45, 90, 61, 0.04);
}

.notif-item-main {
  flex: 1;
  min-width: 0;
}

.notif-item-top {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
  margin-bottom: 6px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-error, #b85c38);
  flex-shrink: 0;
}

.notif-item-title {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: 4px;
  line-height: 1.4;
}

.notif-item-content {
  font-size: var(--font-size-xs, 13px);
  color: var(--color-text-secondary, #4a5249);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notif-item-time {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  white-space: nowrap;
  flex-shrink: 0;
  padding-top: 2px;
}
</style>
