<template>
  <div class="checkout-page">
    <div class="page-header">
      <h1>确认订单</h1>
    </div>

    <div class="checkout-content" v-loading="loading">
      <!-- 收货地址 -->
      <div class="section address-section">
        <div class="section-header">
          <h3>收货地址</h3>
        </div>

        <!-- 无地址时显示 -->
        <el-empty v-if="addresses.length === 0" description="暂无地址，请添加收货地址" :image-size="80">
          <el-button type="primary" @click="showAddAddressDialog">新增地址</el-button>
        </el-empty>

        <!-- 有地址时显示 -->
        <template v-else>
          <!-- 当前选中的地址 -->
          <div v-if="selectedAddress" class="selected-address-card" @click="showAddressSelector = true">
            <div class="address-check">
              <el-icon class="check-icon"><CircleCheck /></el-icon>
              <span class="check-text">已选择</span>
            </div>
            <div class="address-content">
              <div class="address-info">
                <span class="consignee">{{ selectedAddress.consignee }}</span>
                <span class="phone">{{ selectedAddress.phone }}</span>
              </div>
              <div class="address-full">
                {{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.area }} {{ selectedAddress.addressDetail }}
              </div>
            </div>
            <div class="address-change">
              <span>切换地址</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>

          <!-- 地址选择抽屉 -->
          <el-drawer v-model="showAddressSelector" title="选择收货地址" direction="rtl" size="400">
            <div class="address-selector">
              <div class="selector-header">
                <el-button type="primary" @click="showAddAddressDialog">
                  <el-icon><Plus /></el-icon>
                  新增收货地址
                </el-button>
              </div>

              <div class="address-list-drawer">
                <div
                  v-for="address in addresses"
                  :key="address.id"
                  class="address-list-item"
                  :class="{ 'selected': selectedAddress && selectedAddress.id === address.id }"
                  @click="selectAddressFromDrawer(address)"
                >
                  <div class="item-check">
                    <el-icon v-if="selectedAddress && selectedAddress.id === address.id" class="checked-icon">
                      <CircleCheckFilled />
                    </el-icon>
                    <span v-else class="unchecked-icon">○</span>
                  </div>
                  <div class="item-content">
                    <div class="item-info">
                      <span class="item-name">{{ address.consignee }}</span>
                      <span class="item-phone">{{ address.phone }}</span>
                      <el-tag v-if="address.isDefault === 1" type="success" size="small">默认</el-tag>
                    </div>
                    <div class="item-detail">
                      {{ address.province }} {{ address.city }} {{ address.area }} {{ address.addressDetail }}
                    </div>
                    <div class="item-actions">
                      <el-button link type="primary" size="small" @click.stop="editAddress(address)">编辑</el-button>
                      <el-button
                        v-if="address.isDefault !== 1"
                        link
                        type="warning"
                        size="small"
                        @click.stop="setDefault(address)"
                      >
                        设为默认
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-drawer>
        </template>
      </div>

      <!-- 商品列表 -->
      <div class="section">
        <div class="section-header">
          <h3>商品信息</h3>
        </div>

        <div class="product-list">
          <div v-for="item in items" :key="item.orderId" class="product-item">
            <div class="product-image">
              <img :src="item.picPath || '/placeholder.jpg'" :alt="item.title" />
            </div>
            <div class="product-info">
              <div class="product-name">{{ item.title }}</div>
              <div class="product-desc">{{ item.content || '暂无描述' }}</div>
              <div class="product-price">¥{{ item.price }} × {{ item.count }}</div>
            </div>
            <div class="product-total">
              <span class="total-price">¥{{ (item.price * item.count).toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <div class="order-total">
          <div class="total-row">
            <span>商品总额：</span>
            <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <div class="total-row final">
            <span>应付总额：</span>
            <span class="final-price">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <!-- 提交订单 -->
      <div class="submit-section">
        <el-button
          type="primary"
          size="large"
          :disabled="!selectedAddress"
          @click="submitOrder"
          :loading="submitting"
        >
          提交订单
        </el-button>
      </div>
    </div>

    <!-- 添加/编辑地址对话框 -->
    <el-dialog
      v-model="addressDialogVisible"
      :title="editingAddressData ? '编辑地址' : '添加地址'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="addressForm" :rules="addressRules" ref="addressFormRef" label-width="80px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="addressForm.realName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="addressForm.phone" placeholder="请输入手机号码" />
        </el-form-item>
        <el-form-item label="地区" prop="region">
          <el-cascader
            v-model="addressRegion"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover', value: 'label', label: 'label' }"
            placeholder="请选择省/市/区"
            @change="handleAddressRegionChange"
            style="width: 100%"
            clearable
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="addressForm.detail"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址，如街道、楼栋号、门牌号等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress" :loading="savingAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, CircleCheck, CircleCheckFilled, ArrowRight } from '@element-plus/icons-vue'
import { createOrder } from '@/api/order'
import { deleteCartItem } from '@/api/cart'
import {
  getAddressList,
  getDefaultAddress,
  addAddress,
  updateAddress,
  setDefaultAddress as setDefaultAddr
} from '@/api/address'
import { regionData } from 'element-china-area-data'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const showAddressSelector = ref(false)
const addresses = ref([])
const selectedAddress = ref(null)
const items = ref([])

// 地址对话框
const addressDialogVisible = ref(false)
const editingAddressData = ref(null)
const savingAddress = ref(false)
const addressFormRef = ref()
const addressRegion = ref([])

const addressForm = reactive({
  realName: '',
  phone: '',
  province: '',
  city: '',
  area: '',
  detail: ''
})

const addressRules = {
  realName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (!/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('请输入正确的手机号码'))
      } else {
        callback()
      }
    }, trigger: 'blur' }
  ],
  region: [{ required: true, validator: (rule, value, callback) => {
    if (!addressRegion.value || addressRegion.value.length === 0) {
      callback(new Error('请选择地区'))
    } else {
      callback()
    }
  }, trigger: 'change' }],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const regionOptions = regionData

