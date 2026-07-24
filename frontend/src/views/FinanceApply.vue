<template>
  <div class="apply-container">
    <div class="page-header">
      <h2>融资申请</h2>
      <p>填写申请信息，快速获取资金支持</p>
    </div>

    <!-- 产品信息卡片 -->
    <el-card v-if="product" class="product-info-card">
      <div class="product-header">
        <h3>{{ product.name }}</h3>
        <el-tag type="success" size="large">{{ product.rate }}%</el-tag>
      </div>
      <div class="product-details">
        <div class="detail-item">
          <span class="label">贷款期限</span>
          <span class="value">{{ product.term }}个月</span>
        </div>
        <div class="detail-item">
          <span class="label">额度范围</span>
          <span class="value">{{ product.minLimit }}-{{ product.maxLimit }}万元</span>
        </div>
      </div>
      <p class="product-description">{{ product.description }}</p>
    </el-card>

    <!-- 申请表单 -->
    <el-card class="form-card">
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="140px"
        label-position="right"
      >
        <el-form-item label="申请产品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择融资产品" style="width: 100%">
            <el-option
              v-for="item in productOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="申请金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="product?.minLimit || 1"
            :max="product?.maxLimit || 1000"
            :precision="2"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
          <span class="unit-tip">万元</span>
        </el-form-item>

        <el-form-item label="融资用途" prop="application">
          <el-input
            v-model="form.application"
            type="textarea"
            :rows="3"
            placeholder="请详细描述融资用途，如：购买种子、农药、农机具等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="农作物类型" prop="item">
          <el-input v-model="form.item" placeholder="如：水稻、小麦、玉米等" />
        </el-form-item>

        <el-form-item label="种植面积" prop="area">
          <el-input-number
            v-model="form.area"
            :min="0"
            :precision="2"
            controls-position="right"
            style="width: 100%"
          />
          <span class="unit-tip">亩</span>
        </el-form-item>

        <el-form-item label="还款期限" prop="repaymentPeriod">
          <el-input-number
            v-model="form.repaymentPeriod"
            :min="1"
            :max="product?.term || 60"
            controls-position="right"
            style="width: 100%"
          />
          <span class="unit-tip">个月</span>
        </el-form-item>

        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>

        <el-form-item label="备注说明" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="其他需要说明的内容（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="submit">
            提交申请
          </el-button>
          <el-button size="large" @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 温馨提示 -->
    <el-card class="tips-card">
      <template #header>
        <div class="tips-header">
          <el-icon><Warning /></el-icon>
          <span>温馨提示</span>
        </div>
      </template>
      <ul class="tips-list">
        <li>请确保填写的信息真实有效</li>
        <li>申请提交后，银行将在1-3个工作日内审核</li>
        <li>审核通过后，资金将在1-2个工作日内到账</li>
        <li>如有疑问，请联系银行客服</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import { getFinanceProductDetail, getFinanceProducts, applyFinance } from '@/api/finance'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const submitting = ref(false)

const product = ref(null)
const allProducts = ref([])

const form = reactive({
  productId: null,
  amount: 0,
  application: '',
  item: '',
  area: 0,
  repaymentPeriod: 12,
  phone: '',
  remark: ''
})

const rules = {
  productId: [{ required: true, message: '请选择融资产品', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入申请金额', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value < (product.value?.minLimit || 1)) {
          callback(new Error(`申请金额不能低于${product.value?.minLimit || 1}万元`))
        } else if (value > (product.value?.maxLimit || 1000)) {
          callback(new Error(`申请金额不能高于${product.value?.maxLimit || 1000}万元`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  application: [
    { required: true, message: '请输入融资用途', trigger: 'blur' },
    { min: 10, message: '融资用途至少10个字符', trigger: 'blur' }
  ],
  item: [{ required: true, message: '请输入农作物类型', trigger: 'blur' }],
  area: [
    { required: true, message: '请输入种植面积', trigger: 'blur' },
    { type: 'number', min: 0.1, message: '种植面积必须大于0', trigger: 'blur' }
  ],
  repaymentPeriod: [
    { required: true, message: '请输入还款期限', trigger: 'blur' },
    { type: 'number', min: 1, message: '还款期限至少1个月', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

const productOptions = computed(() => {
  return allProducts.value
    .filter(p => p.status === 1)
    .map(p => ({
      value: p.productId,
      label: `${p.name} (${p.rate}% / ${p.term}个月)`
    }))
})

onMounted(async () => {
  // 加载所有产品
  await loadAllProducts()

  const productId = route.query.productId
  if (productId) {
    await loadProduct(productId)
  } else {
    // 默认选择第一个产品
    if (allProducts.value.length > 0) {
      const firstProduct = allProducts.value.find(p => p.status === 1) || allProducts.value[0]
      await loadProduct(firstProduct.productId)
    }
  }
})

const loadAllProducts = async () => {
  try {
    const res = await getFinanceProducts({ page: 1, pageSize: 100 })
    allProducts.value = res.data.records || []
  } catch (error) {
    console.error('加载产品列表失败', error)
  }
}

const loadProduct = async (id) => {
  try {
    const res = await getFinanceProductDetail(id)
    product.value = res.data
    form.productId = Number(id)
    form.amount = res.data.minLimit
    form.repaymentPeriod = Math.min(12, res.data.term)
  } catch (error) {
    ElMessage.error('加载产品信息失败')
  }
}

const submit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    const submitData = {
      productId: form.productId,
      amount: Number(form.amount),
      application: form.application,
      item: form.item,
      area: Number(form.area),
      repaymentPeriod: Number(form.repaymentPeriod),
      phone: form.phone,
      remark: form.remark
    }

    await applyFinance(submitData)
    ElMessage.success('申请提交成功，请等待银行审核')
    router.push('/farmer/my-finance')
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '提交失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

// 监听产品变化
const onProductChange = async (productId) => {
  await loadProduct(productId)
}
</script>

<style scoped>
.apply-container {
  max-width: 700px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1f2923;
  margin: 0 0 8px;
}

.page-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.product-info-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #f7f5f0 0%, #ffffff 100%);
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.product-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #2d5a3d;
}

.product-details {
  display: flex;
  gap: 30px;
  margin-bottom: 10px;
}

.detail-item {
  display: flex;
  flex-direction: column;
}

.detail-item .label {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 4px;
}

.detail-item .value {
  font-size: 16px;
  font-weight: 600;
  color: #1f2923;
}

.product-description {
  color: #6b7280;
  font-size: 14px;
  margin: 10px 0 0 0;
  line-height: 1.6;
}

.form-card {
  margin-bottom: 20px;
}

.unit-tip {
  margin-left: 8px;
  color: #6b7280;
  font-size: 14px;
}

.tips-card {
  background: #fffaf0;
  border: 1px solid #ffe4b5;
}

.tips-header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #d48806;
  font-weight: 600;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.8;
}

.tips-list li {
  margin-bottom: 4px;
}

@media (max-width: 640px) {
  .apply-container {
    padding: 0 16px;
  }

  :deep(.el-form-item__label) {
    width: 100px !important;
  }

  .product-details {
    flex-direction: column;
    gap: 15px;
  }
}
</style>
