<template>
  <div v-loading="loading" class="product-detail-page">
    <div v-if="product" class="detail-container">
      <!-- 返回按钮 -->
      <div class="back-section">
        <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
      </div>

      <!-- 商品详情主体 -->
      <div class="detail-main">
        <!-- 左侧图片区 -->
        <div class="image-section">
          <div class="main-image">
            <img
              :src="product.picPath || '/placeholder.jpg'"
              :alt="product.name"
              @error="handleImageError"
            />
            <div v-if="product.stock <= 0" class="out-of-stock-badge">缺货</div>
          </div>
        </div>

        <!-- 右侧信息区 -->
        <div class="info-section">
          <div class="product-badges">
            <span class="badge-type" :class="product.type === 'goods' ? 'supply' : 'demand'">
              {{ product.type === 'goods' ? '货源供应' : '求购需求' }}
            </span>
            <span class="badge-status" :class="getStatusClass(product.orderStatus)">
              {{ getStatusText(product.orderStatus) }}
            </span>
          </div>

          <h1 class="product-name">{{ product.title }}</h1>
          <div class="product-meta">
            <span class="product-id">商品编号: {{ product.productId }}</span>
            <span class="product-seller">发布方: {{ product.ownName || '农户' }}</span>
            <span v-if="product.ownPhone" class="product-phone">电话: {{ product.ownPhone }}</span>
          </div>

          <div class="price-section" v-if="product.type === 'goods'">
            <span class="price-label">价格</span>
            <span class="price-value">¥{{ product.price }}</span>
            <span class="price-unit">/ kg</span>
          </div>

          <div class="price-section" v-else>
            <span class="price-label">预算</span>
            <span class="price-value">{{ product.price ? '¥' + product.price : '面议' }}</span>
          </div>

          <div class="divider"></div>

          <div class="description-section">
            <h3 class="section-title">{{ product.type === 'goods' ? '商品描述' : '需求描述' }}</h3>
            <p class="description">{{ product.content }}</p>
          </div>

          <div class="meta-section">
            <div class="meta-item">
              <span class="meta-label">发布时间:</span>
              <span class="meta-value">{{ formatTime(product.createTime) }}</span>
            </div>
          </div>

          <div class="action-section" v-if="product.type === 'goods' && product.orderStatus !== 2 && isBuyer">
            <div class="quantity-selector">
              <span class="quantity-label">数量：</span>
              <el-input-number
                v-model="quantity"
                :min="1"
                size="large"
              />
            </div>
            <div class="action-buttons">
              <el-button
                type="primary"
                size="large"
                :disabled="product.orderStatus === 2"
                @click="addToCart"
              >
                <el-icon><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
              <el-button
                type="success"
                size="large"
                :disabled="product.orderStatus === 2"
                @click="buyNow"
              >
                立即购买
              </el-button>
            </div>
          </div>

          <div class="action-section" v-else-if="product.type === 'goods' && product.orderStatus !== 2 && !isBuyer">
            <div class="non-buyer-tip">仅买家账号可购买，如需采购请使用买家账号登录。</div>
          </div>

          <div class="action-section" v-else-if="product.type === 'demand'">
            <template v-if="isOwner">
              <div class="non-buyer-tip">这是您自己发布的求购信息。</div>
            </template>
            <el-button v-else type="primary" size="large" @click="contactSeller">
              <el-icon><ChatLineSquare /></el-icon>
              联系发布方
            </el-button>
          </div>

          <div class="seller-section">
            <div class="seller-info">
              <span class="seller-label">发布方：</span>
              <span class="seller-name">{{ product.ownName || '农户' }}</span>
              <span v-if="product.ownPhone" class="seller-phone">{{ product.ownPhone }}</span>
            </div>
            <el-button v-if="!isOwner" type="primary" link size="small" @click="contactSeller">
              {{ product.type === 'demand' ? '联系发布方' : '联系卖家' }}
            </el-button>
            <span v-else class="seller-self">我发布的</span>
          </div>
        </div>
      </div>

      <!-- 商品详情/评价标签页 -->
      <div class="detail-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div class="tab-content">
              <div class="detail-item">
                <h4>{{ product.type === 'goods' ? '商品名称' : '需求标题' }}</h4>
                <p>{{ product.title }}</p>
              </div>
              <div class="detail-item">
                <h4>{{ product.type === 'goods' ? '商品描述' : '需求描述' }}</h4>
                <p>{{ product.content }}</p>
              </div>
              <div class="detail-item">
                <h4>价格</h4>
                <p>{{ product.type === 'goods' ? `¥${product.price} / kg` : (product.price ? `¥${product.price} / kg` : '面议') }}</p>
              </div>
              <div class="detail-item">
                <h4>类型</h4>
                <p>{{ product.type === 'goods' ? '货源供应' : '求购需求' }}</p>
              </div>
              <div class="detail-item">
                <h4>交易状态</h4>
                <p>{{ getStatusText(product.orderStatus) }}</p>
              </div>
              <div class="detail-item">
                <h4>发布方</h4>
                <p>
                  {{ product.ownName || '未知' }}
                  <span v-if="product.ownPhone" class="detail-phone">（电话：{{ product.ownPhone }}）</span>
                </p>
              </div>
              <div class="detail-item">
                <h4>发布时间</h4>
                <p>{{ formatTime(product.createTime) }}</p>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 加载失败或不存在 -->
    <div v-else-if="!loading" class="not-found">
      <div class="not-found-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12" y2="16"/>
        </svg>
      </div>
      <h2>商品不存在</h2>
      <el-button type="primary" @click="goBack">返回列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ShoppingCart, ChatLineSquare } from '@element-plus/icons-vue'
