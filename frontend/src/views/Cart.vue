<template>
  <div class="cart-container">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft" class="back-btn">返回</el-button>
      <h2>购物车</h2>
    </div>

    <el-empty v-if="cartItems.length === 0" description="购物车为空" />

    <div v-else>
      <el-table :data="cartItems" style="width: 100%">
        <el-table-column label="商品">
          <template #default="{ row }">
            <div class="product-cell">
              <img :src="row.picPath" class="product-img" />
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" prop="price" width="120">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :min="1" :max="row.stock"
                             @change="updateQuantity(row)" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" link @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="cart-footer">
        <div class="total">
          已选商品 <span class="count">{{ totalQuantity }}</span> 件
          合计: <span class="amount">¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        <el-button type="primary" size="large" @click="checkout">结算</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getCartList, updateCartItem, deleteCartItem } from '@/api/cart'
import { createOrder } from '@/api/order'

const router = useRouter()

const cartItems = ref([])

const totalQuantity = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
})

const totalPrice = computed(() => {
  return cartItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
})

onMounted(async () => {
  await loadCart()
})

const loadCart = async () => {
  try {
    const res = await getCartList()
    cartItems.value = res.data || []
  } catch (error) {
    ElMessage.error('加载购物车失败')
  }
}

const updateQuantity = async (item) => {
  try {
    await updateCartItem(item.cartId, item.quantity)
  } catch (error) {
    ElMessage.error('更新失败')
    await loadCart()
  }
}

const removeItem = async (item) => {
  try {
    await ElMessageBox.confirm('确定删除该商品吗？', '提示', {
      type: 'warning'
    })
    await deleteCartItem(item.cartId)
    ElMessage.success('删除成功')
    await loadCart()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const checkout = async () => {
  if (cartItems.value.length === 0) {
    ElMessage.warning('请先添加商品到购物车')
    return
  }
  try {
    const orderItems = cartItems.value.map(item => ({
      productId: item.productId,
      quantity: item.quantity
    }))
    await createOrder({ items: orderItems })
    ElMessage.success('订单创建成功')
    router.push('/order')
  } catch (error) {
    ElMessage.error(error.message || '创建订单失败')
  }
}

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.cart-container {
  max-width: 1200px;
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

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.product-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.cart-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 20px;
  background: white;
  margin-top: 20px;
  gap: 20px;
}

.total {
  font-size: 16px;
}

.count {
  color: #409eff;
  font-weight: bold;
}

.amount {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
}
</style>