// 计算总价
const totalPrice = computed(() => {
  return items.value.reduce((sum, item) => sum + (item.price * item.count), 0)
})

onMounted(async () => {
  // 从路由参数获取商品信息
  const checkoutItems = route.query.items
  if (checkoutItems) {
    try {
      items.value = JSON.parse(checkoutItems)
    } catch (e) {
      ElMessage.error('商品信息错误')
      router.back()
      return
    }
  } else {
    ElMessage.error('缺少商品信息')
    router.back()
    return
  }

  // 加载地址列表
  await loadAddresses()
})

const loadAddresses = async () => {
  loading.value = true
  try {
    const res = await getAddressList()
    addresses.value = res.data || []

    // 获取默认地址
    try {
      const defaultRes = await getDefaultAddress()
      if (defaultRes.data) {
        selectedAddress.value = defaultRes.data
      } else if (addresses.value.length > 0) {
        // 如果没有默认地址，选择第一个
        selectedAddress.value = addresses.value[0]
      }
    } catch (e) {
      if (addresses.value.length > 0) {
        selectedAddress.value = addresses.value[0]
      }
    }
  } catch (error) {
    ElMessage.error('加载地址失败')
  } finally {
    loading.value = false
  }
}

const selectAddress = (address) => {
  selectedAddress.value = address
}

const selectAddressFromDrawer = (address) => {
  selectedAddress.value = address
  showAddressSelector.value = false
}

const showAddAddressDialog = () => {
  editingAddressData.value = null
  // 预填充收货人姓名/电话（来自个人信息），用户仍可手动修改；省市区与详细地址留空待填
  const ui = userStore.userInfo || {}
  Object.assign(addressForm, {
    realName: ui.realName || '',
    phone: ui.phone || '',
    province: '',
    city: '',
    area: '',
    detail: ''
  })
  addressRegion.value = []
  addressDialogVisible.value = true
}

const editAddress = (address) => {
  editingAddressData.value = address
  Object.assign(addressForm, {
    realName: address.consignee,
    phone: address.phone,
    province: address.province,
    city: address.city,
    area: address.area,
    detail: address.addressDetail
  })
  addressRegion.value = [address.province, address.city, address.area]
  addressDialogVisible.value = true
}

