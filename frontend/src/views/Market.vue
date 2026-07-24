<template>
  <div class="market-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <el-button v-if="showBackButton" :icon="ArrowLeft" @click="goBack" class="back-button">返回</el-button>
      <div class="header-content">
        <h1 class="page-title">农产品市场</h1>
        <p class="page-subtitle">优质农产品，直接从农户到您手中</p>
      </div>
    </div>

    <!-- 筛选区 -->
    <div class="filter-section">
      <div class="filter-tabs">
        <button
          v-for="tab in filterTabs"
          :key="tab.value"
          class="filter-tab"
          :class="{ active: filterType === tab.value }"
          @click="handleFilterChange(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="filter-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索农产品..."
          prefix-icon="Search"
          size="large"
          class="search-input"
          @keyup.enter="handleSearch"
          clearable
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <!-- 买家发布需求按钮 -->
      <el-button
        v-if="userStore.isLoggedIn() && userStore.role === 'buyer'"
        type="warning"
        @click="showPublishDialog"
      >
        <el-icon><Plus /></el-icon>
        发布求购需求
      </el-button>
    </div>

    <!-- 商品网格 -->
    <div v-loading="loading" class="products-grid">
      <div
        v-for="product in products"
        :key="product.productId"
        class="product-card"
        @click="handleProductClick(product)"
      >
        <div class="product-image">
          <img
            v-if="product.picPath"
            :src="product.picPath"
            :alt="product.title"
            @error="handleImageError"
          />
          <div v-else class="image-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7,10 12,15 17,10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
          </div>
          <div class="product-tag" :class="product.type === 'goods' ? 'supply' : 'demand'">
            {{ product.type === 'goods' ? '供' : '求' }}
          </div>
          <div v-if="product.orderStatus === 2" class="product-status sold">已完成</div>
        </div>
        <div class="product-info">
          <div class="product-name">{{ product.title }}</div>
          <div class="product-desc">{{ product.content }}</div>
          <div class="product-footer">
            <div class="product-price">
              {{ product.type === 'goods' ? `¥${product.price}` : '面议' }}
            </div>
            <div class="product-seller">
              {{ product.ownName || '未知卖家' }}
              <span v-if="product.ownPhone" class="seller-phone">· {{ product.ownPhone }}</span>
            </div>
          </div>
          <div class="product-time">{{ formatTime(product.createTime) }}</div>
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
        <p>{{ filterType === 'demand' ? '暂无求购信息' : '暂无商品' }}</p>
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

    <!-- 发布求购需求对话框 -->
    <el-dialog v-model="publishDialogVisible" title="发布求购需求" width="600px">
      <el-form :model="publishForm" :rules="publishRules" ref="publishFormRef" label-width="100px">
        <el-form-item label="需求标题" prop="title">
          <el-input v-model="publishForm.title" placeholder="请输入需求标题，如：求购优质大米" />
        </el-form-item>
        <el-form-item label="需求描述" prop="content">
          <el-input
            v-model="publishForm.content"
            type="textarea"
            :rows="4"
            placeholder="请详细描述您的需求，包括数量、质量要求、交货时间等"
          />
        </el-form-item>
        <el-form-item label="预算价格">
          <el-input-number v-model="publishForm.price" :min="0" :precision="2" :step="0.1" />
          <span class="unit-suffix">元/kg（可选）</span>
        </el-form-item>
        <el-form-item label="商品图片">
          <div class="product-image-upload">
            <div class="image-preview">
              <img v-if="publishForm.picPath" :src="publishForm.picPath" alt="预览" />
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
                v-model="publishForm.picPath"
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
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import { getProducts, publishProduct } from '@/api/product'
import { LOCAL_PRODUCT_IMAGES } from '@/config/localImages'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 判断是否显示返回按钮（仅在公开市场页面显示，不在买家市场页面显示）
const showBackButton = computed(() => {
  return route.path === '/market'
})

// 本地图片列表
const localImages = LOCAL_PRODUCT_IMAGES

// 筛选标签
const filterTabs = [
  { label: '全部', value: 'all' },
  { label: '货源', value: 'goods' },
  { label: '需求', value: 'demand' }
]

const filterType = ref('all')
const searchKeyword = ref('')
const loading = ref(false)
const publishing = ref(false)
const products = ref([])

// 分页
const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0
})

// 发布对话框
const publishDialogVisible = ref(false)
const publishFormRef = ref(null)

const publishForm = reactive({
  title: '',
  content: '',
  price: 0,
  picPath: '',
  type: 'demand' // 买家发布的是需求
})

const publishRules = {
  title: [{ required: true, message: '请输入需求标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入需求描述', trigger: 'blur' }]
}

