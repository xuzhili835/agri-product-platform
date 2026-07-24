<template>
  <div class="admin-finance">
    <div class="header">
      <h2>融资申请管理</h2>
      <el-select
        v-model="statusFilter"
        placeholder="状态筛选"
        clearable
        @change="loadFinances"
        style="width: 150px"
      >
        <el-option label="全部" :value="-1" />
        <el-option label="申请中" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
    </div>

    <el-table :data="finances" style="width: 100%" v-loading="loading" border>
      <el-table-column label="申请人" width="120">
        <template #default="{ row }">{{ row.realName || row.ownName }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column label="申请金额" width="120">
        <template #default="{ row }">¥{{ row.money }}</template>
      </el-table-column>
      <el-table-column label="利率" width="90">
        <template #default="{ row }">{{ row.rate != null ? (row.rate + '%') : '—' }}</template>
      </el-table-column>
      <el-table-column label="期限" width="90">
        <template #default="{ row }">{{ row.repayment ? (row.repayment + '期') : '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="融资申请详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请人">{{ currentFinance.realName || currentFinance.ownName }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ currentFinance.phone }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">¥{{ currentFinance.money }}</el-descriptions-item>
        <el-descriptions-item label="利率">{{ currentFinance.rate != null ? (currentFinance.rate + '%') : '—' }}</el-descriptions-item>
        <el-descriptions-item label="还款期限">{{ currentFinance.repayment ? (currentFinance.repayment + '期') : '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentFinance.status)" size="small">{{ statusText(currentFinance.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ currentFinance.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentFinance.remark" label="备注" :span="2">{{ currentFinance.remark }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminFinances } from '@/api/admin'

const loading = ref(false)
const finances = ref([])
const statusFilter = ref(-1)
const detailVisible = ref(false)
const currentFinance = ref({})

const statusText = (s) => ({ 0: '申请中', 1: '已通过', 2: '已驳回' }[s] ?? '未知')
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')

onMounted(() => {
  loadFinances()
})

const loadFinances = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value !== -1) params.status = statusFilter.value
    const res = await getAdminFinances(params)
    finances.value = res.data || []
  } catch (error) {
    ElMessage.error('加载融资申请失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (finance) => {
  currentFinance.value = finance
  detailVisible.value = true
}
</script>

<style scoped>
.admin-finance {
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
