<template>
  <div class="cart-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>购物车</h1>
      <p class="cart-count">共 {{ cartList.length }} 件商品</p>
    </div>

    <!-- 购物车为空 -->
    <div v-if="!loading && cartList.length === 0" class="empty-cart">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="9" cy="21" r="1"/>
          <circle cx="20" cy="21" r="1"/>
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
        </svg>
      </div>
      <h2>购物车是空的</h2>
      <p>快去选购心仪的农产品吧~</p>
      <el-button type="primary" @click="goToMarket">去逛逛</el-button>
    </div>

    <!-- 购物车列表 -->
    <div v-else class="cart-content">
      <!-- 表头 -->
      <div class="cart-header">
        <el-checkbox
          v-model="selectAll"
          :indeterminate="isIndeterminate"
          @change="handleSelectAll"
        >
          全选
        </el-checkbox>
        <span class="header-product">商品信息</span>
        <span class="header-price">单价</span>
        <span class="header-quantity">数量</span>
        <span class="header-total">小计</span>
        <span class="header-action">操作</span>
      </div>

      <!-- 购物车列表 -->
      <div v-loading="loading" class="cart-list">
        <div
          v-for="item in cartList"
          :key="item.cartId"
          class="cart-item"
          :class="{
            'item-selected': selectedIds.includes(item.cartId),
            'item-invalid': item.isInvalid
          }"
        >
          <div class="item-checkbox">
            <el-checkbox
              v-model="item.selected"
              :disabled="item.isInvalid"
              @change="handleItemSelect"
            />
          </div>

          <div
            class="item-product"
            :class="{ 'invalid-product': item.isInvalid }"
            @click="!item.isInvalid && goToProduct(item.productId)"
          >
            <div class="product-image">
              <img
                :src="item.picPath || '/placeholder.jpg'"
                :alt="item.title"
                @error="handleImageError"
              />
              <div v-if="item.isInvalid" class="invalid-badge">已下架</div>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ item.title }}</h3>
              <p class="product-desc">{{ item.content || '暂无描述' }}</p>
            </div>
          </div>

          <div class="item-price">
            <span v-if="!item.isInvalid" class="price-value">¥{{ item.price }}</span>
            <span v-else class="price-invalid">—</span>
          </div>

          <div class="item-quantity">
            <el-input-number
              v-if="!item.isInvalid"
              v-model="item.count"
              :min="1"
              :max="99"
              size="small"
              @change="(value) => handleQuantityChange(item, value)"
            />
            <span v-else class="quantity-invalid">—</span>
          </div>

          <div class="item-total">
            <span v-if="!item.isInvalid" class="total-value">¥{{ (item.price * item.count).toFixed(2) }}</span>
            <span v-else class="total-invalid">—</span>
          </div>

          <div class="item-action">
            <el-button
              type="danger"
              size="small"
              link
              @click="handleDelete(item)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="cart-footer">
        <div class="footer-left">
          <el-checkbox
            v-model="selectAll"
            :indeterminate="isIndeterminate"
            @change="handleSelectAll"
          >
            全选
          </el-checkbox>
          <el-button
            type="danger"
            link
            size="small"
            @click="handleDeleteSelected"
            :disabled="selectedIds.length === 0"
          >
            删除选中
          </el-button>
        </div>

        <div class="footer-right">
          <div class="selected-info">
            已选择 <strong>{{ selectedIds.length }}</strong> 件商品
          </div>
          <div class="total-section">
            <span class="total-label">合计：</span>
            <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <el-button
            type="primary"
            size="large"
            :disabled="selectedIds.length === 0"
            @click="handleCheckout"
          >
            结算
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCartList, updateCartItem, deleteCartItem } from '@/api/cart'
import { createOrder } from '@/api/order'

const router = useRouter()

const loading = ref(false)
const cartList = ref([])

// 全选状态
const selectAll = ref(false)
const isIndeterminate = ref(false)

// 选中的商品ID列表
const selectedIds = computed(() => {
  return cartList.value
    .filter(item => item.selected && !item.isInvalid)
    .map(item => item.cartId)
})

// 计算总价
const totalPrice = computed(() => {
  return cartList.value
    .filter(item => item.selected)
    .reduce((sum, item) => sum + (item.price * item.count), 0)
})

// 调试：监听 cartList 变化
watch(cartList, (newVal) => {
  console.log('=== cartList 变化了 ===', newVal.length, '项')
  nextTick(() => {
    console.log('=== nextTick 后 DOM 应已更新 ===')
  })
}, { deep: true })

// 调试：检查渲染状态
const debugInfo = computed(() => {
  return {
    loading: loading.value,
    cartListLength: cartList.value.length,
    shouldShowEmpty: !loading.value && cartList.value.length === 0,
    shouldShowContent: !loading.value && cartList.value.length > 0
  }
})

