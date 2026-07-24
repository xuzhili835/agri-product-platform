<template>
  <div class="farmer-dashboard">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">{{ greeting }}，{{ userStore.displayName() }}</h1>
      <p class="page-subtitle">这是您的农户工作台，可以管理您的农产品、订单和融资申请</p>
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
          <div class="stat-value">¥ {{ stats.monthlySales.toLocaleString() }}</div>
          <div class="stat-label">本月销售额</div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>实时更新</span>
          </div>
        </div>
      </div>

      <div class="stat-card success">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="9" cy="21" r="1"/>
            <circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.productCount }}</div>
          <div class="stat-label">在售商品</div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>持续更新</span>
          </div>
        </div>
      </div>

      <div class="stat-card warning">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.pendingOrders }}</div>
          <div class="stat-label">待处理订单</div>
          <div class="stat-action">
            <router-link to="/farmer/orders">查看订单</router-link>
          </div>
        </div>
      </div>

      <div class="stat-card info">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 10v6M2 10l2-4h16l2 4M12 2v3M12 10a3 3 0 100 6 3 3 0 000-6z"/>
            <path d="M12 16c-3 0-5 1.5-6 3v2h12v-2c-1-1.5-3-3-6-3z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.financeCount }}</div>
          <div class="stat-label">融资进度</div>
          <div class="stat-action">
            <router-link to="/farmer/finance">查看详情</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="content-grid">
      <!-- 最近订单 -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">最近订单</h3>
          <router-link to="/farmer/orders" class="card-link">查看全部</router-link>
        </div>
        <div class="order-list">
          <div v-for="order in recentOrders" :key="order.id" class="order-item">
            <div class="order-info">
              <div class="order-product">{{ order.product }}</div>
              <div class="order-meta">
                <span class="order-buyer">买家：{{ order.buyer }}</span>
                <span class="order-price">¥{{ order.totalPrice }}</span>
                <span class="order-id">#{{ order.id }}</span>
                <span class="order-time">{{ order.time }}</span>
              </div>
            </div>
            <div class="order-status" :class="order.statusClass">
              {{ order.status }}
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">快捷操作</h3>
        </div>
        <div class="quick-actions">
          <router-link to="/farmer/products" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 8v8M8 12h8"/>
              </svg>
            </div>
            <span>发布商品</span>
          </router-link>
          <router-link to="/farmer/finance" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
                <circle cx="12" cy="12" r="2" fill="currentColor"/>
              </svg>
            </div>
            <span>申请融资</span>
          </router-link>
          <router-link to="/farmer/expert" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="8" r="3"/>
                <path d="M8 21v-5a4 4 0 014-4M8 21h8M12 12v5M8 10l2-2M16 10l-2-2"/>
              </svg>
            </div>
            <span>咨询专家</span>
          </router-link>
          <router-link to="/farmer/profile" class="quick-action">
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

    <!-- 最新动态（真实站内通知，来自 tb_message） -->
    <RecentNotifications title="最新动态" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Top } from '@element-plus/icons-vue'
import { getProducts } from '@/api/product'
import { getReceivedOrders } from '@/api/order'
import { getMyFinanceApplications } from '@/api/finance'
import RecentNotifications from '@/components/RecentNotifications.vue'

const userStore = useUserStore()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 统计数据
const stats = ref({
  monthlySales: 0,
  productCount: 0,
  pendingOrders: 0,
  financeCount: 0
})

// 最近订单
const recentOrders = ref([])
const loading = ref(false)

