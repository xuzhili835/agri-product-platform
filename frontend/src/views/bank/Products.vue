<template>
  <div class="bank-products">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">融资产品</h1>
        <p class="page-subtitle">发布与管理本行提供的融资产品，供农户浏览与申请</p>
      </div>
      <el-button type="primary" @click="openPublish">
        <el-icon><Plus /></el-icon>
        发布产品
      </el-button>
    </div>

    <!-- 产品列表 -->
    <div v-loading="loading" class="content-card">
      <div class="card-header">
        <h3 class="card-title">产品列表（共 {{ pagination.total }} 个）</h3>
      </div>

      <div class="products-body">
        <div v-if="products.length > 0" class="products-grid">
          <div v-for="product in products" :key="product.productId" class="product-card">
            <div class="product-top">
              <div class="product-main">
                <div class="product-name">{{ product.productName || '未命名产品' }}</div>
                <div class="product-bank">{{ product.bankName }}</div>
              </div>
              <div class="product-top-tags">
                <el-tag :type="product.status === 1 ? 'info' : 'success'" effect="plain" size="small">
                  {{ product.status === 1 ? '已暂停' : '在售' }}
                </el-tag>
                <el-tag type="warning" effect="plain" size="small">年利率 {{ product.rate }}%</el-tag>
              </div>
            </div>
            <div class="product-introduce">{{ product.introduce }}</div>
            <div class="product-terms">
              <div class="term">
                <span class="label">可贷额度</span>
                <span class="value">¥ {{ formatNumber(product.money) }}</span>
              </div>
              <div class="term">
                <span class="label">还款期限</span>
                <span class="value">{{ product.repayment }} 个月</span>
              </div>
              <div class="term">
                <span class="label">联系电话</span>
                <span class="value">{{ product.bankPhone }}</span>
              </div>
            </div>
            <div class="product-actions">
              <el-button type="primary" link size="small" @click="openEdit(product)">编辑</el-button>
              <el-button
                v-if="product.status !== 1"
                type="warning"
                link
                size="small"
                @click="toggleStatus(product, 1)"
              >暂停供应</el-button>
              <el-button
                v-else
                type="success"
                link
                size="small"
                @click="toggleStatus(product, 0)"
              >恢复供应</el-button>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无融资产品，点击右上角「发布产品」创建">
          <el-button type="primary" @click="openPublish">立即发布</el-button>
        </el-empty>

        <div v-if="pagination.total > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[9, 18, 36]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadProducts"
            @size-change="loadProducts"
          />
        </div>
      </div>
    </div>

    <!-- 发布 / 编辑 对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑融资产品' : '发布融资产品'" width="560px" @close="resetForm">
      <el-alert
        v-if="editingId"
        type="warning"
        :closable="false"
        show-icon
        title="核心条款（额度 / 利率 / 期限）发布后不可修改；产品名称、介绍与联系电话可随时调整。如需调整核心条款，请暂停后重新发布。"
        style="margin-bottom: 16px"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="form.productName" placeholder="如：助农贷 / 春耕贷" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="银行名称">
          <el-input :model-value="form.bankName" disabled>
            <template #append>固定为注册银行</template>
          </el-input>
          <div class="form-tip">银行名称取自注册信息（不可冒名修改），产品名由本行自行命名。</div>
        </el-form-item>
        <el-form-item label="产品介绍" prop="introduce">
          <el-input
            v-model="form.introduce"
            type="textarea"
            :rows="3"
            placeholder="简要介绍产品特色、适用对象等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="bankPhone">
          <el-input v-model="form.bankPhone" placeholder="客服电话，如 95599" maxlength="20" />
        </el-form-item>
        <el-form-item label="可贷额度" prop="money">
          <el-input-number v-model="form.money" :min="1" :step="10000" :precision="2" style="width: 100%" :disabled="!!editingId" />
          <span class="unit-text">元</span>
        </el-form-item>
        <el-form-item label="年利率" prop="rate">
          <el-input-number v-model="form.rate" :min="0" :max="36" :step="0.1" :precision="2" style="width: 100%" :disabled="!!editingId" />
          <span class="unit-text">%</span>
        </el-form-item>
        <el-form-item label="还款期限" prop="repayment">
          <el-input-number v-model="form.repayment" :min="1" :max="120" :step="1" style="width: 100%" :disabled="!!editingId" />
          <span class="unit-text">个月</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ editingId ? '保存修改' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getBankProducts, publishFinanceProduct, updateBankProduct, setBankProductStatus } from '@/api/finance'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
// 银行名称固定取自注册信息（realName），不可在发布时手填，杜绝冒名
const bankName = userStore.userInfo?.realName || ''

const loading = ref(false)
const submitting = ref(false)
const products = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const pagination = reactive({
  page: 1,
  pageSize: 9,
  total: 0
})

