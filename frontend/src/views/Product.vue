<template>
  <div class="product-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <h1 class="page-title">商品中心</h1>
      <p class="page-subtitle">优质农产品，直接从农户到您手中</p>
    </div>

    <!-- 搜索筛选区 -->
    <div class="filter-section">
      <div class="filter-tabs">
        <button
          v-for="cat in categories"
          :key="cat.value"
          class="filter-tab"
          :class="{ active: searchForm.category === cat.value }"
          @click="filterByCategory(cat.value)"
        >
          {{ cat.label }}
        </button>
      </div>
      <div class="filter-actions">
        <el-input
          v-model="searchForm.name"
          placeholder="搜索商品名称..."
          prefix-icon="Search"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 价格筛选区 -->
    <div class="price-filter">
      <span class="price-label">价格区间：</span>
      <el-input-number
        v-model="searchForm.minPrice"
        :min="0"
        :precision="2"
        placeholder="最低价"
        class="price-input"
      />
      <span class="price-separator">-</span>
      <el-input-number
        v-model="searchForm.maxPrice"
        :min="0"
        :precision="2"
        placeholder="最高价"
        class="price-input"
      />
      <el-button type="primary" @click="handleSearch" size="small">筛选</el-button>
    </div>

    <!-- 农户发布按钮 -->
    <div v-if="userStore.userInfo?.role === 'farmer'" class="publish-section">
      <el-button type="success" :icon="Plus" @click="showPublishDialog">
        发布商品
      </el-button>
    </div>

    <!-- 商品网格 -->
    <div v-loading="loading" class="products-grid">
      <div
        v-for="product in products"
        :key="product.productId"
        class="product-card"
        @click="viewDetail(product.productId)"
      >
        <div class="product-image">
          <img
            :src="product.picPath || '/placeholder.jpg'"
            :alt="product.name"
            @error="handleImageError"
          />
          <div v-if="product.stock <= 0" class="out-of-stock">缺货</div>
        </div>
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-desc">{{ product.description }}</p>
          <div class="product-footer">
            <span class="product-price">¥{{ product.price }}</span>
            <span class="product-stock">库存: {{ product.stock }}</span>
          </div>
          <el-button
            type="primary"
            size="small"
            :disabled="product.stock <= 0"
            @click.stop="addToCart(product)"
          >
            加入购物车
          </el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="products.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7,10 12,15 17,10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
        </div>
        <p>暂无商品</p>
      </div>
    </div>

    <!-- 分页 -->
    <div v-if="pagination.total > 0" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadProducts"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 发布商品对话框 -->
    <el-dialog v-model="publishDialogVisible" title="发布商品" width="500px">
      <el-form :model="publishForm" :rules="publishRules" ref="publishFormRef" label-width="80px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="publishForm.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="publishForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
          />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="publishForm.price" :min="0" :precision="2" :step="0.1" />
          <span class="unit-suffix">元/kg</span>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="publishForm.stock" :min="0" :step="1" />
          <span class="unit-suffix">件</span>
        </el-form-item>
        <el-form-item label="图片" prop="picPath">
          <el-input v-model="publishForm.picPath" placeholder="请输入图片URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getProducts, publishProduct } from '@/api/product'
import { addToCart as addCartItem } from '@/api/cart'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const publishing = ref(false)
const products = ref([])
const publishDialogVisible = ref(false)
const publishFormRef = ref(null)

const categories = [
  { label: '全部', value: '' },
  { label: '谷物', value: 'grain' },
  { label: '蔬菜', value: 'vegetable' },
  { label: '水果', value: 'fruit' },
  { label: '畜禽', value: 'livestock' },
  { label: '水产', value: 'aquatic' }
]

const searchForm = reactive({
  name: '',
  category: '',
  minPrice: null,
  maxPrice: null
})

const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0
})

const publishForm = reactive({
  name: '',
  description: '',
  price: 0,
  stock: 0,
  picPath: ''
})

const publishRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

onMounted(() => {
  loadProducts()
})

const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined,
      category: searchForm.category || undefined,
      minPrice: searchForm.minPrice || undefined,
      maxPrice: searchForm.maxPrice || undefined
    }
    const res = await getProducts(params)
    products.value = res.data?.records || res.data || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    console.error('加载商品列表失败:', error)
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadProducts()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.category = ''
  searchForm.minPrice = null
  searchForm.maxPrice = null
  handleSearch()
}

