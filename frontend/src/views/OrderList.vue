<template>
  <div class="order-list-container">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft" class="back-btn">返回</el-button>
      <h2>我的订单</h2>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadData">
      <el-tab-pane label="订单中" name="order">
        <template #label>
          <span class="tab-label">
            <el-icon><Box /></el-icon>
            订单中
            <el-badge v-if="orderCount > 0" :value="orderCount" type="success" />
          </span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="求购中" name="demand">
        <template #label>
          <span class="tab-label">
            <el-icon><ShoppingCart /></el-icon>
            求购中
            <el-badge v-if="demandCount > 0" :value="demandCount" type="primary" />
          </span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- 求购列表 -->
    <div v-if="activeTab === 'demand'" v-loading="loading.demand">
      <el-empty v-if="demands.length === 0 && !loading.demand" description="暂无求购需求，去发布一个？">
        <el-button type="primary" @click="goToPublish">发布求购</el-button>
      </el-empty>

      <div v-else class="card-list">
        <el-card v-for="item in demands" :key="item.productId || item.orderId" class="demand-card">
          <div class="card-header">
            <el-tag type="warning" size="small">求购</el-tag>
            <span class="card-title">{{ item.title }}</span>
            <el-tag :type="getDemandStatusType(item.orderStatus)" size="small">
              {{ getDemandStatusText(item.orderStatus) }}
            </el-tag>
          </div>
          <div class="card-content">
            <div class="content-left">
              <img v-if="item.picPath" :src="item.picPath" @error="handleImageError" />
              <div v-else class="no-image">无图</div>
            </div>
            <div class="content-right">
              <p class="description">{{ item.content }}</p>
              <div class="meta-info">
                <span class="price">预算: {{ item.price ? `¥${item.price}/kg` : '面议' }}</span>
                <span class="time">{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
          </div>
          <div class="card-footer">
            <el-button size="small" @click="editDemand(item)">编辑</el-button>
            <el-button size="small" type="danger" @click="cancelDemand(item)">撤单</el-button>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 订单列表 -->
    <div v-if="activeTab === 'order'" v-loading="loading.order">
      <!-- 状态筛选：订单存在 待付款/待发货/待收货/已完成/已取消 等流转状态，提供按状态筛选 -->
      <div class="status-filter">
        <el-radio-group v-model="orderStatus" @change="onStatusChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button :value="1">待付款</el-radio-button>
          <el-radio-button :value="2">待发货</el-radio-button>
          <el-radio-button :value="3">待收货</el-radio-button>
          <el-radio-button :value="4">已完成</el-radio-button>
          <el-radio-button :value="5">已取消</el-radio-button>
        </el-radio-group>
      </div>

      <el-empty v-if="orders.length === 0 && !loading.order" description="暂无订单，去市场逛逛？">
        <el-button type="primary" @click="goToMarket">去市场</el-button>
      </el-empty>

      <div v-else class="card-list">
        <el-card v-for="order in orders" :key="order.purchaseId" class="order-card">
          <div class="card-header">
            <span>订单号: {{ order.purchaseId }}</span>
            <el-tag :type="getOrderStatusType(order.purchaseStatus)" size="small">
              {{ order.statusText }}
            </el-tag>
          </div>
          <div class="card-content">
            <div class="items-preview">
              <div
                v-for="(item, index) in order.items?.slice(0, 3)"
                :key="item.productId"
                class="item-thumb"
              >
                <img :src="item.productPic" @error="handleImageError" />
                <span v-if="index === 0 && order.items?.length > 3" class="more-items">
                  +{{ order.items.length - 1 }}
                </span>
              </div>
              <div v-if="!order.items || order.items.length === 0" class="no-items">暂无商品</div>
            </div>
            <div class="order-info">
              <div class="time">{{ formatTime(order.createTime) }}</div>
              <div class="total-price">总价: ¥{{ order.totalPrice }}</div>
              <div v-if="order.sellerName" class="seller">发货人: {{ order.sellerName }}</div>
              <div v-if="order.address" class="address">配送至: {{ order.address }}</div>
            </div>
          </div>
          <div class="card-footer">
            <!-- 只有已付款订单才能查看详情 -->
            <el-button
              v-if="order.purchaseStatus > 1"
              size="small"
              @click="viewOrderDetail(order.purchaseId)"
            >查看详细</el-button>
            <!-- 待收货：可直接在列表确认收货，无需进入详情 -->
            <el-button
              v-if="order.purchaseStatus === 3"
              size="small"
              type="success"
              :loading="confirmingId === order.purchaseId"
              @click="confirmReceipt(order)"
            >确认收货</el-button>
            <el-button
              v-if="order.purchaseStatus === 1"
              size="small"
              type="primary"
              :loading="payingId === order.purchaseId"
              @click="payOrderNow(order)"
            >
              去支付
            </el-button>
            <el-button
              v-if="order.purchaseStatus === 1"
              size="small"
              type="danger"
              @click="cancelOrder(order)"
            >
              取消订单
            </el-button>
          </div>
        </el-card>
      </div>
    </div>

    <el-pagination
      v-if="pagination.total > 0"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      @current-change="loadData"
    />

    <!-- 编辑求购对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑求购需求" width="600px">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="100px">
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入需求标题，如：求购优质大米" />
        </el-form-item>
        <el-form-item label="需求描述" prop="content">
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="4"
            placeholder="请详细描述您的需求，包括数量、质量要求、交货时间等"
          />
        </el-form-item>
        <el-form-item label="预算价格">
          <el-input-number v-model="editForm.price" :min="0" :precision="2" :step="0.1" />
          <span class="unit-suffix">元/kg（可选）</span>
        </el-form-item>
        <el-form-item label="商品图片">
          <div class="product-image-upload">
            <div class="image-preview">
              <img v-if="editForm.picPath" :src="editForm.picPath" alt="预览" />
              <div v-else class="no-image">暂无图片</div>
            </div>
            <div class="image-actions">
              <el-upload
                action="/api/upload"
                :before-upload="beforeImageUpload"
                :on-success="handleImageSuccess"
                :show-file-list="false"
                accept="image/*"
              >
                <el-button type="primary" size="small">上传图片</el-button>
              </el-upload>
              <span class="divider">或</span>
              <el-select
                v-model="editForm.picPath"
                placeholder="选择本地图片"
                clearable
                filterable
                size="small"
              >
                <el-option
                  v-for="img in localImages"
                  :key="img.path"
                  :label="img.name"
                  :value="img.path"
                >
                  <div class="image-option">
                    <img :src="img.path" :alt="img.name" />
                    <span>{{ img.name }}</span>
                  </div>
                </el-option>
              </el-select>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit" :loading="saving">保存修改</el-button>
      </template>
    </el-dialog>

    <!-- 支付二维码对话框 -->
    <el-dialog
      v-model="payQrVisible"
      title="支付宝扫码支付"
      width="360px"
      @close="handleQrDialogClose"
    >
      <div class="pay-qr-box">
        <p class="pay-qr-tip">请使用 <b>支付宝（沙箱版App）</b> 扫描下方二维码完成付款</p>
        <img v-if="qrImgSrc" :src="qrImgSrc" class="pay-qr-img" alt="支付二维码" />
        <p v-if="payOrderInfo" class="pay-qr-amount">
          订单号：{{ payOrderInfo.purchaseId }}　金额：¥{{ payOrderInfo.totalPrice }}
        </p>
        <p class="pay-qr-status">
          <el-icon class="is-loading"><Loading /></el-icon>
          正在等待支付结果…
        </p>
      </div>
      <template #footer>
        <el-button @click="payQrVisible = false">关闭</el-button>
        <el-button type="primary" @click="refreshPayStatus(payOrderInfo?.purchaseId)">
          我已支付，查询结果
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeMount, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart, Box, Loading, ArrowLeft } from '@element-plus/icons-vue'
import { getOrders, cancelOrder as cancelOrderApi, updateOrderStatus } from '@/api/order'
import { payOrder, queryPay } from '@/api/alipay'
import QRCode from 'qrcode'
import { getMyProducts, deleteProduct as deleteProductApi, updateProduct } from '@/api/product'
import { LOCAL_PRODUCT_IMAGES } from '@/config/localImages'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 本地图片列表
const localImages = LOCAL_PRODUCT_IMAGES