// 加载商品列表
const loadProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value || undefined
    }

    // 根据筛选类型添加参数
    if (filterType.value !== 'all') {
      params.type = filterType.value
    }

    const res = await getProducts(params)
    if (res.code === 200) {
      const data = res.data
      products.value = data.records || []
      pagination.total = data.total || 0
    }
  } catch (error) {
    console.error('加载商品列表失败:', error)
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

// 筛选变更
const handleFilterChange = (value) => {
  filterType.value = value
  pagination.page = 1
  loadProducts()
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadProducts()
}

// 分页大小变更
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.page = 1
  loadProducts()
}

// 处理产品点击
const handleProductClick = (product) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录后查看产品详情')
    router.push('/login')
    return
  }

  // 跳转到商品详情页
  if (product.productId) {
    router.push(`/product/${product.productId}`)
  }
}

// 显示发布对话框
const showPublishDialog = () => {
  publishDialogVisible.value = true
}

// 处理发布
const handlePublish = async () => {
  if (!publishFormRef.value) return

  await publishFormRef.value.validate(async (valid) => {
    if (!valid) return

    publishing.value = true
    try {
      // 使用现有的发布商品API，类型设置为demand
      await publishProduct(publishForm)
      ElMessage.success('发布成功')
      publishDialogVisible.value = false

      // 重置表单
      Object.assign(publishForm, {
        title: '',
        content: '',
        price: 0,
        picPath: '',
        type: 'demand'
      })
      publishFormRef.value.resetFields()

      // 刷新列表
      if (filterType.value === 'demand' || filterType.value === 'all') {
        loadProducts()
      }
    } catch (error) {
      console.error('发布失败:', error)
      ElMessage.error(error.message || '发布失败')
    } finally {
      publishing.value = false
    }
  })
}

// 处理图片加载错误
const handleImageError = (e) => {
  e.target.style.display = 'none'
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
    publishForm.picPath = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString()
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.market-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: var(--spacing-8, 32px) var(--spacing-6, 24px);
}

.page-header {
  display: flex;
  align-items: center;
  margin-bottom: var(--spacing-8, 32px);
}

.back-button {
  flex-shrink: 0;
  margin-right: var(--spacing-4, 16px);
}

.header-content {
  flex: 1;
  text-align: center;
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
  margin-bottom: var(--spacing-6, 24px);
  gap: var(--spacing-4, 16px);
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  gap: var(--spacing-2, 8px);
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

.filter-search {
  display: flex;
  gap: var(--spacing-2, 8px);
}

.search-input {
  width: 300px;
}

/* 商品网格 */
.products-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-5, 20px);
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

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary, #6b7280);
}

.image-placeholder svg {
  width: 48px;
  height: 48px;
}

.product-tag {
  position: absolute;
  top: var(--spacing-3, 12px);
  left: var(--spacing-3, 12px);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-inverse, #ffffff);
  border-radius: var(--radius-base, 8px);
}

.product-tag.supply {
  background: var(--color-success, #4a7c59);
}

.product-tag.demand {
  background: var(--color-warning, #c9a661);
}

.product-status {
  position: absolute;
  top: var(--spacing-3, 12px);
  right: var(--spacing-3, 12px);
  padding: var(--spacing-1, 4px) var(--spacing-2, 8px);
  background: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  font-size: var(--font-size-xs, 12px);
  border-radius: var(--radius-base, 8px);
}

.product-info {
  padding: var(--spacing-4, 16px);
}

.product-name {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin-bottom: var(--spacing-1, 4px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin-bottom: var(--spacing-3, 12px);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  min-height: 40px;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-2, 8px);
}

.product-price {
  font-size: var(--font-size-lg, 18px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-error, #b85c38);
}

.product-seller {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

.product-time {
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

/* 单位后缀 */
.unit-suffix {
  margin-left: var(--spacing-2, 8px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
}

/* 图片选择器选项样式 */
.image-option {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
}

.image-option img {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border-radius: var(--radius-sm, 4px);
}

/* 商品图片上传区域 */
.product-image-upload {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3, 12px);
}

.image-preview {
  width: 120px;
  height: 120px;
  border: 2px dashed var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary, #f7f5f0);
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  color: var(--color-text-tertiary, #6b7280);
  font-size: var(--font-size-sm, 14px);
}

.image-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
}

.divider {
  color: var(--color-text-tertiary, #6b7280);
  font-size: var(--font-size-sm, 14px);
}

/* 响应式 */
@media (max-width: 1024px) {
  .products-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .search-input {
    width: 240px;
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

  .filter-search {
    flex-direction: column;
  }

  .page-title {
    font-size: var(--font-size-3xl, 38px);
  }

  .page-header {
    flex-direction: column;
    gap: var(--spacing-2, 8px);
  }

  .header-content {
    text-align: center;
  }
}

@media (max-width: 480px) {
  .products-grid {
    grid-template-columns: 1fr;
  }

  .market-page {
    padding: var(--spacing-6, 24px) var(--spacing-4, 16px);
  }
}
</style>
