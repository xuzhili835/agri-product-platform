<template>
  <div class="buyer-dashboard">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 内容区域 -->
    <template v-else>
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">{{ greeting }}，{{ userStore.displayName() }}</h1>
      <p class="page-subtitle">欢迎来到买家工作台，探索优质农产品，轻松采购</p>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-grid">
      <div class="stat-card primary">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="9" cy="21" r="1"/>
            <circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.totalOrders }}</div>
          <div class="stat-label">累计订单</div>
        </div>
      </div>

      <div class="stat-card success">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
            <circle cx="12" cy="12" r="2" fill="currentColor"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">¥ {{ formatMoney(stats.totalSpent) }}</div>
          <div class="stat-label">累计消费</div>
        </div>
      </div>

      <div class="stat-card warning">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M9 20a1 1 0 100 2 1 1 0 000-2zM7 4h10a2 2 0 012 2v1H5V6a2 2 0 012-2zM5 8h14v10a2 2 0 01-2 2H7a2 2 0 01-2-2V8z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.cartCount }}</div>
          <div class="stat-label">购物车商品</div>
          <div class="stat-action">
            <router-link to="/buyer/cart">去结算</router-link>
          </div>
        </div>
      </div>

      <div class="stat-card info">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="8" r="3"/>
            <path d="M8 21v-5a4 4 0 014-4M8 21h8M12 12v5M8 10l2-2M16 10l-2-2"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.expertCount }}</div>
          <div class="stat-label">专家咨询</div>
          <div class="stat-action">
            <router-link to="/buyer/expert">继续咨询</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="content-grid">
      <!-- 我的购物车（真实数据，非"推荐"排序） -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">我的购物车</h3>
          <router-link to="/buyer/cart" class="card-link">去结算</router-link>
        </div>
        <div class="product-grid">
          <div v-for="item in cartItems" :key="item.cartId" class="product-card" @click="goToProduct(item)">
            <div class="product-image">
              <img v-if="item.picPath" :src="item.picPath" :alt="item.title" />
              <div v-else class="image-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 12c0 4.97-4.03 9-9 9s-9-4.03-9-9 4.03-9 9-9 9 4.03 9 9z"/>
                  <path d="M12 7v5l3 3"/>
                </svg>
              </div>
            </div>
            <div class="product-info">
              <div class="product-name">{{ item.title }}</div>
              <div class="product-desc">数量 x{{ item.count }}</div>
              <div class="product-price">¥{{ item.price }}</div>
            </div>
          </div>
          <div v-if="!cartItems.length" class="cart-empty">
            <p>购物车还是空的，快去选购吧~</p>
            <router-link to="/buyer/market" class="card-link">去逛市场</router-link>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">快捷操作</h3>
        </div>
        <div class="quick-actions">
          <router-link to="/buyer/market" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="11" cy="11" r="8"/>
                <path d="m21 21-4.35-4.35M11 8v6M8 11h6"/>
              </svg>
            </div>
            <span>逛市场</span>
          </router-link>
          <router-link to="/buyer/cart" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M9 20a1 1 0 100 2 1 1 0 000-2zM7 4h10a2 2 0 012 2v1H5V6a2 2 0 012-2zM5 8h14v10a2 2 0 01-2 2H7a2 2 0 01-2-2V8z"/>
              </svg>
            </div>
            <span>购物车</span>
          </router-link>
          <router-link to="/buyer/orders" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
              </svg>
            </div>
            <span>我的订单</span>
          </router-link>
          <router-link to="/buyer/expert" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="8" r="3"/>
                <path d="M8 21v-5a4 4 0 014-4M8 21h8M12 12v5M8 10l2-2M16 10l-2-2"/>
              </svg>
            </div>
            <span>专家咨询</span>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 最新动态（真实站内通知，来自 tb_message） -->
    <RecentNotifications title="最新动态" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Top } from '@element-plus/icons-vue'
import { getOrders } from '@/api/order'
import { getCartList } from '@/api/cart'
import RecentNotifications from '@/components/RecentNotifications.vue'

const router = useRouter()
const userStore = useUserStore()

// 统计数据
const stats = ref({
  totalOrders: 0,
  totalSpent: 0,
  cartCount: 0,
  expertCount: 0
})

// 购物车商品（首页「我的购物车」展示前 4 项，真实数据）
const cartItems = ref([])