// 加载Dashboard数据
const loadDashboardData = async () => {
  loading.value = true
  try {
    // 获取商品数量
    const productRes = await getProducts({ page: 1, pageSize: 1 })
    if (productRes.code === 200) {
      stats.value.productCount = productRes.data.total || 0
    }

    // 获取订单列表和统计
    // 注意：农户要用「收到的订单」（别人买了我商品的订单），而非 getOrders（那是买家视角、按 purchase.ownName=自己 过滤，
    // 农户不能下单故恒为空）——此前销售额/待处理/最近订单全部为空即因此而来
    const orderRes = await getReceivedOrders({ page: 1, pageSize: 10 })
    if (orderRes.code === 200) {
      // 处理分页数据
      const orderData = orderRes.data
      const orders = orderData.records || []

      stats.value.pendingOrders = orders.filter(o => o.purchaseStatus === 1 || o.purchaseStatus === 2).length

      // 计算本月销售额（已完成订单）
      const now = new Date()
      const currentMonth = now.getMonth()
      const currentYear = now.getFullYear()
      stats.value.monthlySales = orders
        .filter(o => {
          if (o.purchaseStatus !== 4) return false // 仅计算已完成订单
          const orderDate = new Date(o.createTime)
          return orderDate.getMonth() === currentMonth && orderDate.getFullYear() === currentYear
        })
        .reduce((sum, o) => sum + (o.totalPrice ? Number(o.totalPrice) : 0), 0)

      // 格式化最近订单数据：展示商品名(多个拼接)、买家姓名、金额；订单号降为次要信息
      recentOrders.value = orders.slice(0, 5).map(order => {
        const statusText = getStatusText(order.purchaseStatus)
        const productNames = (order.items || [])
          .map(i => i.productName)
          .filter(Boolean)
        const product = productNames.length > 2
          ? productNames.slice(0, 2).join('、') + ` 等${productNames.length}种`
          : (productNames.join('、') || '—')
        return {
          id: order.purchaseId || order.id,
          product,
          // 对方=买家（这是农户收到的订单）；优先真实姓名，回退登录账号
          buyer: order.buyerRealName || order.ownName || '—',
          totalPrice: order.totalPrice ?? 0,
          time: formatTime(order.createTime),
          status: statusText,
          statusClass: getStatusClass(statusText)
        }
      })
    }

    // 获取融资申请数量
    const financeRes = await getMyFinanceApplications({ page: 1, pageSize: 1 })
    if (financeRes.code === 200) {
      stats.value.financeCount = financeRes.data.total || 0
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

// 获取状态样式类
const getStatusClass = (status) => {
  const statusMap = {
    '待付款': 'warning',
    '待发货': 'pending',
    '待收货': 'info',
    '已完成': 'success',
    '已取消': 'info'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (statusCode) => {
  const statusMap = {
    1: '待付款',
    2: '待发货',
    3: '待收货',
    4: '已完成',
    5: '已取消'
  }
  return statusMap[statusCode] || '未知'
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.farmer-dashboard {
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
  background: rgba(45, 90, 61, 0.1);
  color: var(--color-primary, #2d5a3d);
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
  background: rgba(91, 138, 158, 0.1);
  color: var(--color-info, #5b8a9e);
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

.stat-trend.negative {
  color: var(--color-error, #b85c38);
}

.stat-action {
  margin-top: var(--spacing-2, 8px);
}

.stat-action a {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-primary, #2d5a3d);
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
  color: var(--color-primary, #2d5a3d);
  text-decoration: none;
  font-weight: var(--font-weight-medium, 500);
}

.card-link:hover {
  text-decoration: underline;
}

/* ===== 订单列表 ===== */
.order-list {
  padding: var(--spacing-3, 12px);
}

.order-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border-radius: var(--radius-base, 8px);
  transition: background var(--transition-fast, 150ms ease);
}

.order-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
}

.order-info {
  flex: 1;
  min-width: 0;
}

.order-product {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.order-price {
  color: var(--color-error, #b85c38);
  font-weight: var(--font-weight-semibold, 600);
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

.order-status.warning {
  background: rgba(184, 92, 56, 0.1);
  color: var(--color-error, #b85c38);
}

.order-status.success {
  background: rgba(74, 124, 89, 0.1);
  color: var(--color-success, #4a7c59);
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
  background: rgba(45, 90, 61, 0.08);
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
  color: var(--color-primary, #2d5a3d);
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

  .quick-actions {
    grid-template-columns: 1fr;
  }
}
</style>
