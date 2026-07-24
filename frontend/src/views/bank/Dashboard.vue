<template>
  <div class="bank-dashboard">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">{{ greeting }}，{{ userStore.displayName() }}</h1>
      <p class="page-subtitle">银行工作台 - 处理融资申请，智能匹配农户需求</p>
    </div>

    <!-- 核心指标卡片 -->
    <div class="stats-grid">
      <div class="stat-card primary">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
            <circle cx="12" cy="12" r="2" fill="currentColor"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">¥ {{ formatNumber(stats.monthlyAmount) }}</div>
          <div class="stat-label">本月放款金额</div>
          <div class="stat-trend positive">
            <el-icon><Top /></el-icon>
            <span>{{ stats.approvedCount }} 笔已放款</span>
          </div>
        </div>
      </div>

      <div class="stat-card warning">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.pendingCount }}</div>
          <div class="stat-label">待审批申请</div>
          <div class="stat-action">
            <router-link to="/bank/approvals">立即处理</router-link>
          </div>
        </div>
      </div>

      <div class="stat-card success">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <path d="M22 4L12 14.01l-3-3"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.approvedCount }}</div>
          <div class="stat-label">已批准申请</div>
        </div>
      </div>

      <div class="stat-card info">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M13.5 3H12a8 8 0 0 0-8 8v12a2 2 0 0 0 2 2h7"/>
            <path d="M13 21v-8a4 4 0 0 0-4-4H5"/>
            <path d="M5 21h14"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.approvalRate }}%</div>
          <div class="stat-label">审批通过率</div>
        </div>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="content-grid">
      <!-- 待审批申请 -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">待审批申请</h3>
          <router-link to="/bank/approvals" class="card-link">查看全部</router-link>
        </div>
        <div v-loading="loading" class="approval-list">
          <div v-for="approval in pendingApprovals" :key="approval.id" class="approval-item">
            <div class="approval-info">
              <div class="approval-farmer">
                <span class="farmer-name">{{ approval.farmerName || approval.farmer }}</span>
                <span class="approval-amount">¥{{ formatNumber(approval.amount) }}</span>
              </div>
              <div class="approval-meta">
                <span class="approval-product">{{ approval.productName || approval.product }}</span>
                <span class="approval-time">{{ formatTime(approval.createdAt) }}</span>
              </div>
            </div>
            <div class="approval-actions">
              <el-button type="success" size="small" @click="handleApproval(approval.id, 'approve')">
                通过
              </el-button>
              <el-button type="danger" plain size="small" @click="handleApproval(approval.id, 'reject')">
                拒绝
              </el-button>
            </div>
          </div>
          <div v-if="pendingApprovals.length === 0 && !loading" class="empty-state">
            <p>暂无待审批申请</p>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="content-card">
        <div class="card-header">
          <h3 class="card-title">快捷操作</h3>
        </div>
        <div class="quick-actions">
          <router-link to="/bank/approvals" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
              </svg>
            </div>
            <span>融资审批</span>
          </router-link>
          <router-link to="/bank/matching" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="12" r="3"/>
                <path d="M12 1v6m0 6v6"/>
                <path d="m5 12 6-6m0 12 6-6"/>
                <circle cx="12" cy="12" r="9"/>
              </svg>
            </div>
            <span>智能匹配</span>
          </router-link>
          <router-link to="/bank/products" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
                <circle cx="12" cy="12" r="2" fill="currentColor"/>
              </svg>
            </div>
            <span>产品管理</span>
          </router-link>
          <router-link to="/bank/matching" class="quick-action">
            <div class="action-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <span>意向客户</span>
          </router-link>
        </div>
      </div>
    </div>

    <!-- 最近审批 -->
    <div class="content-card">
      <div class="card-header">
        <h3 class="card-title">最近审批记录</h3>
      </div>
      <div v-loading="loading" class="approval-history">
        <div v-for="record in approvalHistory" :key="record.id" class="history-item">
          <div class="history-icon" :class="record.statusClass">
            <svg v-if="record.status === 'approved'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <path d="M22 4L12 14.01l-3-3"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/>
              <path d="M15 9l-6 6M9 9l6 6"/>
            </svg>
          </div>
          <div class="history-content">
            <div class="history-title">
              {{ record.farmerName || record.farmer }} 的融资申请
            </div>
            <div class="history-meta">
              <span>金额：¥{{ formatNumber(record.amount) }}</span>
              <span>{{ record.productName || record.product }}</span>
            </div>
          </div>
          <div class="history-time">{{ formatTime(record.updatedAt || record.createdAt) }}</div>
        </div>
        <div v-if="approvalHistory.length === 0 && !loading" class="empty-state">
          <p>暂无审批记录</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBankStats, approveFinanceApplication } from '@/api/finance'

const userStore = useUserStore()

const loading = ref(false)

// 统计数据
const stats = ref({
  monthlyAmount: 0,
  growthRate: 0,
  pendingCount: 0,
  approvedCount: 0,
  approvalRate: 0
})

// 待审批申请列表
const pendingApprovals = ref([])

// 审批历史记录
const approvalHistory = ref([])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

/**
 * 格式化数字，添加千位分隔符
 */