// 默认显示订单中（已付款的订单）
const activeTab = ref('order')
// 订单状态筛选：'all' 全部，或具体状态值 1待付款 2待发货 3待收货 4已完成 5已取消
const orderStatus = ref('all')
const demands = ref([])
const orders = ref([])
const demandCount = ref(0)
const orderCount = ref(0)
const payingId = ref(null)
const confirmingId = ref(null)
const payQrVisible = ref(false)
const qrImgSrc = ref('')
const payOrderInfo = ref(null)
const payPolling = ref(false)
let payTimer = null
// 单次查询在途标记：防止轮询 tick 叠加；成功已通知标记：防止重复弹「支付成功」
let payQuerying = false
let payDone = false

const loading = reactive({
  demand: false,
  order: false
})

// 编辑对话框状态
const editDialogVisible = ref(false)
const editFormRef = ref(null)
const saving = ref(false)
const editForm = reactive({
  productId: null,
  title: '',
  content: '',
  price: 0,
  picPath: '',
  type: 'demand'
})

const editRules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入需求描述', trigger: 'blur' }]
}

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

onMounted(() => {
  // 同时加载两个标签页的数量数据（用于徽章显示）
  loadDemandCount()
  loadOrderCount()
  // 加载当前激活标签页的详细数据
  loadData()
})