watch(debugInfo, (newVal) => {
  console.log('=== 调试信息 ===', newVal)
}, { immediate: true })

onMounted(() => {
  console.log('=== Cart onMounted 触发 ===')
  loadCartList()
})

// 加载购物车列表
const loadCartList = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    console.log('=== 购物车原始数据 ===', res.data)

    // 过滤掉商品信息不完整的项，并标记
    const validItems = []
    const invalidItems = []

    ;(res.data || []).forEach(item => {
      // 确保字段映射正确
      const cartId = item.cartId || item.shoppingId
      const productId = item.productId || item.orderId

      // 检查商品信息是否完整
      if (!item.title || !item.price) {
        console.warn('商品信息不完整:', item)
        invalidItems.push({
          ...item,
          cartId: cartId,
          productId: productId,
          selected: false,
          isInvalid: true,
          title: '商品已下架',
          price: 0,
          picPath: '',
          content: '该商品已下架或删除'
        })
      } else {
        validItems.push({
          ...item,
          cartId: cartId,
          productId: productId,
          selected: false,
          isInvalid: false
        })
      }
    })

    cartList.value = [...validItems, ...invalidItems]
    console.log('=== 有效商品项 ===', validItems.length)
    console.log('=== 无效商品项 ===', invalidItems.length)

    // 如果有无效商品，提示用户
    if (invalidItems.length > 0) {
      ElMessage.warning(`发现 ${invalidItems.length} 个已下架的商品`)
    }

    updateSelectState()
  } catch (error) {
    console.error('加载购物车失败:', error)
    ElMessage.error('加载购物车失败')
  } finally {
    loading.value = false
    console.log('=== loading 状态已清除 ===', loading.value)
    console.log('=== cartList 长度 ===', cartList.value.length)
  }
}

// 更新全选状态
const updateSelectState = () => {
  const selectedCount = cartList.value.filter(item => item.selected).length
  selectAll.value = selectedCount === cartList.value.length && cartList.value.length > 0
  isIndeterminate.value = selectedCount > 0 && selectedCount < cartList.value.length
}

// 全选/取消全选
const handleSelectAll = (checked) => {
  cartList.value.forEach(item => {
    item.selected = checked
  })
  isIndeterminate.value = false
}

// 单个商品选中状态变化
const handleItemSelect = () => {
  updateSelectState()
}

// 修改商品数量
const handleQuantityChange = async (item, value) => {
  if (!item.cartId) {
    ElMessage.error('购物车项ID无效')
    return
  }

  const originalValue = item.count
  try {
    await updateCartItem(item.cartId, value)
    ElMessage.success('修改成功')
  } catch (error) {
    console.error('修改数量失败:', error)
    ElMessage.error(error.message || '修改数量失败')
    item.count = originalValue
  }
}

// 删除单个商品
const handleDelete = async (item) => {
  if (!item.cartId) {
    ElMessage.error('购物车项ID无效')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除 "${item.title}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    await deleteCartItem(item.cartId)
    ElMessage.success('删除成功')

    // 从列表中移除
    const index = cartList.value.findIndex(i => i.cartId === item.cartId)
    if (index > -1) {
      cartList.value.splice(index, 1)
    }
    updateSelectState()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 删除选中的商品
const handleDeleteSelected = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的商品')
    return
  }

  // 验证所有ID都有效
  const validIds = selectedIds.value.filter(id => id != null)
  if (validIds.length === 0) {
    ElMessage.error('没有有效的购物车项')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${validIds.length} 件商品吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    // 逐个删除
    for (const cartId of validIds) {
      await deleteCartItem(cartId)
    }

    ElMessage.success('删除成功')
    // 重新加载列表
    await loadCartList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    loading.value = false
  }
}

// 结算：跳转到结算确认页面
const handleCheckout = () => {
  const selected = cartList.value.filter(i => i.selected && !i.isInvalid)
  if (selected.length === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }

  // 将选中的商品信息传递给结算页面
  const items = selected.map(i => ({
    cartId: i.cartId,
    productId: i.productId,
    orderId: i.productId,
    title: i.title,
    content: i.content,
    price: i.price,
    count: i.count,
    picPath: i.picPath
  }))

  router.push({
    path: '/buyer/checkout',
    query: { items: JSON.stringify(items) }
  })
}

// 跳转到商品详情
const goToProduct = (productId) => {
  router.push(`/product/${productId}`)
}

// 跳转到市场页面
const goToMarket = () => {
  router.push('/buyer/market')
}

// 图片加载失败处理
const handleImageError = (e) => {
  e.target.src = '/placeholder.jpg'
}
</script>

<style scoped>
.cart-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-8, 32px) var(--spacing-6, 24px);
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-6, 24px);
}

