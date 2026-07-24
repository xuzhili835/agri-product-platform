<template>
  <div class="expert-profile-container">
    <div class="page-header">
      <h2>专家个人中心</h2>
      <el-button type="primary" @click="toggleEdit" :icon="editMode ? Close : Edit">
        {{ editMode ? '取消编辑' : '编辑资料' }}
      </el-button>
    </div>

    <!-- 专家信息卡片 -->
    <el-card class="profile-card">
      <template #header>
        <div class="card-header"><span>基本信息</span></div>
      </template>

      <div class="avatar-row">
        <el-avatar :size="84" :src="avatarUrl" class="expert-avatar">
          {{ avatarFallback }}
        </el-avatar>
        <div class="avatar-meta">
          <el-upload
            action="/api/upload"
            :show-file-list="false"
            accept="image/*"
            :before-upload="beforeAvatarUpload"
            :on-success="onAvatarSuccess"
            :on-error="onAvatarError"
          >
            <el-button :loading="uploading" :icon="Camera">修改头像</el-button>
          </el-upload>
          <div class="avatar-tip">支持 JPG / PNG，建议正方形，≤5MB</div>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="专家用户名">
          {{ expertInfo.userName || '—' }}
        </el-descriptions-item>

        <el-descriptions-item label="专家姓名">
          <span v-if="!editMode">{{ expertInfo.realName || '—' }}</span>
          <el-input v-else v-model="form.realName" placeholder="请输入姓名" />
        </el-descriptions-item>

        <el-descriptions-item label="联系电话">
          <span v-if="!editMode">{{ expertInfo.phone || '—' }}</span>
          <el-input v-else v-model="form.phone" placeholder="请输入电话" />
        </el-descriptions-item>

        <el-descriptions-item label="专业领域">
          <span v-if="!editMode">{{ expertInfo.profession || '—' }}</span>
          <el-input v-else v-model="form.profession" placeholder="如：农学、植保、土壤肥料" />
        </el-descriptions-item>

        <el-descriptions-item label="职位">
          <span v-if="!editMode">{{ expertInfo.position || '—' }}</span>
          <el-input v-else v-model="form.position" placeholder="如：教授、研究员" />
        </el-descriptions-item>

        <el-descriptions-item label="所属单位">
          <span v-if="!editMode">{{ expertInfo.belong || '—' }}</span>
          <el-input v-else v-model="form.belong" placeholder="如：山东省农科院" />
        </el-descriptions-item>
      </el-descriptions>

      <div class="save-actions" v-if="editMode">
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="saveProfile" :loading="saving">保存修改</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Close, Camera } from '@element-plus/icons-vue'
import { getExpertInfo, updateExpertInfo } from '@/api/expert'
import { updateUserInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const editMode = ref(false)
const saving = ref(false)
const uploading = ref(false)

// 头像：来源 tb_user.avatar（由 /user/info 返回）。专家头像复用用户表头像字段，无需新表。
const avatarUrl = ref(userStore.userInfo?.avatar || '')
const avatarFallback = computed(() => {
  const name = expertInfo.value?.realName || userStore.userInfo?.realName || userStore.userInfo?.userName || '专'
  return name.charAt(0)
})

const expertInfo = ref({})

const form = reactive({
  realName: '',
  phone: '',
  profession: '',
  position: '',
  belong: ''
})

onMounted(() => {
  loadExpertInfo()
})

const loadExpertInfo = async () => {
  try {
    const res = await getExpertInfo()
    if (res.data) {
      expertInfo.value = res.data
      Object.assign(form, res.data)
    }
  } catch (error) {
    ElMessage.error('加载专家信息失败')
  }
}

const toggleEdit = () => {
  editMode.value = !editMode.value
  if (editMode.value) {
    Object.assign(form, expertInfo.value)
  }
}

const cancelEdit = () => {
  Object.assign(form, expertInfo.value)
  editMode.value = false
}

const saveProfile = async () => {
  try {
    saving.value = true
    await updateExpertInfo(form)
    ElMessage.success('保存成功')
    expertInfo.value = { ...expertInfo.value, ...form }
    editMode.value = false
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 头像上传前校验：仅图片、≤5MB
const beforeAvatarUpload = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('仅支持上传图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  uploading.value = true
  return true
}

// 上传成功：把 url 写回 tb_user.avatar，并同步本地与全局 userInfo
const onAvatarSuccess = async (res) => {
  uploading.value = false
  const url = res?.data?.url
  if (!url) {
    ElMessage.error('上传失败，未获取到图片地址')
    return
  }
  try {
    await updateUserInfo({ avatar: url })
    avatarUrl.value = url
    // 同步到全局用户信息，使侧边栏/首页头像即时刷新
    if (userStore.userInfo) {
      userStore.setUserInfo({ ...userStore.userInfo, avatar: url })
    }
    ElMessage.success('头像更新成功')
  } catch (error) {
    ElMessage.error(error?.message || '保存头像失败')
  }
}

const onAvatarError = () => {
  uploading.value = false
  ElMessage.error('头像上传失败，请重试')
}
</script>

<style scoped>
.expert-profile-container {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #303133;
}

.profile-card {
  margin-bottom: 20px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding: 8px 0;
}

.expert-avatar {
  flex-shrink: 0;
  background: #2d5a3d;
  color: #fff;
  font-size: 30px;
  font-weight: 600;
}

.avatar-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.avatar-tip {
  font-size: 12px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.save-actions {
  margin-top: 20px;
  text-align: right;
}
</style>