const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  return Number(num).toLocaleString()
}

/**
 * 格式化时间显示
 */
const formatTime = (timestamp) => {
  if (!timestamp) return ''

  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 2) {
    const hour = date.getHours().toString().padStart(2, '0')
    const minute = date.getMinutes().toString().padStart(2, '0')
    return `今天 ${hour}:${minute}`
  }
  if (days < 7) {
    const hour = date.getHours().toString().padStart(2, '0')
    const minute = date.getMinutes().toString().padStart(2, '0')
    return `${days}天前 ${hour}:${minute}`
  }

  // 长时间显示完整日期
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hour = date.getHours().toString().padStart(2, '0')
  const minute = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

/**
 * 获取统计数据和列表数据（来自后端聚合接口，字段与 Finance 实体一致）
 */
const farmerName = (f) => f.realName || f.ownName || '未知农户'

const toPendingItem = (f) => ({
  id: f.financeId,
  farmer: farmerName(f),
  farmerName: farmerName(f),
  amount: f.money || 0,
  product: f.productName || '融资产品',
  productName: f.productName || '融资产品',
  createdAt: f.createTime
})

const toHistoryItem = (f) => {
  const approved = f.status === 1
  return {
    id: f.financeId,
    farmer: farmerName(f),
    farmerName: farmerName(f),
    amount: f.money || 0,
    product: f.productName || '融资产品',
    productName: f.productName || '融资产品',
    status: approved ? 'approved' : 'rejected',
    statusClass: approved ? 'success' : 'error',
    createdAt: f.createTime,
    updatedAt: f.updateTime
  }
}

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const { data } = await getBankStats()
    const s = data || {}

    stats.value = {
      monthlyAmount: s.monthlyAmount || 0,
      growthRate: s.approvalRate || 0,
      pendingCount: s.pendingCount || 0,
      approvedCount: s.approvedCount || 0,
      approvalRate: s.approvalRate || 0
    }

    pendingApprovals.value = (s.pendingList || []).map(toPendingItem)
    approvalHistory.value = (s.recentList || []).map(toHistoryItem)
  } catch (error) {
    console.error('获取仪表盘数据失败:', error)
    ElMessage.error(error?.message || '加载数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleApproval = async (id, action) => {
  const status = action === 'approve' ? 1 : 2
  const actionText = action === 'approve' ? '通过' : '拒绝'
  try {
    let remark = ''
    if (action === 'reject') {
      const res = await ElMessageBox.prompt('请输入拒绝原因（选填）', '拒绝申请', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：材料不全 / 信用评估未通过'
      })
      remark = res.value || ''
    } else {
      await ElMessageBox.confirm('确认通过该融资申请？', '审批确认', {
        type: 'success',
        confirmButtonText: '确认通过',
        cancelButtonText: '取消'
      })
    }
    await approveFinanceApplication(id, { status, remark })
    ElMessage.success(`已${actionText}`)
    fetchDashboardData()
  } catch (e) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.bank-dashboard {
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
  color: var(--color-bank, #2d5a3d);
}

.stat-card.warning .stat-icon {
  background: rgba(201, 166, 97, 0.1);
  color: var(--color-warning, #c9a661);
}

.stat-card.success .stat-icon {
  background: rgba(74, 124, 89, 0.1);
  color: var(--color-success, #4a7c59);
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

.stat-action {
  margin-top: var(--spacing-2, 8px);
}

.stat-action a {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-bank, #2d5a3d);
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
  color: var(--color-bank, #2d5a3d);
  text-decoration: none;
  font-weight: var(--font-weight-medium, 500);
}

.card-link:hover {
  text-decoration: underline;
}

/* ===== 待审批列表 ===== */
.approval-list {
  padding: var(--spacing-3, 12px);
  min-height: 100px;
}

.approval-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
  gap: var(--spacing-4, 16px);
}

.approval-item:last-child {
  border-bottom: none;
}

.approval-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
}

.approval-info {
  flex: 1;
  min-width: 0;
}

.approval-farmer {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  margin-bottom: var(--spacing-1, 4px);
}

.farmer-name {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
}

.approval-amount {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-error, #b85c38);
}

.approval-meta {
  display: flex;
  gap: var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.approval-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
  flex-shrink: 0;
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
  color: var(--color-bank, #2d5a3d);
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

/* ===== 审批历史 ===== */
.approval-history {
  padding: var(--spacing-3, 12px);
  min-height: 100px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.history-item:last-child {
  border-bottom: none;
}

.history-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full, 9999px);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.history-icon.success {
  background: rgba(74, 124, 89, 0.1);
  color: var(--color-success, #4a7c59);
}

.history-icon.error {
  background: rgba(184, 92, 56, 0.1);
  color: var(--color-error, #b85c38);
}

.history-icon svg {
  width: 18px;
  height: 18px;
}

.history-content {
  flex: 1;
  min-width: 0;
}

.history-title {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
}

.history-meta {
  display: flex;
  gap: var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.history-time {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  flex-shrink: 0;
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: var(--spacing-6, 24px);
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

  .approval-actions {
    flex-direction: column;
  }

  .page-title {
    font-size: var(--font-size-2xl, 30px);
  }
}
</style>