// 字段与后端 FinanceProductRequest / tb_finance_product 对齐
const form = reactive({
  productName: '',
  bankName,
  introduce: '',
  bankPhone: '',
  money: 100000,
  rate: 3.5,
  repayment: 12
})

const rules = {
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  introduce: [{ required: true, message: '请输入产品介绍', trigger: 'blur' }],
  bankPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  money: [{ required: true, type: 'number', min: 1, message: '可贷额度须大于 0', trigger: 'blur' }],
  rate: [{ required: true, type: 'number', min: 0, message: '请输入有效的年利率', trigger: 'blur' }],
  repayment: [{ required: true, type: 'number', min: 1, message: '还款期限须大于 0', trigger: 'blur' }]
}

onMounted(() => {
  loadProducts()
})

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await getBankProducts({ page: pagination.page, pageSize: pagination.pageSize })
    products.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error?.message || '加载融资产品失败')
  } finally {
    loading.value = false
  }
}

const openPublish = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (product) => {
  editingId.value = product.productId
  Object.assign(form, {
    productName: product.productName || '',
    bankName: product.bankName || bankName,
    introduce: product.introduce || '',
    bankPhone: product.bankPhone || '',
    money: Number(product.money) || 0,
    rate: Number(product.rate) || 0,
    repayment: Number(product.repayment) || 0
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    productName: '',
    bankName,
    introduce: '',
    bankPhone: '',
    money: 100000,
    rate: 3.5,
    repayment: 12
  })
  formRef.value?.clearValidate?.()
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (editingId.value) {
        // 编辑模式：核心条款(额度/利率/期限)已锁定，仅提交可修改的产品名称、介绍与联系电话
        await updateBankProduct(editingId.value, {
          productName: form.productName,
          introduce: form.introduce,
          bankPhone: form.bankPhone
        })
        ElMessage.success('产品已更新')
      } else {
        const payload = {
          productName: form.productName,
          bankName: form.bankName,
          introduce: form.introduce,
          bankPhone: form.bankPhone,
          money: form.money,
          rate: form.rate,
          repayment: form.repayment
        }
        await publishFinanceProduct(payload)
        ElMessage.success('产品发布成功')
      }
      dialogVisible.value = false
      await loadProducts()
    } catch (error) {
      ElMessage.error(error?.message || '操作失败，请重试')
    } finally {
      submitting.value = false
    }
  })
}

const toggleStatus = async (product, status) => {
  const action = status === 1 ? '暂停供应' : '恢复供应'
  try {
    await ElMessageBox.confirm(
      `确认${action}产品「${product.productName || product.bankName}」？` + (status === 1
        ? '暂停后农户将无法再申请该产品，但已有申请仍保留在审批列表中。'
        : '恢复后农户可重新申请该产品。'),
      `${action}确认`,
      { type: 'warning', confirmButtonText: `确认${action}`, cancelButtonText: '取消' }
    )
    await setBankProductStatus(product.productId, status)
    ElMessage.success(`已${action}`)
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '操作失败')
    }
  }
}

const formatNumber = (num) => {
  if (num === null || num === undefined || num === '') return '0'
  return Number(num).toLocaleString()
}
</script>

<style scoped>
.bank-products {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-4, 16px);
  margin-bottom: var(--spacing-1, 4px);
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

/* 内容卡片 */
.content-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(31, 41, 35, 0.06));
  overflow: hidden;
}
.card-header {
  padding: var(--spacing-4, 16px) var(--spacing-5, 20px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}
.card-title {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0;
}
.products-body {
  padding: var(--spacing-5, 20px);
}

/* 产品卡片网格 */
.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-4, 16px);
}
.product-card {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-5, 20px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  transition: all var(--transition-fast, 150ms ease);
}
.product-card:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(31, 41, 35, 0.08));
  transform: translateY(-2px);
}
.product-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-2, 8px);
}
.product-top-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.product-main {
  min-width: 0;
}
.product-name {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-bank {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-bank, #2d5a3d);
  margin-top: 2px;
}
.product-introduce {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
  line-height: var(--line-height-normal, 1.5);
  min-height: 42px;
}
.product-terms {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2, 8px);
  padding-top: var(--spacing-2, 8px);
  border-top: 1px dashed var(--color-divider, #edebe6);
}
.term {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-sm, 14px);
}
.term .label { color: var(--color-text-tertiary, #6b7280); }
.term .value { font-weight: var(--font-weight-medium, 500); color: var(--color-text-primary, #1f2923); }
.product-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-2, 8px);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--spacing-5, 20px);
}

.unit-text {
  margin-left: var(--spacing-2, 8px);
  color: var(--color-text-tertiary, #6b7280);
  font-size: var(--font-size-sm, 14px);
}

.form-tip {
  margin-top: 4px;
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  line-height: 1.5;
}

@media (max-width: 640px) {
  .page-header { flex-direction: column; }
}
</style>
