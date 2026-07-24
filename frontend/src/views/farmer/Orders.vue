<template>
  <div class="orders-page">
    <div class="page-header">
      <h1 class="page-title">订单管理</h1>
    </div>

    <!-- 状态筛选：点击切换（与买家订单页一致），1待付款 2待发货 3待收货 4已完成 5已取消 -->
    <div class="status-filter">
      <el-radio-group v-model="statusFilter" @change="onStatusChange">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button :value="1">待付款</el-radio-button>
        <el-radio-button :value="2">待发货</el-radio-button>
        <el-radio-button :value="3">待收货</el-radio-button>
        <el-radio-button :value="4">已完成</el-radio-button>
        <el-radio-button :value="5">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 订单列表 -->
    <el-card class="orders-card" v-loading="loading">
      <div v-for="order in orders" :key="order.purchaseId" class="order-item">
        <div class="order-header">
          <span class="order-id">订单号：{{ order.purchaseId }}</span>
          <el-tag :type="getStatusType(order.purchaseStatus)" size="small">
            {{ order.statusText || statusText(order.purchaseStatus) }}
          </el-tag>
        </div>

        <!-- 商品列表（可能包含多个商品） -->
        <div class="products-section">
          <div v-for="item in (order.items || [])" :key="item.productId" class="product-item">
            <div class="product-info">
              <img :src="item.productPic || '/images/default-product.jpg'" class="product-image" />
              <div class="product-details">
                <div class="product-name">{{ item.productName }}</div>
                <div class="product-meta">
                  <span>数量：{{ item.count }}</span>
                  <span>单价：¥{{ item.price }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="order-content">
          <div class="order-info">
            <div class="order-total">
              <span class="label">订单总额：</span>
              <span class="amount">¥{{ order.totalPrice }}</span>
            </div>
            <div class="order-buyer">
              <span class="label">买家：</span>
              <!-- 优先展示买家真实姓名，缺失时回退登录账号 -->
              <span>{{ order.buyerRealName || order.ownName }}</span>
            </div>
            <div class="order-buyer" v-if="order.address">
              <span class="label">收货地址：</span>
              <span>{{ order.address }}</span>
            </div>
            <div class="order-buyer">
              <span class="label">下单时间：</span>
              <span>{{ formatTime(order.createTime) }}</span>
            </div>
          </div>

          <div class="order-actions">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="viewDetail(order)">详情</el-button>
              <el-button v-if="order.purchaseStatus === 2" type="primary" size="small" @click="shipOrder(order)">
                发货
              </el-button>
              <el-button v-if="order.purchaseStatus === 3" type="info" size="small" plain disabled>
                待买家收货
              </el-button>
              <el-button v-if="order.purchaseStatus === 1" type="danger" size="small" plain @click="cancelOrder(order)">
                取消订单
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadOrders"
          @current-change="loadOrders"
        />
      </div>

      <el-empty v-if="!loading && orders.length === 0" description="暂无订单数据" />
    </el-card>

    <!-- 订单详情对话框（含跟踪进度条，与买家订单详情一致） -->
    <el-dialog v-model="detailVisible" title="订单详情" width="680px">
      <template v-if="current">
        <!-- 跟踪进度条 -->
        <div class="detail-status">
          <el-steps :active="getStatusStep(current.purchaseStatus)" finish-status="success" align-center>
            <el-step title="待付款" description="等待买家付款" />
            <el-step title="待发货" description="买家已付款" />
            <el-step title="待收货" description="商品已发出" />
            <el-step title="已完成" description="订单完成" />
          </el-steps>
          <div v-if="current.purchaseStatus === 5" class="cancelled-status">
            <el-tag type="danger" size="large">订单已取消</el-tag>
          </div>
        </div>

        <!-- 订单信息 -->
        <div class="detail-section">
          <div class="info-grid">
            <div class="info-item"><span class="label">订单号：</span>{{ current.purchaseId }}</div>
            <div class="info-item">
              <span class="label">订单状态：</span>
              <el-tag :type="getStatusType(current.purchaseStatus)" size="small">
                {{ current.statusText || statusText(current.purchaseStatus) }}
              </el-tag>
            </div>
            <div class="info-item"><span class="label">下单时间：</span>{{ formatTime(current.createTime) }}</div>
            <div class="info-item"><span class="label">订单总额：</span><span class="amount">¥{{ current.totalPrice }}</span></div>
            <div class="info-item"><span class="label">买家：</span>{{ current.buyerRealName || current.ownName }}</div>
            <div class="info-item" v-if="current.address"><span class="label">收货地址：</span>{{ current.address }}</div>
          </div>
        </div>

        <!-- 商品清单 -->
        <div class="detail-section">
          <h4 class="detail-subtitle">商品清单</h4>
          <div v-for="item in (current.items || [])" :key="item.productId" class="detail-product">
            <img :src="item.productPic || '/images/default-product.jpg'" class="detail-product-img" />
            <div class="detail-product-info">
              <div class="product-name">{{ item.productName }}</div>
              <div class="product-meta">
                <span>单价：¥{{ item.price }}</span>
                <span>数量：{{ item.count }}</span>
                <span>小计：¥{{ item.totalPrice }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current && current.purchaseStatus === 2" type="primary" @click="runFromDetail(shipOrder)">
          发货
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReceivedOrders, cancelOrder as cancelOrderApi, updateOrderStatus } from '@/api/order'

const loading = ref(false)
const orders = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
// 状态筛选：'all' 全部，或具体状态值 1待付款 2待发货 3待收货 4已完成 5已取消
const statusFilter = ref('all')

// 详情对话框
const detailVisible = ref(false)
const current = ref(null)

// 切换状态筛选：回到第一页并重新拉取
const onStatusChange = () => {
  currentPage.value = 1
  loadOrders()
}

// 加载订单列表
const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getReceivedOrders({
      page: currentPage.value,
      pageSize: pageSize.value,
      status: statusFilter.value === 'all' ? undefined : statusFilter.value
    })
    if (res.code === 200) {
      orders.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化时间
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

// 获取状态标签类型（purchaseStatus 为整数 1-5）
const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'primary', 3: 'info', 4: 'success', 5: 'danger' }
  return map[status] || 'info'
}

