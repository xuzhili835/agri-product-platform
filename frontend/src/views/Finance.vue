<template>
  <div class="finance-container">
    <div class="page-header">
      <h2>融资服务</h2>
      <p>为农户提供专业融资解决方案</p>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <el-input
        v-model="filterKeyword"
        placeholder="搜索产品名称"
        style="width: 200px"
        clearable
        @change="loadProducts"
      />
      <el-select v-model="filterTerm" placeholder="贷款期限" clearable style="width: 120px" @change="loadProducts">
        <el-option label="全部" value="" />
        <el-option label="12个月以下" value="short" />
        <el-option label="12-36个月" value="medium" />
        <el-option label="36个月以上" value="long" />
      </el-select>
    </div>

    <!-- 产品列表 -->
    <div v-loading="loading" class="product-list">
      <el-card v-for="product in products" :key="product.productId" class="product-card">
        <div class="product-header">
          <h3>{{ product.name }}</h3>
          <el-tag :type="product.status === 1 ? 'success' : 'info'">
            {{ product.status === 1 ? '上架中' : '已下架' }}
          </el-tag>
        </div>
        <div class="product-info">
          <div class="info-item">
            <span class="label">年化利率</span>
            <span class="value rate">{{ product.rate }}%</span>
          </div>
          <div class="info-item">
            <span class="label">贷款期限</span>
            <span class="value">{{ product.term }}个月</span>
          </div>
          <div class="info-item">
            <span class="label">最高额度</span>
            <span class="value">{{ product.maxLimit }}万元</span>
          </div>
          <div class="info-item">
            <span class="label">最低额度</span>
            <span class="value">{{ product.minLimit }}万元</span>
          </div>
        </div>
        <p class="description">{{ product.description }}</p>
        <div class="product-actions">
          <el-button type="primary" @click="apply(product)" :disabled="product.status !== 1">
            立即申请
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && products.length === 0" description="暂无融资产品" />

    <!-- 分页 -->
    <div v-if="pagination.total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[6, 9, 12, 18]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadProducts"
        @size-change="loadProducts"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { getFinanceProducts } from '@/api/finance'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const filterKeyword = ref('')
const filterTerm = ref('')

const products = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 9,
  total: 0
})

onMounted(() => {
  loadProducts()
})

const loadProducts = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: filterKeyword.value,
      term: filterTerm.value
    }
    const res = await getFinanceProducts(params)
    products.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error('加载融资产品失败')
  } finally {
    loading.value = false
  }
}

const apply = (product) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (userStore.role !== 'farmer') {
    ElMessage.warning('仅农户可申请融资')
    return
  }
  router.push({
    path: '/finance/apply',
    query: { productId: product.productId }
  })
}
</script>

<style scoped>
.finance-container {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h2 {
  font-size: 32px;
  font-weight: 600;
  color: #1f2923;
  margin: 0 0 8px;
}

.page-header p {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
}

.filter-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  justify-content: center;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.product-card {
  text-align: center;
  transition: all 0.3s ease;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(31, 41, 35, 0.12);
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
  color: #1f2923;
}

.product-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  flex-direction: column;
  padding: 10px;
  background: #f7f5f0;
  border-radius: 8px;
}

.label {
  color: #6b7280;
  font-size: 13px;
  margin-bottom: 4px;
}

.value {
  font-size: 18px;
  font-weight: 600;
  color: #2d5a3d;
}

.value.rate {
  color: #b85c38;
}

.description {
  color: #6b7280;
  min-height: 60px;
  margin-bottom: 15px;
  font-size: 14px;
  line-height: 1.6;
}

.product-actions {
  padding-top: 15px;
  border-top: 1px solid #edebe6;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

@media (max-width: 1024px) {
  .product-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .product-list {
    grid-template-columns: 1fr;
  }

  .filter-section {
    flex-direction: column;
  }

  .filter-section .el-input,
  .filter-section .el-select {
    width: 100% !important;
  }
}
</style>
