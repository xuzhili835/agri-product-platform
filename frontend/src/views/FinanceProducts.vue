<template>
  <div class="finance-products-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">融资产品</h1>
      <p class="page-subtitle">专为农业设计的融资解决方案</p>
    </div>

    <!-- 产品列表 -->
    <div class="products-list">
      <div v-for="product in products" :key="product.id" class="product-card">
        <div class="product-header">
          <div class="product-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
              <circle cx="12" cy="12" r="2" fill="currentColor"/>
            </svg>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-bank">{{ product.bank }}</p>
          </div>
        </div>
        <div class="product-details">
          <div class="detail-item">
            <span class="detail-label">额度范围</span>
            <span class="detail-value">{{ product.amountRange }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">年利率</span>
            <span class="detail-value rate">{{ product.interestRate }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">期限</span>
            <span class="detail-value">{{ product.term }}</span>
          </div>
        </div>
        <div class="product-features">
          <div v-for="feature in product.features" :key="feature" class="feature-tag">
            {{ feature }}
          </div>
        </div>
        <div class="product-actions">
          <el-button type="primary" size="large" block @click="handleApply(product)">立即申请</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 处理融资申请
const handleApply = (product) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录后申请融资')
    router.push('/login')
    return
  }
  if (userStore.role !== 'farmer') {
    ElMessage.warning('融资服务仅对农户开放')
    return
  }
  // TODO: 跳转到融资申请页面
  ElMessage.info('申请产品：' + product.name)
}

const products = ref([
  {
    id: 1,
    name: '农业生产贷款',
    bank: '中国农业银行',
    amountRange: '5万 - 100万',
    interestRate: '3.85% 起',
    term: '1-3年',
    features: ['无抵押', '审批快', '随借随还']
  },
  {
    id: 2,
    name: '农机购置贷款',
    bank: '农商银行',
    amountRange: '10万 - 200万',
    interestRate: '4.25% 起',
    term: '1-5年',
    features: ['专项贷款', '利率优惠', '政府补贴']
  },
  {
    id: 3,
    name: '大棚建设贷款',
    bank: '建设银行',
    amountRange: '20万 - 300万',
    interestRate: '4.15% 起',
    term: '3-5年',
    features: ['长期限', '额度高', '分期还款']
  },
  {
    id: 4,
    name: '种子采购贷款',
    bank: '邮储银行',
    amountRange: '3万 - 50万',
    interestRate: '3.95% 起',
    term: '1-2年',
    features: ['门槛低', '放款快', '灵活还款']
  }
])
</script>

<style scoped>
.finance-products-page {
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

/* ===== 产品列表 ===== */
.products-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-5, 20px);
}

.product-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  padding: var(--spacing-5, 20px);
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(31, 41, 35, 0.06));
  transition: all var(--transition-fast, 150ms ease);
}

.product-card:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(31, 41, 35, 0.08));
  transform: translateY(-2px);
}

.product-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-4, 16px);
  margin-bottom: var(--spacing-5, 20px);
  padding-bottom: var(--spacing-5, 20px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.product-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45, 90, 61, 0.1);
  border-radius: var(--radius-lg, 12px);
  color: var(--color-primary, #2d5a3d);
  flex-shrink: 0;
}

.product-icon svg {
  width: 28px;
  height: 28px;
}

.product-name {
  font-size: var(--font-size-xl, 24px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-1, 4px);
}

.product-bank {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

/* ===== 产品详情 ===== */
.product-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-4, 16px);
  margin-bottom: var(--spacing-4, 16px);
}

.detail-item {
  text-align: center;
  padding: var(--spacing-3, 12px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
}

.detail-label {
  display: block;
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  margin-bottom: var(--spacing-1, 4px);
}

.detail-value {
  display: block;
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
}

.detail-value.rate {
  color: var(--color-error, #b85c38);
}

/* ===== 产品特点 ===== */
.product-features {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-2, 8px);
  margin-bottom: var(--spacing-5, 20px);
}

.feature-tag {
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-primary, #2d5a3d);
  background: rgba(45, 90, 61, 0.08);
  border-radius: var(--radius-full, 9999px);
}

/* ===== 产品操作 ===== */
.product-actions {
  padding-top: var(--spacing-4, 16px);
  border-top: 1px solid var(--color-divider, #edebe6);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .products-list {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: var(--font-size-3xl, 38px);
  }
}
</style>