// 状态文本
const statusText = (status) => {
  const map = { 1: '待付款', 2: '待发货', 3: '待收货', 4: '已完成', 5: '已取消' }
  return map[status] || '未知'
}

// 进度条当前步：1→0 2→1 3→2 4→3；5已取消单独展示
const getStatusStep = (status) => {
  const map = { 1: 0, 2: 1, 3: 2, 4: 3 }
  return map[status] ?? -1
}

// 查看详情
const viewDetail = (order) => {
  current.value = order
  detailVisible.value = true
}

// 详情对话框里的操作：执行后关闭对话框
const runFromDetail = async (fn) => {
  if (!current.value) return
  const order = current.value
  detailVisible.value = false
  await fn(order)
}

// 发货：状态 2(待发货) → 3(待收货)
const shipOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`确认订单 ${order.purchaseId} 已发货？`, '提示', { type: 'info' })
    await updateOrderStatus(order.purchaseId, { status: 3 })
    ElMessage.success('发货成功')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('发货失败')
  }
}

// 取消订单
const cancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`确定要取消订单 ${order.purchaseId} 吗？`, '提示', { type: 'warning' })
    await cancelOrderApi(order.purchaseId)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('取消订单失败')
  }
}

onMounted(() => {
  loadOrders()
})
</script>

<style scoped>
.orders-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

/* 状态筛选 */
.status-filter {
  margin-bottom: 4px;
}

.orders-card {
  flex: 1;
}

.order-item {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.order-item:last-child {
  border-bottom: none;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 14px;
  color: #666;
}

.order-id {
  font-weight: 500;
}

.products-section {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-item {
  display: flex;
  gap: 15px;
}

.order-content {
  display: flex;
  gap: 20px;
}

.product-info {
  display: flex;
  gap: 15px;
  flex: 1;
}

.product-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.product-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-name {
  font-weight: 500;
  font-size: 16px;
}

.product-meta {
  font-size: 14px;
  color: #666;
  display: flex;
  gap: 15px;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 150px;
  flex: 1;
}

.order-info .label {
  color: #909399;
}

.order-total .amount {
  font-weight: 600;
  color: #f56c6c;
  font-size: 18px;
}

.order-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
  min-width: 150px;
}

.action-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 详情对话框 */
.detail-status {
  margin-bottom: 24px;
}

.cancelled-status {
  text-align: center;
  margin-top: 16px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-subtitle {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  font-size: 14px;
  color: #303133;
}

.info-item .label {
  color: #909399;
}

.info-item .amount {
  color: #f56c6c;
  font-weight: 600;
}

.detail-product {
  display: flex;
  gap: 12px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 10px;
}

.detail-product-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.detail-product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
  .order-content {
    flex-direction: column;
  }
  .order-actions {
    align-items: flex-start;
  }
}
</style>