// 加载状态
const loading = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 格式化金额
const formatMoney = (amount) => {
  if (!amount) return '0'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
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

// 获取订单状态样式
const getStatusClass = (status) => {
  const statusMap = {
    0: 'pending',
    1: 'processing',
    2: 'success'
  }
  return statusMap[status] || 'pending'
}

// 获取订单状态文本
const getStatusText = (status) => {
  const statusMap = {
    0: '待确认',
    1: '进行中',
    2: '已完成'
  }
  return statusMap[status] || '未知'
}

// 跳转到产品详情
const goToProduct = (product) => {
  router.push(`/buyer/market?productId=${product.productId}`)
}

// 加载仪表板数据
const loadDashboardData = async () => {
  loading.value = true
  try {
    // 加载订单用于统计（累计订单/累计消费），概览页不再展示最近订单列表
    const orderRes = await getOrders({ page: 1, pageSize: 5 })
    if (orderRes.code === 200) {
      const orders = orderRes.data.records || []
      stats.value.totalOrders = orderRes.data.total || 0
      stats.value.totalSpent = orders.reduce((sum, order) => sum + (order.totalPrice || 0), 0)
    }

    // 加载购物车：既用于统计计数，也用于「我的购物车」卡片展示前 4 项
    const cartRes = await getCartList()
    if (cartRes.code === 200) {
      const list = cartRes.data || []
      stats.value.cartCount = list.length
      cartItems.value = list.slice(0, 4)
    }
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.buyer-dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

.loading-state {
  padding: var(--spacing-6, 24px);
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
  background: rgba(91, 138, 158, 0.1);
  color: var(--color-buyer, #5b8a9e);
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
  background: rgba(184, 92, 56, 0.1);
  color: var(--color-error, #b85c38);
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
  color: var(--color-buyer, #5b8a9e);
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
  color: var(--color-buyer, #5b8a9e);
  text-decoration: none;
  font-weight: var(--font-weight-medium, 500);
}

.card-link:hover {
  text-decoration: underline;
}

/* ===== 商品网格 ===== */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px);
}

.product-card {
  cursor: pointer;
  transition: transform var(--transition-fast, 150ms ease);
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-image {
  aspect-ratio: 1;
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
  margin-bottom: var(--spacing-2, 8px);
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  width: 32px;
  height: 32px;
}

.product-name {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-desc {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  margin-bottom: var(--spacing-1, 4px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-price {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-error, #b85c38);
}

.cart-empty {
  grid-column: 1 / -1;
  padding: var(--spacing-6, 24px);
  text-align: center;
  color: var(--color-text-tertiary, #6b7280);
  font-size: var(--font-size-sm, 14px);
}

.cart-empty p {
  margin: 0 0 var(--spacing-2, 8px);
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
  background: rgba(91, 138, 158, 0.08);
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
  color: var(--color-buyer, #5b8a9e);
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

/* ===== 订单列表 ===== */
.order-list {
  padding: var(--spacing-3, 12px);
}

.order-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-4, 16px);
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.order-item:last-child {
  border-bottom: none;
}

.order-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
}

.order-info {
  flex: 1;
  min-width: 0;
}

.order-products {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
}

.order-meta {
  display: flex;
  gap: var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.order-amount {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
}

.order-status {
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  border-radius: var(--radius-full, 9999px);
  flex-shrink: 0;
}

.order-status.pending {
  background: rgba(201, 166, 97, 0.1);
  color: var(--color-warning, #c9a661);
}

.order-status.success {
  background: rgba(74, 124, 89, 0.1);
  color: var(--color-success, #4a7c59);
}

.order-status.processing {
  background: rgba(91, 138, 158, 0.1);
  color: var(--color-buyer, #5b8a9e);
}

.empty-orders {
  padding: var(--spacing-8, 32px);
  text-align: center;
  color: var(--color-text-tertiary, #6b7280);
}

.empty-orders p {
  margin: 0 0 var(--spacing-4, 16px);
}

.empty-orders a {
  color: var(--color-buyer, #5b8a9e);
  text-decoration: none;
  font-weight: var(--font-weight-medium, 500);
}

.empty-orders a:hover {
  text-decoration: underline;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: var(--font-size-2xl, 30px);
  }
}
</style>