const filterByCategory = (category) => {
  searchForm.category = category
  handleSearch()
}

const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadProducts()
}

const viewDetail = (id) => {
  router.push(`/product/${id}`)
}

const addToCart = async (product) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    await addCartItem({ productId: product.productId, quantity: 1 })
    ElMessage.success('已加入购物车')
  } catch (error) {
    console.error('加入购物车失败:', error)
    ElMessage.error(error.message || '加入购物车失败')
  }
}

const showPublishDialog = () => {
  publishDialogVisible.value = true
}

const handlePublish = async () => {
  if (!publishFormRef.value) return

  await publishFormRef.value.validate(async (valid) => {
    if (!valid) return

    publishing.value = true
    try {
      await publishProduct(publishForm)
      ElMessage.success('发布成功')
      publishDialogVisible.value = false
      // 重置表单
      Object.assign(publishForm, {
        name: '',
        description: '',
        price: 0,
        stock: 0,
        picPath: ''
      })
      publishFormRef.value.resetFields()
      loadProducts()
    } catch (error) {
      console.error('发布失败:', error)
      ElMessage.error(error.message || '发布失败')
    } finally {
      publishing.value = false
    }
  })
}

const handleImageError = (e) => {
  e.target.src = '/placeholder.jpg'
}
</script>

<style scoped>
.product-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--spacing-8, 32px) var(--spacing-6, 24px);
}

.page-header {
  text-align: center;
  margin-bottom: var(--spacing-8, 32px);
}

.page-title {
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: var(--font-size-4xl, 48px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.page-subtitle {
  font-size: var(--font-size-lg, 20px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

/* 筛选区 */
.filter-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-4, 16px);
  gap: var(--spacing-4, 16px);
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: var(--spacing-2, 8px);
  flex-wrap: wrap;
}

.filter-tab {
  padding: var(--spacing-2, 8px) var(--spacing-4, 16px);
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-secondary, #4a5249);
  background: transparent;
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.filter-tab:hover {
  color: var(--color-primary, #2d5a3d);
  border-color: var(--color-primary, #2d5a3d);
}

.filter-tab.active {
  color: var(--color-text-inverse, #ffffff);
  background: var(--color-primary, #2d5a3d);
  border-color: var(--color-primary, #2d5a3d);
}

.filter-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
}

.search-input {
  width: 240px;
}

/* 价格筛选 */
.price-filter {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
  margin-bottom: var(--spacing-4, 16px);
}

.price-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.price-input {
  width: 120px;
}

.price-separator {
  color: var(--color-text-tertiary, #6b7280);
}

.unit-suffix {
  margin-left: var(--spacing-2, 8px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
}

/* 发布按钮 */
.publish-section {
  margin-bottom: var(--spacing-4, 16px);
  display: flex;
  justify-content: flex-end;
}

/* 商品网格 */
.products-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-5, 20px);
  margin-bottom: var(--spacing-6, 24px);
  min-height: 400px;
}

.product-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  overflow: hidden;
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.product-card:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(31, 41, 35, 0.08));
  transform: translateY(-4px);
}

.product-image {
  position: relative;
  aspect-ratio: 1;
  background: var(--color-bg-secondary, #f7f5f0);
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.out-of-stock {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-bold, 700);
}

.product-info {
  padding: var(--spacing-4, 16px);
}

.product-name {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0 0 var(--spacing-3, 12px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-3, 12px);
}

.product-price {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-error, #b85c38);
}

.product-stock {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-10, 40px);
  color: var(--color-text-tertiary, #6b7280);
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin-bottom: var(--spacing-4, 16px);
  opacity: 0.5;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-state p {
  font-size: var(--font-size-base, 16px);
  margin: 0;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--spacing-4, 16px) 0;
}

/* 响应式 */
@media (max-width: 1024px) {
  .products-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .search-input {
    width: 200px;
  }
}

@media (max-width: 768px) {
  .products-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .search-input {
    width: 100%;
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    flex-wrap: wrap;
  }

  .price-filter {
    flex-wrap: wrap;
  }

  .page-title {
    font-size: var(--font-size-3xl, 38px);
  }
}

@media (max-width: 480px) {
  .products-grid {
    grid-template-columns: 1fr;
  }

  .product-page {
    padding: var(--spacing-6, 24px) var(--spacing-4, 16px);
  }
}
</style>
