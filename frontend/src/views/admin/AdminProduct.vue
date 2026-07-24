<template>
  <div class="admin-product">
    <div class="header">
      <h2>商品管理</h2>
      <el-input
        v-model="searchKeyword"
        placeholder="按标题搜索商品"
        style="width: 250px"
        clearable
        @clear="loadProducts"
        @keyup.enter="loadProducts"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="loadProducts">搜索</el-button>
        </template>
      </el-input>
    </div>

    <el-table :data="products" style="width: 100%" v-loading="loading" border>
      <el-table-column label="图片" width="90">
        <template #default="{ row }">
          <img v-if="row.picPath" :src="row.picPath" class="product-preview" @error="handleImageError" />
          <span v-else class="no-img">无图</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'demand' ? 'warning' : 'success'" size="small">
            {{ typeText(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="110">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="ownName" label="发布人" width="120" />
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="editProduct(row)">编辑</el-button>
          <el-button link type="danger" @click="deleteProduct(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="编辑商品" width="600px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="输入商品标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="输入商品描述" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="货源（出售）" value="goods" />
            <el-option label="需求（求购）" value="demand" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品图片">
          <div class="product-image-upload">
            <div class="image-preview">
              <img v-if="form.picPath" :src="form.picPath" alt="预览" />
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
                v-model="form.picPath"
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
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAdminProducts, updateAdminProduct, deleteProduct as deleteProductApi } from '@/api/admin'
import { LOCAL_PRODUCT_IMAGES } from '@/config/localImages'

const loading = ref(false)
const saving = ref(false)
const products = ref([])
const searchKeyword = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)

// 本地图片列表
const localImages = LOCAL_PRODUCT_IMAGES

const form = reactive({
  title: '',
  content: '',
  price: 0,
  picPath: '',
  type: 'goods'
})

const typeText = (t) => ({ goods: '货源', demand: '需求' }[t] || t || '—')

onMounted(() => {
  loadProducts()
})

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await getAdminProducts({ keyword: searchKeyword.value })
    products.value = res.data || []
  } catch (error) {
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

const editProduct = (product) => {
  console.log('编辑商品数据:', product)
  console.log('productId:', product.productId)
  console.log('orderId:', product.orderId)
  const id = product.productId || product.orderId
  if (!id) {
    ElMessage.error('商品ID缺失，无法编辑')
    console.error('商品ID缺失 - product对象:', product)
    return
  }
  editingId.value = id
  Object.assign(form, {
    title: product.title || '',
    content: product.content || '',
    price: product.price ?? 0,
    picPath: product.picPath || '',
    type: product.type || 'goods'
  })
  dialogVisible.value = true
}

const save = async () => {
  if (!form.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!editingId.value) {
    ElMessage.error('商品ID缺失，无法保存')
    return
  }
  saving.value = true
  try {
    // 过滤掉 undefined 和 null 值，避免发送 "undefined" 字符串到后端
    const data = Object.entries(form).reduce((acc, [key, value]) => {
      if (value !== undefined && value !== null) {
        acc[key] = value
      }
      return acc
    }, {})
    await updateAdminProduct(editingId.value, data)
    ElMessage.success('更新成功')
    dialogVisible.value = false
    await loadProducts()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const deleteProduct = async (product) => {
  const id = product.productId || product.orderId
  if (!id) {
    ElMessage.error('商品ID缺失，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除商品「${product.title}」吗？`, '提示', { type: 'warning' })
    await deleteProductApi(id)
    ElMessage.success('删除成功')
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

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
    form.picPath = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}
</script>

<style scoped>
.admin-product {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.product-preview {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.no-img {
  display: inline-block;
  width: 60px;
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  border-radius: 4px;
}

/* 图片选择器选项样式 */
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
  font-size: 14px;
}

.image-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.divider {
  color: #6b7280;
  font-size: 14px;
}
</style>