import { getProductDetail, contactSeller as contactSellerApi } from '@/api/product'
import { addToCart as addCartItem } from '@/api/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const product = ref(null)
const quantity = ref(1)
const activeTab = ref('detail')
const contacting = ref(false)

// 仅买家可购买/加购物车；农户等角色不展示购买入口
const isBuyer = computed(() => userStore.role === 'buyer')

// 是否为当前用户自己发布的信息（发布方 ownName 存真实姓名）：禁止联系自己
const isOwner = computed(() => {
  if (!product.value || !userStore.userInfo) return false
  const me = userStore.userInfo
  return product.value.ownName === me.realName || product.value.ownName === me.userName
})

onMounted(() => {
  loadProductDetail()
})

const loadProductDetail = async () => {
  const id = route.params.id
  console.log('路由参数:', route.params, '商品ID:', id)

  if (!id) {
    ElMessage.error('商品ID不存在')
    goBack()
    return
  }

  loading.value = true
  try {
    const res = await getProductDetail(id)
    console.log('商品详情数据:', res.data)
    product.value = res.data
  } catch (error) {
    console.error('加载商品详情失败:', error)
    ElMessage.error('加载商品详情失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  // 返回来源页。此前用 router.push 固定跳「市场页」，会与市场页自身的 router.back()
  // 互相追加历史记录，造成「市场 ↔ 商品详情」来回跳转的死循环。
  // 改为回退到来源页（pop 当前详情页）；仅当无历史（直接打开/刷新进入）时才兜底跳到角色对应市场。
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(userStore.role === 'buyer' ? '/buyer/market' : '/market')
  }
}

const addToCart = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (!isBuyer.value) {
    ElMessage.warning('仅买家账号可加入购物车')
    return
  }

  if (!product.value) {
    ElMessage.warning('商品信息未加载')
    return
  }

  if (product.value.orderStatus === 2) {
    ElMessage.warning('该商品已完成交易，无法加入购物车')
    return
  }

  if (product.value.type !== 'goods') {
    ElMessage.warning('求购信息无法加入购物车')
    return
  }

  if (!product.value.productId) {
    ElMessage.warning('商品ID无效')
    return
  }

  try {
    console.log('=== 添加购物车参数 ===', {
      orderId: product.value.productId,
      productData: product.value
    })
    await addCartItem({
      orderId: product.value.productId,
      count: quantity.value
    })
    ElMessage.success('已加入购物车')
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error(error.message || '加入购物车失败')
  }
}

