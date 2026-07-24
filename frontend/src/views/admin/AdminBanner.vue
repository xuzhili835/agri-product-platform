<template>
  <div class="admin-banner">
    <div class="header">
      <h2>轮播图管理</h2>
      <el-button type="primary" @click="showAddDialog">添加轮播图</el-button>
    </div>

    <el-table :data="banners" style="width: 100%" v-loading="loading">
      <el-table-column label="图片" width="200">
        <template #default="{ row }">
          <img :src="row.picPath" class="banner-preview" @error="handleImageError" />
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="100" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="editBanner(row)">编辑</el-button>
          <el-button link type="danger" @click="deleteBanner(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingBanner ? '编辑轮播图' : '添加轮播图'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="图片预览">
          <div class="image-preview-wrapper">
            <img v-if="form.picPath" :src="form.picPath" class="preview-image" alt="预览" />
            <div v-else class="no-image">暂无图片</div>
          </div>
        </el-form-item>
        <el-form-item label="图片来源">
          <div class="image-source-wrapper">
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
              style="width: 200px"
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
        </el-form-item>
        <el-form-item label="或输入URL">
          <el-input v-model="form.picPath" placeholder="输入图片URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
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
import { getBanners, addBanner, updateBanner, deleteBanner as deleteBan } from '@/api/admin'
import { LOCAL_PRODUCT_IMAGES } from '@/config/localImages'

const localImages = LOCAL_PRODUCT_IMAGES

const loading = ref(false)
const saving = ref(false)
const banners = ref([])
const dialogVisible = ref(false)
const editingBanner = ref(null)

const form = reactive({
  picPath: '',
  sortOrder: 0
})

onMounted(() => {
  loadBanners()
})

const loadBanners = async () => {
  loading.value = true
  try {
    const res = await getBanners()
    console.log('轮播图数据:', res)
    console.log('轮播图详细数据:', JSON.stringify(res.data, null, 2))
    banners.value = res.data || []
    if (banners.value.length === 0) {
      console.warn('暂无轮播图数据，请先添加')
    } else {
      console.log('第一条轮播图:', banners.value[0])
      console.log('第一条轮播图的 picPath:', banners.value[0].picPath, typeof banners.value[0].picPath)
    }
  } catch (error) {
    console.error('加载轮播图失败:', error)
    ElMessage.error('加载失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  editingBanner.value = null
  form.picPath = ''
  form.sortOrder = 0
  dialogVisible.value = true
}

const editBanner = (banner) => {
  editingBanner.value = banner
  form.picPath = banner.picPath
  form.sortOrder = banner.sortOrder || 0
  dialogVisible.value = true
}

const save = async () => {
  if (!form.picPath) {
    ElMessage.warning('请选择或输入图片URL')
    return
  }

  // 转换为普通对象
  const dataToSave = {
    picPath: String(form.picPath),
    sortOrder: Number(form.sortOrder)
  }

  console.log('保存轮播图:', dataToSave)
  saving.value = true
  try {
    let res
    if (editingBanner.value) {
      console.log('更新轮播图 ID:', editingBanner.value.bannerId)
      res = await updateBanner(editingBanner.value.bannerId, dataToSave)
      ElMessage.success('更新成功')
    } else {
      console.log('添加新轮播图')
      res = await addBanner(dataToSave)
      ElMessage.success('添加成功')
    }
    console.log('保存响应:', res)
    dialogVisible.value = false
    await loadBanners()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + (error.response?.data?.message || error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

const deleteBanner = async (banner) => {
  try {
    await ElMessageBox.confirm('确定删除此轮播图吗？', '提示', { type: 'warning' })
    await deleteBan(banner.bannerId)
    ElMessage.success('删除成功')
    await loadBanners()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleImageError = (e) => {
  e.target.style.display = 'none'
}

// 图片上传前验证
const beforeImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 图片上传成功
const handleImageSuccess = (response) => {
  console.log('上传响应:', response)
  if (response.code === 200) {
    form.picPath = response.data.url || response.data
    console.log('设置图片路径:', form.picPath)
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败: ' + response.message)
  }
}
</script>

<style scoped>
.admin-banner {
  max-width: 1000px;
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

.banner-preview {
  width: 150px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.image-preview-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.preview-image {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 4px;
  border: 1px solid #e5e0d8;
}

.no-image {
  width: 200px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  border-radius: 4px;
}

.image-source-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.divider {
  color: #999;
}

.image-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.image-option img {
  width: 30px;
  height: 30px;
  object-fit: cover;
  border-radius: 4px;
}
</style>