.page-header h1 {
  font-size: var(--font-size-2xl, 32px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0;
}

.cart-count {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

/* 空购物车 */
.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  padding: var(--spacing-8, 32px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-lg, 12px);
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin-bottom: var(--spacing-4, 16px);
  color: var(--color-text-tertiary, #6b7280);
  opacity: 0.3;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-cart h2 {
  font-size: var(--font-size-xl, 24px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.empty-cart p {
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-secondary, #4a5249);
  margin: 0 0 var(--spacing-4, 16px);
}

/* 购物车内容 */
.cart-content {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  overflow: hidden;
}

/* 表头 */
.cart-header {
  display: grid;
  grid-template-columns: 50px 1fr 120px 140px 120px 80px;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-bottom: 1px solid var(--color-border, #e5e0d8);
  align-items: center;
}

.cart-header span {
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-secondary, #4a5249);
}

.header-product {
  grid-column: 2;
}

.header-price {
  grid-column: 3;
  text-align: center;
}

.header-quantity {
  grid-column: 4;
  text-align: center;
}

.header-total {
  grid-column: 5;
  text-align: center;
}

.header-action {
  grid-column: 6;
  text-align: center;
}

/* 购物车列表 */
.cart-list {
  max-height: 600px;
  overflow-y: auto;
}

.cart-item {
  display: grid;
  grid-template-columns: 50px 1fr 120px 140px 120px 80px;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-border, #e5e0d8);
  align-items: center;
  transition: background-color 0.2s;
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
}

/* 无效商品样式 */
.cart-item.item-invalid {
  background: rgba(185, 92, 56, 0.05);
  opacity: 0.8;
}

.cart-item.item-invalid:hover {
  background: rgba(185, 92, 56, 0.08);
}

.invalid-product {
  cursor: not-allowed !important;
}

.product-image {
  position: relative;
}

.invalid-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: var(--spacing-2, 8px) var(--spacing-3, 12px);
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-semibold, 600);
  border-radius: var(--radius-base, 8px);
}

.price-invalid,
.quantity-invalid,
.total-invalid {
  color: var(--color-text-tertiary, #6b7280);
  font-size: var(--font-size-sm, 14px);
}

.item-checkbox {
  grid-column: 1;
}

.item-product {
  grid-column: 2;
  display: flex;
  gap: var(--spacing-3, 12px);
  cursor: pointer;
}

.product-image {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: var(--radius-base, 8px);
  overflow: hidden;
  background: var(--color-bg-tertiary, #e8e4dc);
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--spacing-1, 4px);
  min-width: 0;
}

.product-name {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-price {
  grid-column: 3;
  text-align: center;
}

.price-value {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
}

.item-quantity {
  grid-column: 4;
  display: flex;
  justify-content: center;
}

.item-total {
  grid-column: 5;
  text-align: center;
}

.total-value {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-error, #b85c38);
}

.item-action {
  grid-column: 6;
  text-align: center;
}

/* 底部结算栏 */
.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-4, 16px) var(--spacing-6, 24px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-top: 1px solid var(--color-border, #e5e0d8);
}

.footer-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-4, 16px);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-4, 16px);
}

.selected-info {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.selected-info strong {
  color: var(--color-primary, #2d5a3d);
  font-size: var(--font-size-lg, 20px);
}

.total-section {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-1, 4px);
}

.total-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.total-price {
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-error, #b85c38);
}

/* 响应式 */
@media (max-width: 1024px) {
  .cart-header,
  .cart-item {
    grid-template-columns: 50px 1fr 100px 120px 100px 60px;
    gap: var(--spacing-2, 8px);
  }

  .cart-header span,
  .price-value,
  .total-value {
    font-size: var(--font-size-sm, 14px);
  }

  .total-price {
    font-size: var(--font-size-2xl, 32px);
  }
}

@media (max-width: 768px) {
  .cart-page {
    padding: var(--spacing-4, 16px);
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-2, 8px);
  }

  .cart-header {
    display: none;
  }

  .cart-item {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
    gap: var(--spacing-3, 12px);
    padding: var(--spacing-4, 16px);
  }

  .item-checkbox {
    grid-column: 1;
    grid-row: 1;
  }

  .item-product {
    grid-column: 1;
    grid-row: 2;
  }

  .item-price,
  .item-quantity,
  .item-total,
  .item-action {
    grid-column: 1;
    text-align: left;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .item-price::before {
    content: '单价：';
    color: var(--color-text-tertiary, #6b7280);
  }

  .item-quantity::before {
    content: '数量：';
    color: var(--color-text-tertiary, #6b7280);
  }

  .item-total::before {
    content: '小计：';
    color: var(--color-text-tertiary, #6b7280);
  }

  .cart-footer {
    flex-direction: column;
    gap: var(--spacing-3, 12px);
    padding: var(--spacing-4, 16px);
  }

  .footer-left,
  .footer-right {
    width: 100%;
    justify-content: space-between;
  }

  .total-price {
    font-size: var(--font-size-2xl, 32px);
  }
}
</style>