const buyNow = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (!isBuyer.value) {
    ElMessage.warning('仅买家账号可购买')
    return
  }

  if (!product.value) {
    ElMessage.warning('商品信息未加载')
    return
  }

  if (product.value.orderStatus === 2) {
    ElMessage.warning('该商品已完成交易')
    return
  }

  if (product.value.type !== 'goods') {
    ElMessage.warning('求购信息无法直接购买')
    return
  }

  if (!product.value.productId) {
    ElMessage.warning('商品ID无效')
    return
  }

  // 立即购买：直接进入结算页走「直接购买」(purchaseType=2)，不再先加入购物车
  // Checkout.vue 从 route.query.items(JSON) 读取待结算商品，提交时 createOrder({purchaseType:2,...})
  const item = {
    orderId: product.value.productId,
    productId: product.value.productId,
    title: product.value.title,
    content: product.value.content,
    price: product.value.price,
    count: quantity.value,
    picPath: product.value.picPath
  }
  router.push({
    path: '/buyer/checkout',
    query: { items: JSON.stringify([item]) }
  })
}

const contactSeller = async () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!product.value) return
  // 询问留言（选填），确认后通过站内通知把联系方式发给发布方
  let message = ''
  try {
    const { value } = await ElMessageBox.prompt(
      '确认后系统会把您的姓名和电话通过通知发给发布方，对方会主动与您联系。可附留言（选填）：',
      product.value.type === 'demand' ? '联系发布方' : '联系卖家',
      {
        confirmButtonText: '发送联系请求',
        cancelButtonText: '取消',
        inputPlaceholder: '留言（选填，如：想了解规格/物流/供货量等）',
        inputType: 'textarea',
        inputValue: ''
      }
    )
    message = (value || '').trim()
  } catch (e) {
    return // 用户取消
  }

  contacting.value = true
  try {
    await contactSellerApi(product.value.productId, message)
    ElMessage.success('已通知发布方，请保持电话畅通')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '发送联系请求失败')
  } finally {
    contacting.value = false
  }
}

const handleImageError = (e) => {
  e.target.src = '/placeholder.jpg'
}

const getStatusText = (status) => {
  const statusMap = {
    0: '待交易',
    1: '交易中',
    2: '已完成'
  }
  return statusMap[status] || '未知'
}

const getStatusClass = (status) => {
  const classMap = {
    0: 'pending',
    1: 'trading',
    2: 'completed'
  }
  return classMap[status] || ''
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
</script>

<style scoped>
.product-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-8, 32px) var(--spacing-6, 24px);
}

/* 返回按钮 */
.back-section {
  margin-bottom: var(--spacing-4, 16px);
}

/* 详情主体 */
.detail-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-8, 32px);
  margin-bottom: var(--spacing-8, 32px);
}

/* 图片区 */
.image-section {
  position: sticky;
  top: var(--spacing-6, 24px);
  height: fit-content;
}

.main-image {
  position: relative;
  aspect-ratio: 1;
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.out-of-stock-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: var(--spacing-3, 12px) var(--spacing-6, 24px);
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  font-size: var(--font-size-xl, 24px);
  font-weight: var(--font-weight-bold, 700);
  border-radius: var(--radius-base, 8px);
}

/* 信息区 */
.info-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
}

/* 徽章区 */
.product-badges {
  display: flex;
  gap: var(--spacing-2, 8px);
  margin-bottom: var(--spacing-2, 8px);
}

.badge-type {
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  border-radius: var(--radius-base, 8px);
}