onUnmounted(() => {
  stopPayPolling()
})

// 监听路由变化，确保每次导航都重新加载数据
watch(() => route.path, () => {
  console.log('路由变化，重新加载数据')
  loadData()
})

const loadData = async () => {
  if (activeTab.value === 'demand') {
    await loadDemands()
  } else {
    await loadOrders()
  }
}

// 切换状态筛选：回到第一页并重新拉取
const onStatusChange = () => {
  pagination.page = 1
  loadOrders()
}

// 仅加载求购数量（用于徽章显示）
const loadDemandCount = async () => {
  try {
    const res = await getMyProducts({
      page: 1,
      pageSize: 1,  // 只需要获取总数
      type: 'demand'
    })
    demandCount.value = res.data?.total || 0
  } catch (error) {
    console.error('加载求购数量失败:', error)
  }
}

// 仅加载订单数量（用于徽章显示）
const loadOrderCount = async () => {
  try {
    const res = await getOrders({
      page: 1,
      pageSize: 1  // 只需要获取总数
    })
    orderCount.value = res.data?.total || 0
  } catch (error) {
    console.error('加载订单数量失败:', error)
  }
}

const loadDemands = async () => {
  loading.demand = true
  try {
    console.log('开始加载求购列表...')
    const res = await getMyProducts({
      page: pagination.page,
      pageSize: pagination.pageSize,
      type: 'demand'  // 只获取求购类型
    })
    console.log('求购API响应:', res)

    // 后端已按类型过滤，直接使用返回的数据
    demands.value = res.data?.records || []
    pagination.total = res.data?.total || 0

    // 使用后端返回的总数（仅求购类型的总数）
    demandCount.value = pagination.total

    console.log('求购数据:', demands.value)
    console.log('求购总数:', demandCount.value)
  } catch (error) {
    console.error('加载求购列表失败:', error)
    ElMessage.error('加载求购列表失败')
  } finally {
    loading.demand = false
  }
}

const loadOrders = async () => {
  loading.order = true
  try {
    console.log('开始加载订单列表...')
    const res = await getOrders({
      page: pagination.page,
      pageSize: pagination.pageSize,
      status: orderStatus.value === 'all' ? undefined : orderStatus.value
    })
    console.log('订单API响应:', res)

    // 后端返回 OrderPageResponse { records, total, page, pageSize }
    orders.value = res.data?.records || []
    pagination.total = res.data?.total || 0

    // 订单数量显示总数量（所有订单，不限状态）
    orderCount.value = pagination.total
    console.log('订单数据:', orders.value)
    console.log('订单总数:', orderCount.value)
  } catch (error) {
    console.error('加载订单列表失败:', error)
    ElMessage.error('加载订单列表失败')
  } finally {
    loading.order = false
  }
}

const getDemandStatusType = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success' }
  return map[status] || 'info'
}

const getDemandStatusText = (status) => {
  const map = { 0: '待交易', 1: '交易中', 2: '已完成' }
  return map[status] || '未知'
}

