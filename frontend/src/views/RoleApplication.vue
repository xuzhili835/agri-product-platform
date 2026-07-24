<template>
  <div class="role-application-page">
    <div class="container">
      <div class="page-header">
        <el-button @click="$router.back()" style="margin-bottom: 12px;">← 返回</el-button>
        <h1>角色升级申请</h1>
        <p class="subtitle">农户/买家可申请成为「专家」或「银行」角色，提交后由管理员审核</p>
      </div>

      <!-- 申请表单 -->
      <el-card v-if="canApply" class="form-card">
        <template #header><span>提交新申请</span></template>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
          <el-form-item label="申请成为" prop="targetRole">
            <el-radio-group v-model="form.targetRole">
              <el-radio value="expert">专家</el-radio>
              <el-radio value="bank">银行</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>

          <template v-if="form.targetRole === 'expert'">
            <el-form-item label="专业领域" prop="profession">
              <el-input v-model="form.profession" placeholder="如：农学、植保、土壤肥料" />
            </el-form-item>
            <el-form-item label="职位">
              <el-input v-model="form.position" placeholder="如：教授、研究员" />
            </el-form-item>
            <el-form-item label="所属单位">
              <el-input v-model="form.belong" placeholder="如：山东省农科院" />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item label="银行名称" prop="belong">
              <el-input v-model="form.belong" placeholder="如：青岛银行" />
            </el-form-item>
          </template>

          <el-form-item label="申请理由" prop="reason">
            <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请简述申请理由及相关资质" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item label="相关材料">
            <el-upload
              action="/api/upload"
              :before-upload="beforeMaterialUpload"
              :on-success="handleMaterialSuccess"
              :on-remove="handleMaterialRemove"
              :file-list="materialFileList"
              :limit="1"
              :on-exceed="() => ElMessage.warning('最多上传1个材料文件')"
            >
              <el-button type="primary" plain>上传材料</el-button>
              <template #tip>
                <div class="el-upload__tip">资质证书/相关证明，支持 jpg/png/pdf，单个 &lt; 10MB（可选）</div>
              </template>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submit" :loading="submitting">提交申请</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-alert v-else type="info" :closable="false" show-icon class="form-card">
        您当前角色为「{{ roleLabel }}」，无需提交角色升级申请。
      </el-alert>

      <!-- 我的申请记录 -->
      <el-card class="history-card">
        <template #header><span>我的申请记录</span></template>
        <el-table :data="applications" v-loading="loading" border>
          <el-table-column prop="targetRole" label="申请角色" width="100">
            <template #default="{ row }">{{ roleMap[row.targetRole] || row.targetRole }}</template>
          </el-table-column>
          <el-table-column prop="profession" label="专业/银行" show-overflow-tooltip>
            <template #default="{ row }">{{ row.targetRole === 'bank' ? row.belong : row.profession }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="申请理由" show-overflow-tooltip />
          <el-table-column label="材料" width="80">
            <template #default="{ row }">
              <el-link v-if="row.materials" :href="row.materials" target="_blank" type="primary">查看</el-link>
              <span v-else style="color:#c0c4cc">—</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewRemark" label="审核备注" show-overflow-tooltip />
          <el-table-column prop="createTime" label="提交时间" width="170" />
        </el-table>
        <el-empty v-if="!loading && applications.length === 0" description="暂无申请记录" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { applyRole, getMyApplications } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const loading = ref(false)
const applications = ref([])

const roleMap = { expert: '专家', bank: '银行' }
const roleLabel = computed(() => {
  const m = { farmer: '农户', buyer: '买家', expert: '专家', bank: '银行', admin: '管理员' }
  return m[userStore.role] || userStore.role
})
const canApply = computed(() => userStore.role === 'farmer' || userStore.role === 'buyer')

const form = reactive({
  targetRole: 'expert',
  realName: userStore.userInfo?.realName || '',
  phone: userStore.userInfo?.phone || '',
  profession: '',
  position: '',
  belong: '',
  reason: '',
  materials: ''
})

const rules = {
  targetRole: [{ required: true, message: '请选择申请角色', trigger: 'change' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  profession: [{ required: true, message: '请输入专业领域', trigger: 'blur' }],
  belong: [{ required: true, message: '请输入银行名称', trigger: 'blur' }],
  reason: [{ required: true, message: '请填写申请理由', trigger: 'blur' }]
}

const statusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getMyApplications()
    applications.value = res.data || []
  } catch (e) {
    ElMessage.error('加载申请记录失败')
  } finally {
    loading.value = false
  }
}

const materialFileList = ref([])
const beforeMaterialUpload = (raw) => {
  const okType = ['image/jpeg', 'image/png', 'image/jpg', 'application/pdf'].includes(raw.type)
  if (!okType) { ElMessage.error('仅支持 JPG/PNG/PDF 格式'); return false }
  if (raw.size / 1024 / 1024 > 10) { ElMessage.error('文件不能超过 10MB'); return false }
  return true
}
const handleMaterialSuccess = (response, file) => {
  if (response.code === 200) {
    form.materials = response.data.url
    materialFileList.value = [file]
    ElMessage.success('材料上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}
const handleMaterialRemove = () => {
  form.materials = ''
  materialFileList.value = []
}

const submit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    await applyRole({ ...form })
    ElMessage.success('申请已提交，请等待管理员审核')
    form.profession = ''
    form.position = ''
    form.belong = ''
    form.reason = ''
    form.materials = ''
    materialFileList.value = []
    loadApplications()
  } catch (e) {
    if (e !== false) ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.role-application-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}
.container {
  max-width: 860px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-5, 20px);
}
.page-header h1 {
  font-size: var(--font-size-3xl, 38px);
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--color-text-primary, #1f2923);
}
.subtitle {
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}
.form-card, .history-card {
  border-radius: var(--radius-lg, 12px);
}
</style>
