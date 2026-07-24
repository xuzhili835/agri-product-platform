<template>
  <div class="expert-knowledge-page">
    <div class="page-header">
      <h1>知识发布</h1>
      <p class="subtitle">发布农业知识文章，分享专业经验</p>
    </div>

    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        发布新知识
      </el-button>
    </div>

    <!-- 知识列表 -->
    <div class="knowledge-list" v-loading="loading">
      <div v-for="knowledge in knowledgeList" :key="knowledge.id" class="knowledge-card">
        <div class="knowledge-header">
          <div class="knowledge-info">
            <h3 class="knowledge-title">{{ knowledge.title }}</h3>
            <div class="knowledge-meta">
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(knowledge.createTime) }}
              </span>
            </div>
          </div>
          <div class="knowledge-status">
            <el-tag :type="knowledge.status === 1 ? 'success' : 'info'" size="large">
              {{ knowledge.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </div>
        </div>

        <div class="knowledge-content">
          <p class="content-preview">{{ stripHtml(knowledge.content) }}</p>
        </div>

        <div class="knowledge-actions">
          <el-button size="small" @click="handleView(knowledge)">
            <el-icon><View /></el-icon>
            查看
          </el-button>
          <el-button size="small" @click="handleEdit(knowledge)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(knowledge)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
          <el-button
            v-if="knowledge.status === 0"
            size="small"
            type="success"
            @click="handlePublish(knowledge)"
          >
            <el-icon><Promotion /></el-icon>
            发布
          </el-button>
        </div>
      </div>

      <el-empty v-if="!loading && knowledgeList.length === 0" description="暂无发布的知识" />
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadKnowledge"
        @size-change="loadKnowledge"
      />
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isEditing ? '编辑知识' : '发布新知识'"
      width="700px"
      @close="handleCloseDialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="formData.title"
            placeholder="请输入知识文章标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="10"
            placeholder="请输入知识文章内容..."
            maxlength="255"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="封面图片">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :before-upload="beforeCoverUpload"
            :on-success="handleCoverSuccess"
            action="/api/upload"
            accept="image/*"
          >
            <img v-if="formData.picPath" :src="formData.picPath" class="cover-preview" />
            <div v-else class="cover-placeholder">
              <el-icon><Plus /></el-icon>
              <span>上传封面图片</span>
            </div>
          </el-upload>
          <div class="cover-tip">支持 jpg/png，≤5MB；发布后在知识列表与详情页展示。</div>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="0">保存为草稿</el-radio>
            <el-radio :value="1">直接发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ formData.status === 1 ? '发布' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Calendar,
  View,
  Edit,
  Delete,
  Promotion
} from '@element-plus/icons-vue'
import {
  getKnowledgeList,
  publishKnowledge,
  updateKnowledge,
  deleteKnowledge
} from '@/api/knowledge'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 去除 HTML 标签，用于列表预览纯文本展示（避免出现 <p> 等标签）
const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim()
}

const loading = ref(false)
const submitting = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const knowledgeList = ref([])

const editDialogVisible = ref(false)
const isEditing = ref(false)
const formRef = ref(null)

const formData = reactive({
  knowledgeId: null,
  title: '',
  content: '',
  picPath: '',
  status: 1
})

const formRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度应在5-100个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 10, max: 255, message: '内容长度应在10-255个字符之间', trigger: 'blur' }
  ]
}