const getOrderStatusType = (status) => {
  const map = { 1: 'warning', 2: 'primary', 3: 'info', 4: 'success', 5: 'danger' }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString('zh-CN')
}

const handleImageError = (e) => {
  e.target.src = '/placeholder.jpg'
}

// 求购操作
const editDemand = (item) => {
  // 打开编辑对话框并填充数据
  editForm.productId = item.productId || item.orderId
  editForm.title = item.title || ''
  editForm.content = item.content || ''
  editForm.price = item.price || 0
  editForm.picPath = item.picPath || ''
  editForm.type = 'demand'
  editDialogVisible.value = true
}

const handleSaveEdit = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      await updateProduct(editForm.productId, editForm)
      ElMessage.success('修改成功')
      editDialogVisible.value = false
      await loadDemands()
    } catch (error) {
      console.error('修改失败:', error)
      ElMessage.error(error.message || '修改失败')
    } finally {
      saving.value = false
    }
  })
}

// 图片上传前的验证
const beforeImageUpload = (raw) => {
  const isImg = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'].includes(raw.type)
  if (!isImg) {
    ElMessage.error('只能上传图片')
    return false
  }
  if (raw.size / 1024 / 1024 > 5) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

// 图片上传成功回调
const handleImageSuccess = (response) => {
  if (response.code === 200) {
    editForm.picPath = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const cancelDemand = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤单「${item.title}」吗？`,
      '撤单确认',
      { type: 'warning' }
    )
    // 使用正确的 ID 字段名
    const id = item.productId || item.orderId
    if (!id) {
      ElMessage.error('商品ID无效')
      return
    }
    await deleteProductApi(id)
    ElMessage.success('撤单成功')
    await loadDemands()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤单失败:', error)
      ElMessage.error(error.message || '撤单失败')
    }
  }
}

const goToPublish = () => {
  if (userStore.userInfo?.role === 'farmer') {
    router.push('/farmer/products')
  } else {
    router.push('/market')
  }
}

// 订单操作 - 只有已付款订单才能查看详情
const viewOrderDetail = (purchaseId) => {
  // 先在当前列表中找到该订单检查状态
  const order = orders.value.find(o => o.purchaseId === purchaseId)
  if (!order) return

  // 只有已付款的订单才能查看详情（状态>1：待发货、待收货、已完成）
  if (order.purchaseStatus <= 1) {
    ElMessage.warning('请先完成支付后再查看订单详情')
    return
  }

  router.push(`/order/${purchaseId}`)
}

const cancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消订单 ${order.purchaseId} 吗？`,
      '取消订单',
      { type: 'warning' }
    )
    await cancelOrderApi(order.purchaseId)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error(error.message || '取消订单失败')
    }
  }
}

// 确认收货：买家在列表直接确认（status 3→4），无需进入详情页
const confirmReceipt = async (order) => {
  try {
    await ElMessageBox.confirm(
      '确认已收到商品吗？确认后订单将完成。',
      '确认收货',
      { type: 'info' }
    )
    confirmingId.value = order.purchaseId
    await updateOrderStatus(order.purchaseId, { status: 4 })
    ElMessage.success('确认收货成功')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认收货失败:', error)
      ElMessage.error(error?.response?.data?.message || error?.message || '确认收货失败')
    }
  } finally {
    confirmingId.value = null
  }
}

// 发起支付宝扫码支付：生成二维码并在弹窗展示，同时轮询支付结果
const payOrderNow = async (order) => {
  payingId.value = order.purchaseId
  try {
    const res = await payOrder(order.purchaseId)
    const qrCodeUrl = res?.data?.qrCode
    if (!qrCodeUrl) {
      ElMessage.error('二维码生成失败')
      return
    }
    // 把支付宝返回的二维码链接渲染成图片
    qrImgSrc.value = await QRCode.toDataURL(qrCodeUrl, { width: 240, margin: 1 })
    payOrderInfo.value = order
    payQrVisible.value = true
    startPayPolling(order.purchaseId)
  } catch (error) {
    console.error('发起支付失败:', error)
    ElMessage.error(error.message || '发起支付失败')
  } finally {
    payingId.value = null
  }
}