.badge-type.supply {
  background: rgba(74, 124, 89, 0.1);
  color: var(--color-success, #4a7c59);
}

.badge-type.demand {
  background: rgba(201, 166, 97, 0.1);
  color: var(--color-warning, #c9a661);
}

.badge-status {
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  border-radius: var(--radius-base, 8px);
  background: var(--color-bg-secondary, #f7f5f0);
  color: var(--color-text-tertiary, #6b7280);
}

.badge-status.pending {
  background: rgba(201, 166, 97, 0.1);
  color: var(--color-warning, #c9a661);
}

.badge-status.trading {
  background: rgba(45, 90, 61, 0.1);
  color: var(--color-primary, #2d5a3d);
}

.badge-status.completed {
  background: rgba(74, 124, 89, 0.1);
  color: var(--color-success, #4a7c59);
}

.product-name {
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0;
  line-height: 1.3;
}

.product-meta {
  display: flex;
  gap: var(--spacing-4, 16px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  flex-wrap: wrap;
}

.product-id {
  color: var(--color-text-tertiary, #6b7280);
}

.product-seller {
  color: var(--color-primary, #2d5a3d);
}

/* 价格区 */
.price-section {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-1, 4px);
  padding: var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
}

.price-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.price-value {
  font-size: var(--font-size-4xl, 48px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-error, #b85c38);
}

.price-unit {
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-tertiary, #6b7280);
}

/* 库存区 */
.stock-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
}

.stock-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.stock-value {
  font-size: var(--font-size-base, 16px);
  color: var(--color-success, #4a7c59);
  font-weight: var(--font-weight-semibold, 600);
}

.stock-value.low {
  color: var(--color-warning, #c9a661);
}

.stock-warning {
  padding: var(--spacing-1, 4px) var(--spacing-2, 8px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-warning, #c9a661);
  background: rgba(201, 166, 97, 0.1);
  border-radius: var(--radius-sm, 4px);
}

.divider {
  height: 1px;
  background: var(--color-border, #e5e0d8);
}

/* 描述区 */
.description-section {
  padding: var(--spacing-4, 16px) 0;
}

.section-title {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-3, 12px);
}

.description {
  font-size: var(--font-size-base, 16px);
  line-height: 1.6;
  color: var(--color-text-secondary, #4a5249);
  margin: 0;
}

/* 元信息区 */
.meta-section {
  padding: var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
  display: flex;
  gap: var(--spacing-4, 16px);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  gap: var(--spacing-1, 4px);
}

.meta-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.meta-value {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-primary, #1f2923);
  font-weight: var(--font-weight-medium, 500);
}

/* 操作区 */
.action-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
  padding: var(--spacing-4, 16px) 0;
}

.quantity-selector {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
}

.quantity-label {
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-secondary, #4a5249);
}

.action-buttons {
  display: flex;
  gap: var(--spacing-3, 12px);
}

.action-buttons .el-button {
  flex: 1;
}

/* 卖家区 */
.seller-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
}

.seller-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
}

.seller-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.seller-name {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
}

.product-phone,
.seller-phone {
  color: var(--color-text-tertiary, #6b7280);
}

.seller-phone {
  font-size: var(--font-size-sm, 14px);
  margin-left: var(--spacing-2, 8px);
}

.seller-self {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
}

.detail-phone {
  color: var(--color-text-tertiary, #6b7280);
  font-size: var(--font-size-sm, 14px);
}

.non-buyer-tip {
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-warning, #c9a661);
  background: rgba(201, 166, 97, 0.1);
  border-radius: var(--radius-base, 8px);
}

/* 标签页 */
.detail-tabs {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  overflow: hidden;
}

.tab-content {
  padding: var(--spacing-6, 24px);
}

.detail-item {
  margin-bottom: var(--spacing-6, 24px);
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-item h4 {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.detail-item p {
  font-size: var(--font-size-base, 16px);
  line-height: 1.6;
  color: var(--color-text-secondary, #4a5249);
  margin: 0;
}

/* 评价区（已下线，仅保留占位） */

/* 404 */
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.not-found-icon {
  width: 80px;
  height: 80px;
  margin-bottom: var(--spacing-4, 16px);
  color: var(--color-text-tertiary, #6b7280);
  opacity: 0.5;
}

.not-found-icon svg {
  width: 100%;
  height: 100%;
}

.not-found h2 {
  font-size: var(--font-size-2xl, 32px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-4, 16px);
}

/* 响应式 */
@media (max-width: 768px) {
  .detail-main {
    grid-template-columns: 1fr;
  }

  .image-section {
    position: static;
  }

  .product-name {
    font-size: var(--font-size-2xl, 32px);
  }

  .price-value {
    font-size: var(--font-size-3xl, 38px);
  }

  .action-buttons {
    flex-direction: column;
  }
}

@media (max-width: 480px) {
  .product-detail-page {
    padding: var(--spacing-6, 24px) var(--spacing-4, 16px);
  }

  .product-name {
    font-size: var(--font-size-xl, 24px);
  }
}
</style>