// 加载知识列表
const loadKnowledge = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      ownName: userStore.userInfo?.userName || userStore.userName
    }

    const res = await getKnowledgeList(params)
    if (res.code === 200) {
      knowledgeList.value = (res.data.records || []).map(k => ({
        id: k.knowledgeId || k.id,
        title: k.title,
        content: k.content,
        picPath: k.picPath,
        ownName: k.ownName,
        status: k.status,
        createTime: k.createTime
      }))
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 创建新知识
const handleCreate = () => {
  isEditing.value = false
  formData.knowledgeId = null
  formData.title = ''
  formData.content = ''
  formData.picPath = ''
  formData.status = 1
  editDialogVisible.value = true
}

// 编辑知识
const handleEdit = (knowledge) => {
  isEditing.value = true
  formData.knowledgeId = knowledge.id
  formData.title = knowledge.title
  formData.content = knowledge.content
  formData.picPath = knowledge.picPath || ''
  formData.status = knowledge.status
  editDialogVisible.value = true
}

// 查看知识：跳转到知识详情页（含评论区），此前本地对话框看不到评论
const handleView = (knowledge) => {
  router.push({ path: `/knowledge/${knowledge.id}`, query: { from: route.fullPath } })
}

// 删除知识
const handleDelete = async (knowledge) => {
  try {
    await ElMessageBox.confirm('确定要删除这篇知识吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteKnowledge(knowledge.id)
    ElMessage.success('删除成功')
    loadKnowledge()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 发布知识
const handlePublish = async (knowledge) => {
  try {
    await ElMessageBox.confirm('确定要发布这篇知识吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await updateKnowledge(knowledge.id, {
      title: knowledge.title,
      content: knowledge.content,
      picPath: knowledge.picPath,
      status: 1
    })
    ElMessage.success('发布成功')
    loadKnowledge()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发布失败')
    }
  }
}

// 关闭对话框
const handleCloseDialog = () => {
  formRef.value?.resetFields()
}

// 封面图片上传前校验：仅图片、≤5MB
const beforeCoverUpload = (rawFile) => {
  const isImage = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'].includes(rawFile.type)
  if (!isImage) {
    ElMessage.error('只能上传 JPG/PNG 格式的图片')
    return false
  }
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 封面图片上传成功：写入 picPath
const handleCoverSuccess = (response) => {
  if (response.code === 200 && response.data?.url) {
    formData.picPath = response.data.url
    ElMessage.success('封面图片上传成功')
  } else {
    ElMessage.error(response.message || '封面图片上传失败')
  }
}

// 提交表单
const submitForm = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    const data = {
      title: formData.title,
      content: formData.content,
      picPath: formData.picPath,
      status: formData.status
    }

    if (isEditing.value) {
      await updateKnowledge(formData.knowledgeId, data)
      ElMessage.success('修改成功')
    } else {
      await publishKnowledge(data)
      ElMessage.success(formData.status === 1 ? '发布成功' : '保存成功')
    }

    editDialogVisible.value = false
    loadKnowledge()
  } catch (error) {
    if (error !== false) {
      ElMessage.error('操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadKnowledge()
})
</script>

<style scoped>
.expert-knowledge-page {
  padding: var(--spacing-6, 24px);
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--spacing-6, 24px);
}

.page-header h1 {
  font-size: var(--font-size-3xl, 38px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.subtitle {
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

.action-bar {
  margin-bottom: var(--spacing-4, 16px);
}

.knowledge-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
}

.knowledge-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-5, 20px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.knowledge-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-3, 12px);
  padding-bottom: var(--spacing-3, 12px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.knowledge-info {
  flex: 1;
}

.knowledge-title {
  font-size: var(--font-size-xl, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.knowledge-meta {
  display: flex;
  gap: var(--spacing-4, 16px);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.knowledge-content {
  margin-bottom: var(--spacing-3, 12px);
}

.content-preview {
  font-size: var(--font-size-base, 16px);
  line-height: 1.6;
  color: var(--color-text-secondary, #4a5249);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.knowledge-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-8, 32px);
}

.knowledge-detail h2 {
  font-size: var(--font-size-2xl, 28px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-3, 12px);
}

.detail-meta {
  display: flex;
  gap: var(--spacing-4, 16px);
  padding-bottom: var(--spacing-4, 16px);
  margin-bottom: var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.detail-content {
  font-size: var(--font-size-base, 16px);
  line-height: 1.8;
  color: var(--color-text-primary, #1f2923);
  white-space: pre-wrap;
}

.detail-image {
  margin-top: var(--spacing-4, 16px);
}

.detail-image img {
  max-width: 100%;
  border-radius: var(--radius-base, 8px);
}

/* ===== 封面图片上传 ===== */
.cover-uploader :deep(.el-upload) {
  border: 1px dashed var(--color-border, #d9d9d9);
  border-radius: var(--radius-base, 8px);
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}
.cover-uploader :deep(.el-upload):hover {
  border-color: var(--color-primary, #2d5a3d);
}
.cover-preview {
  width: 200px;
  height: 120px;
  object-fit: cover;
  display: block;
}
.cover-placeholder {
  width: 200px;
  height: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--color-text-tertiary, #6b7280);
  font-size: 13px;
}
.cover-placeholder .el-icon {
  font-size: 28px;
}
.cover-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-tertiary, #6b7280);
}
</style>
