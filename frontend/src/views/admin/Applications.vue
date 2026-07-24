<template>
  <div class="admin-applications">
    <div class="header">
      <h2>角色申请审核</h2>
      <el-radio-group v-model="filterStatus" @change="loadApplications">
        <el-radio-button :value="-1">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已驳回</el-radio-button>
      </el-radio-group>
    </div>

    <el-table :data="applications" v-loading="loading" border>
      <el-table-column prop="userName" label="申请人" width="120" />
      <el-table-column prop="targetRole" label="申请角色" width="90">
        <template #default="{ row }">{{ roleMap[row.targetRole] || row.targetRole }}</template>
      </el-table-column>
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="专业/银行" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.targetRole === 'bank' ? row.belong : row.profession }}
          <span v-if="row.targetRole === 'expert' && row.position">（{{ row.position }}）</span>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="申请理由" show-overflow-tooltip />
      <el-table-column label="材料" width="90">
        <template #default="{ row }">
          <template v-if="row.materials">
            <el-link
              v-for="(url, i) in splitMaterials(row.materials)"
              :key="i"
              :href="url"
              target="_blank"
              type="primary"
              style="margin-right: 8px"
            >材料{{ splitMaterials(row.materials).length > 1 ? i + 1 : '' }}</el-link>
          </template>
          <span v-else style="color:#c0c4cc">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="170" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button link type="success" @click="openReview(row, 1)">通过</el-button>
            <el-button link type="danger" @click="openReview(row, 2)">驳回</el-button>
          </template>
          <span v-else style="color: #909399">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewVisible" :title="reviewStatus === 1 ? '通过申请' : '驳回申请'" width="480px">
      <el-form label-width="90px">
        <el-form-item label="申请人">{{ current?.userName }}（{{ roleMap[current?.targetRole] }}）</el-form-item>
        <el-form-item label="相关材料">
          <template v-if="current?.materials">
            <el-link
              v-for="(url, i) in splitMaterials(current.materials)"
              :key="i"
              :href="url"
              target="_blank"
              type="primary"
              style="margin-right: 12px"
            >材料{{ splitMaterials(current.materials).length > 1 ? i + 1 : '' }}</el-link>
          </template>
          <span v-else style="color:#909399">未上传</span>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="reviewRemark" type="textarea" :rows="3" placeholder="可选：填写审核意见" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button :type="reviewStatus === 1 ? 'success' : 'danger'" @click="confirmReview" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoleApplications, reviewRoleApplication } from '@/api/admin'

const loading = ref(false)
const submitting = ref(false)
const applications = ref([])
const filterStatus = ref(0)
const reviewVisible = ref(false)
const reviewStatus = ref(1)
const reviewRemark = ref('')
const current = ref(null)

const roleMap = { expert: '专家', bank: '银行' }
const statusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[s] || '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')
// 材料可能为多个 URL（逗号拼接），拆分后逐个展示
const splitMaterials = (m) => (m ? String(m).split(',').map(s => s.trim()).filter(Boolean) : [])

const loadApplications = async () => {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value !== -1) params.status = filterStatus.value
    const res = await getRoleApplications(params)
    applications.value = res.data || []
  } catch (e) {
    ElMessage.error('加载申请列表失败')
  } finally {
    loading.value = false
  }
}

const openReview = (row, status) => {
  current.value = row
  reviewStatus.value = status
  reviewRemark.value = ''
  reviewVisible.value = true
}

const confirmReview = async () => {
  submitting.value = true
  try {
    await reviewRoleApplication(current.value.id, { status: reviewStatus.value, reviewRemark: reviewRemark.value })
    ElMessage.success(reviewStatus.value === 1 ? '已通过' : '已驳回')
    reviewVisible.value = false
    loadApplications()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.admin-applications {
  max-width: 1280px;
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
</style>