const handleAddressRegionChange = (value) => {
  if (value && value.length === 3) {
    addressForm.province = value[0]
    addressForm.city = value[1]
    addressForm.area = value[2]
  }
}

const saveAddress = async () => {
  try {
    await addressFormRef.value.validate()
    savingAddress.value = true

    if (editingAddressData.value) {
      await updateAddress(editingAddressData.value.id, addressForm)
      ElMessage.success('修改成功')
    } else {
      await addAddress(addressForm)
      ElMessage.success('添加成功')
    }

    addressDialogVisible.value = false
    await loadAddresses()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    savingAddress.value = false
  }
}

const setDefault = async (address) => {
  try {
    await setDefaultAddr(address.id)
    ElMessage.success('设置成功')
    await loadAddresses()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

const submitOrder = async () => {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址')
    return
  }

  const fullAddress = `${selectedAddress.value.province}${selectedAddress.value.city}${selectedAddress.value.area}${selectedAddress.value.addressDetail}`

  submitting.value = true
  try {
    await createOrder({
      purchaseType: 2,
      address: fullAddress,
      items: items.value.map(item => ({
        orderId: item.orderId || item.productId,
        count: item.count
      }))
    })

    // 清理购物车中的商品（如果有 cartId）
    for (const item of items.value) {
      if (item.cartId) {
        try {
          await deleteCartItem(item.cartId)
        } catch (e) {
          console.error('清理购物车失败:', e)
        }
      }
    }

    ElMessage.success('下单成功')
    router.push('/buyer/orders')
  } catch (error) {
    ElMessage.error(error.message || '下单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-page {
  max-width: 900px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  color: #303133;
}

.checkout-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

/* 地址列表 */
.address-section {
  position: relative;
}

/* 选中地址卡片 */
.selected-address-card {
  border: 2px solid #67c23a;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  transition: all 0.3s;
  background: linear-gradient(135deg, #f0f9ff 0%, #fff 100%);
}

.selected-address-card:hover {
  box-shadow: 0 4px 16px rgba(103, 194, 58, 0.2);
  transform: translateY(-2px);
}

.address-check {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #67c23a;
  color: white;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.check-icon {
  font-size: 18px;
}

.address-content {
  flex: 1;
}

.address-info {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 8px;
}

.consignee {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.phone {
  color: #606266;
  font-size: 14px;
}

.address-full {
  color: #909399;
  line-height: 1.5;
  font-size: 14px;
}

.address-change {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #409eff;
  font-size: 14px;
}

/* 地址选择抽屉 */
.address-selector {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.selector-header {
  padding: 0 0 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.address-list-drawer {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}

.address-list-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #f0f0f0;
  margin-bottom: 10px;
}

.address-list-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
}

.address-list-item.selected {
  border-color: #67c23a;
  background: #f0f9ff;
}

.item-check {
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.checked-icon {
  font-size: 20px;
  color: #67c23a;
}

.unchecked-icon {
  font-size: 20px;
  color: #d0d0d0;
}

.item-content {
  flex: 1;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.item-name {
  font-weight: 500;
  color: #303133;
}

.item-phone {
  color: #909399;
  font-size: 14px;
}

.item-detail {
  color: #606266;
  line-height: 1.5;
  margin-bottom: 10px;
  font-size: 14px;
}

.item-actions {
  display: flex;
  gap: 8px;
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
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.product-item:last-child {
  border-bottom: none;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
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
  gap: 5px;
}

.product-name {
  font-weight: 500;
  color: #303133;
}

.product-desc {
  font-size: 13px;
  color: #909399;
}

.product-price {
  color: #f56c6c;
}

.product-total {
  display: flex;
  align-items: center;
}

.total-price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}

/* 订单总额 */
.order-total {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 2px solid #f0f0f0;
}

.total-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
  color: #606266;
}

.total-row.final {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.final-price {
  font-size: 24px;
  color: #f56c6c;
}

/* 提交按钮 */
.submit-section {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: flex-end;
}
</style>
