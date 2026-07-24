<template>
  <div class="knowledge-publish-page">
    <div class="container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2 class="page-title">{{ isEdit ? '编辑知识' : '发布知识' }}</h2>
        <p class="page-subtitle">分享您的农业专业知识</p>
      </div>

      <!-- 发布表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="publish-form"
      >
        <!-- 标题 -->
        <el-form-item label="文章标题" prop="title">
          <el-input
            v-model="formData.title"
            placeholder="请输入文章标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <!-- 分类 -->
        <el-form-item label="文章分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类">
            <el-option
              v-for="cat in categories"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>

        <!-- 封面图片 -->
        <el-form-item label="封面图片">
          <el-upload
            class="image-uploader"
            :show-file-list="false"
            :before-upload="beforeImageUpload"
            :on-success="handleImageSuccess"
            action="/api/upload"
          >
            <img v-if="formData.picPath" :src="formData.picPath" class="upload-image">
            <div v-else class="upload-placeholder">
              <el-icon><Plus /></el-icon>
              <div>上传封面图片</div>
            </div>
          </el-upload>
          <div class="upload-tip">建议尺寸：800x500px，支持jpg、png格式</div>
        </el-form-item>

        <!-- 内容 -->
        <el-form-item label="文章内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="15"
            placeholder="请输入文章内容..."
            maxlength="10000"
            show-word-limit
          />
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button @click="handleBack">取消</el-button>
          <el-button @click="handleSaveDraft" :loading="submitting">保存草稿</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '更新文章' : '发布文章' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getKnowledgeDetail, publishKnowledge, updateKnowledge } from '@/api/knowledge'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const submitting = ref(false)

// 是否是编辑模式
const isEdit = computed(() => !!route.query.id)

// 表单数据
const formData = reactive({
  title: '',
  category: '',
  content: '',
  picPath: '',
  status: 'published'
})

// 分类选项
const categories = [
  { value: 'grain', label: '粮食作物' },
  { value: 'vegetable', label: '蔬菜种植' },
  { value: 'fruit', label: '果树栽培' },
  { value: 'soil', label: '土壤肥料' },
  { value: 'pest', label: '病虫害防治' },
  { value: 'technology', label: '农业技术' },
  { value: 'other', label: '其他' }
]

// 表单验证规则
const formRules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度应在5-100个字符之间', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择文章分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入文章内容', trigger: 'blur' },
    { min: 50, max: 10000, message: '内容长度应在50-10000个字符之间', trigger: 'blur' }
  ]
}

// 图片上传前校验
const beforeImageUpload = (rawFile) => {
  const isImage = ['image/jpeg', 'image/png', 'image/jpg'].includes(rawFile.type)
  const isLt5M = rawFile.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传JPG/PNG格式的图片')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

// 图片上传成功
const handleImageSuccess = (response) => {
  if (response.code === 200) {
    formData.picPath = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

// 返回列表
const handleBack = () => {
  router.back()
}

// 保存草稿
const handleSaveDraft = async () => {
  formData.status = 'draft'
  await submitForm()
}

// 提交表单
const handleSubmit = async () => {
  formData.status = 'published'
  await submitForm()
}

// 提交逻辑
const submitForm = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    if (isEdit.value) {
      await updateKnowledge(route.query.id, formData)
      ElMessage.success('更新成功')
    } else {
      await publishKnowledge(formData)
      ElMessage.success('发布成功')
    }

    router.push('/knowledge')
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 加载编辑数据
const loadKnowledge = async () => {
  try {
    const res = await getKnowledgeDetail(route.query.id)
    const data = res.data
    formData.title = data.title
    formData.category = data.category
    formData.content = data.content
    formData.picPath = data.picPath
    formData.status = data.status || 'published'
  } catch (error) {
    ElMessage.error('加载文章失败')
    router.back()
  }
}

onMounted(() => {
  // 检查权限
  if (!userStore.hasRole(['expert', 'admin'])) {
    ElMessage.error('只有专家和管理员可以发布知识')
    router.push('/knowledge')
    return
  }

  // 编辑模式加载数据
  if (isEdit.value) {
    loadKnowledge()
  }
})
</script>

<style scoped>
.knowledge-publish-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
}

.page-header {
  margin-bottom: var(--spacing-6, 24px);
  text-align: center;
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

.publish-form {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-8, 32px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.publish-form :deep(.el-form-item__label) {
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-primary, #1f2923);
}

.publish-form :deep(.el-input__inner),
.publish-form :deep(.el-textarea__inner) {
  font-size: var(--font-size-base, 16px);
}

/* ===== 图片上传 ===== */
.image-uploader {
  width: 100%;
}

.upload-image {
  width: 300px;
  height: 180px;
  object-fit: cover;
  border-radius: var(--radius-base, 8px);
  display: block;
}

.upload-placeholder {
  width: 300px;
  height: 180px;
  border: 2px dashed var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-tertiary, #6b7280);
  transition: all var(--transition-fast, 150ms ease);
}

.upload-placeholder:hover {
  border-color: var(--color-primary, #2d5a3d);
  color: var(--color-primary, #2d5a3d);
}

.upload-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: var(--spacing-2, 8px);
}

.upload-tip {
  margin-top: var(--spacing-2, 8px);
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .publish-form {
    padding: var(--spacing-5, 20px);
  }

  .publish-form :deep(.el-form-item__label) {
    width: 80px !important;
  }

  .upload-image,
  .upload-placeholder {
    width: 100%;
    height: 200px;
  }
}
</style>