// 轮询支付结果，每 2.5s 查一次，查到已支付即关闭弹窗并刷新订单。
// 加单次查询在途(payQuerying)与成功已通知(payDone)两道闸，避免网络抖动时多 tick 叠加、
// 或手动按钮与轮询并发，导致「支付成功」弹窗多次。
const startPayPolling = (purchaseId) => {
  stopPayPolling()
  payPolling.value = true
  payDone = false
  payQuerying = false
  const tick = async () => {
    if (payDone || payQuerying) return
    payQuerying = true
    try {
      const res = await queryPay(purchaseId)
      if (res?.data?.paid && !payDone) {
        payDone = true
        ElMessage.success('支付成功')
        stopPayPolling()
        payQrVisible.value = false
        await loadOrders()
      }
    } catch (e) {
      // 单次查询失败先忽略，下一轮继续（订单归属/网络等瞬时异常）
    } finally {
      payQuerying = false
    }
  }
  payTimer = setInterval(tick, 2500)
}

const stopPayPolling = () => {
  payPolling.value = false
  payQuerying = false
  if (payTimer) {
    clearInterval(payTimer)
    payTimer = null
  }
}

// 手动触发查询（弹窗里「我已支付」按钮）；已成功则不再重复弹窗
const refreshPayStatus = async (purchaseId) => {
  if (!purchaseId || payDone) return
  if (payQuerying) {
    ElMessage.info('正在查询，请稍候')
    return
  }
  payQuerying = true
  try {
    const res = await queryPay(purchaseId)
    if (res?.data?.paid) {
      payDone = true
      ElMessage.success('支付成功')
      stopPayPolling()
      payQrVisible.value = false
      await loadOrders()
    } else {
      ElMessage.warning('暂未检测到支付，请完成扫码后重试')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '查询失败，请稍后重试')
  } finally {
    payQuerying = false
  }
}

const handleQrDialogClose = () => {
  stopPayPolling()
  qrImgSrc.value = ''
  payOrderInfo.value = null
}

const goToMarket = () => {
  router.push('/market')
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.order-list-container {
  max-width: 1000px;
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
}

.back-btn {
  flex-shrink: 0;
}

.order-list-container h2 {
  margin-bottom: 20px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-filter {
  margin-bottom: 16px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.demand-card,
.order-card {
  transition: box-shadow 0.2s;
}

.demand-card:hover,
.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.card-title {
  flex: 1;
  font-weight: 600;
  color: #303133;
}

.card-content {
  display: flex;
  gap: 16px;
  padding: 16px 0;
}

.content-left img,
.no-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
}

.no-image {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 12px;
}

.content-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.description {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.meta-info {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

/* 订单特定样式 */
.items-preview {
  display: flex;
  gap: 8px;
}

.item-thumb {
  position: relative;
  width: 60px;
  height: 60px;
}

.item-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.more-items {
  position: absolute;
  bottom: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 10px;
  padding: 2px 4px;
  border-radius: 2px;
}

.no-items {
  color: #999;
  font-size: 13px;
}

.order-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}

.total-price {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
}

.address {
  color: #909399;
  font-size: 12px;
}

.seller {
  color: #606266;
  font-size: 13px;
}

.el-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .card-content {
    flex-direction: column;
  }

  .content-left img,
  .no-image {
    width: 60px;
    height: 60px;
  }
}

/* 编辑对话框样式 */
.unit-suffix {
  margin-left: 8px;
  font-size: 13px;
  color: #909399;
}

/* 商品图片上传区域 */
.product-image-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-preview {
  width: 120px;
  height: 120px;
  border: 2px dashed #e5e0d8;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f5f0;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: #6b7280;
  font-size: 13px;
}

.image-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.divider {
  color: #6b7280;
  font-size: 13px;
}

.image-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.image-option img {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border-radius: 4px;
}

/* 支付二维码对话框 */
.pay-qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}
.pay-qr-tip {
  margin: 0;
  font-size: 13px;
  color: #606266;
  text-align: center;
}
.pay-qr-img {
  width: 240px;
  height: 240px;
}
.pay-qr-amount {
  margin: 0;
  font-size: 14px;
  color: #f56c6c;
  font-weight: 600;
}
.pay-qr-status {
  margin: 0;
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
