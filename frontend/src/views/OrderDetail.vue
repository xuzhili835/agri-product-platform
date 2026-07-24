<template>
  <div class="order-detail-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft" class="back-btn">返回订单列表</el-button>
      <h2>订单详情</h2>
      <div class="header-spacer"></div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" v-loading="loading" class="loading-container"></div>

    <!-- 订单详情 -->
    <el-card v-else-if="orderDetail" class="detail-card">
      <!-- 订单状态步骤条 -->
      <div class="status-section">
        <el-steps :active="getStatusStep(orderDetail.purchaseStatus)" finish-status="success" align-center>
          <el-step title="待付款" description="等待买家付款" />
          <el-step title="待发货" description="商家准备发货" />
          <el-step title="待收货" description="商品已发出" />
          <el-step title="已完成" description="订单完成" />
        </el-steps>
        <div v-if="orderDetail.purchaseStatus === 5" class="cancelled-status">
          <el-tag type="danger" size="large">订单已取消</el-tag>
        </div>
        <div class="order-id">订单号: {{ orderDetail.purchaseId }}</div>
      </div>

      <!-- 订单基本信息 -->
      <div class="section">
        <h3>订单信息</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">订单状态:</span>
            <el-tag :type="getStatusType(orderDetail.purchaseStatus)">
              {{ orderDetail.statusText }}
            </el-tag>
          </div>
          <div class="info-item">
            <span class="label">创建时间:</span>
            <span>{{ formatTime(orderDetail.createTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">订单总额:</span>
            <span class="amount">¥{{ orderDetail.totalPrice }}</span>
          </div>
          <div class="info-item" v-if="orderDetail.address">
            <span class="label">收货地址:</span>
            <span>{{ orderDetail.address }}</span>
          </div>
        </div>
      </div>

      <!-- 商品清单 -->
      <div class="section">
        <h3>商品清单 (共 {{ getTotalCount() }} 件)</h3>
        <div class="product-list">
          <div v-for="item in orderDetail.items" :key="item.productId" class="product-item">
            <img :src="item.productPic || '/placeholder.jpg'" @error="handleImageError" class="product-img" />
            <div class="product-info">
              <div class="product-name">{{ item.productName }}</div>
              <div class="product-specs">
                <span class="price">¥{{ item.price }} / kg</span>
                <span class="count">x {{ item.count }} kg</span>
              </div>
            </div>
            <div class="product-total">
              <span class="total-price">¥{{ item.totalPrice }}</span>
            </div>
          </div>
        </div>
        <div class="order-summary">
          <div class="summary-row">
            <span>商品总额:</span>
            <span>¥{{ orderDetail.totalPrice }}</span>
          </div>
          <div class="summary-row total">
            <span>实付款:</span>
            <span class="final-amount">¥{{ orderDetail.totalPrice }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions">
        <el-button v-if="orderDetail.purchaseStatus === 1" type="primary" @click="handlePay">
          <el-icon><Wallet /></el-icon>
          立即支付
        </el-button>
        <el-button v-if="orderDetail.purchaseStatus === 1" type="danger" plain @click="handleCancel">
          <el-icon><Delete /></el-icon>
          取消订单
        </el-button>
        <el-button v-if="orderDetail.purchaseStatus === 3" type="success" plain @click="handleConfirm">
          <el-icon><Check /></el-icon>
          确认收货
        </el-button>
        <el-button v-if="orderDetail.purchaseStatus === 4" type="info" plain @click="handleBuyAgain">
          <el-icon><ShoppingCart /></el-icon>
          再次购买
        </el-button>
      </div>
    </el-card>

    <!-- 订单不存在 -->
    <el-card v-else class="empty-card">
      <el-empty description="订单不存在或已被删除">
        <el-button type="primary" @click="goBack">返回订单列表</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Wallet, Delete, Check, ShoppingCart } from '@element-plus/icons-vue'
import { getOrderDetail, cancelOrder as cancelOrderApi, updateOrderStatus } from '@/api/order'

const router = useRouter()
const route = useRoute()

const orderDetail = ref(null)
const loading = ref(false)

onMounted(async () => {
  await loadOrderDetail()
})

const loadOrderDetail = async () => {
  loading.value = true
  try {
    const orderId = route.params.id
    const res = await getOrderDetail(orderId)
    orderDetail.value = res.data
  } catch (error) {
    console.error('加载订单详情失败:', error)
    ElMessage.error(error.message || '加载订单详情失败')
  } finally {
    loading.value = false
  }
}

const getStatusStep = (status) => {
  const stepMap = { 1: 0, 2: 1, 3: 2, 4: 3 }
  return stepMap[status] ?? -1
}

const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'primary', 3: 'info', 4: 'success', 5: 'danger' }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const getTotalCount = () => {
  if (!orderDetail.value?.items) return 0
  return orderDetail.value.items.reduce((sum, item) => sum + (item.count || 0), 0)
}

const handleImageError = (e) => {
  e.target.src = '/placeholder.jpg'
}

const goBack = () => {
  router.push('/buyer/orders')
}

const handlePay = () => {
  ElMessage.info('跳转到支付页面...')
  // TODO: 跳转到支付页面或调用支付API
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要取消订单 ${orderDetail.value.purchaseId} 吗？取消后将无法恢复。`,
      '取消订单',
      { type: 'warning' }
    )
    await cancelOrderApi(orderDetail.value.purchaseId)
    ElMessage.success('订单已取消')
    await loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error(error.message || '取消订单失败')
    }
  }
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm(
      '确认已收到商品吗？确认后订单将完成。',
      '确认收货',
      { type: 'info' }
    )
    await updateOrderStatus(orderDetail.value.purchaseId, { status: 4 })
    ElMessage.success('确认收货成功')
    await loadOrderDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认收货失败:', error)
      ElMessage.error(error.message || '确认收货失败')
    }
  }
}

const handleBuyAgain = () => {
  ElMessage.info('跳转到市场页面...')
  router.push('/market')
}
</script>

<style scoped>
.order-detail-container {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  flex: 1;
}

.header-spacer {
  width: 100px;
}

.back-btn {
  flex-shrink: 0;
}

.loading-container {
  min-height: 300px;
}

.detail-card {
  margin-bottom: 20px;
}

/* 状态区域 */
.status-section {
  margin-bottom: 30px;
}

.cancelled-status {
  text-align: center;
  margin-top: 20px;
}

.order-id {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-top: 20px;
}

/* 信息区域 */
.section {
  margin-bottom: 30px;
}

.section h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-item .label {
  color: #909399;
  font-size: 14px;
}

.info-item .amount {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
}

/* 商品列表 */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.product-item {
  display: flex;
  gap: 15px;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
}

.product-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.product-specs {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #909399;
}

.product-specs .price {
  color: #f56c6c;
}

.product-total {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
}

.total-price {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

/* 订单汇总 */
.order-summary {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.summary-row.total {
  font-size: 16px;
  font-weight: bold;
  padding-top: 15px;
  border-top: 1px dashed #ddd;
  margin-top: 10px;
}

.final-amount {
  font-size: 20px;
  color: #f56c6c;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 10px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.empty-card {
  text-align: center;
}

@media (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .product-item {
    flex-direction: column;
  }

  .product-img {
    width: 100%;
    height: 150px;
  }

  .product-total {
    align-items: flex-start;
  }
}
</style>
