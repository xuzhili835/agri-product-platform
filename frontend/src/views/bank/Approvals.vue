<template>
  <div class="bank-approvals">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">融资审批</h1>
      <p class="page-subtitle">审核农户提交的融资申请，支持按状态、关键字与时间筛选</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card warning">
        <div class="stat-icon">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待审批</div>
        </div>
      </div>
      <div class="stat-card success">
        <div class="stat-icon">
          <el-icon><Select /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.approved }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </div>
      <div class="stat-card error">
        <div class="stat-icon">
          <el-icon><Close /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.rejected }}</div>
          <div class="stat-label">已拒绝</div>
        </div>
      </div>
      <div class="stat-card primary">
        <div class="stat-icon">
          <el-icon><Money /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">¥ {{ formatNumber(stats.totalAmount) }}</div>
          <div class="stat-label">申请总额</div>
        </div>
      </div>
    </div>

    <!-- 筛选 + 列表 -->
    <div class="content-card">
      <div class="card-header">
        <h3 class="card-title">申请列表</h3>
        <div class="filter-bar">
          <el-select
            v-model="filter.status"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @change="onFilterChange"
          >
            <el-option label="待审批" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
          <el-select
            v-model="filter.productId"
            placeholder="全部融资产品"
            clearable
            filterable
            style="width: 200px"
            @change="onFilterChange"
          >
            <el-option
              v-for="p in financeProducts"
              :key="p.productId"
              :label="`${p.bankName}${p.status === 1 ? '（已暂停）' : ''}`"
              :value="p.productId"
            />
          </el-select>
          <el-input
            v-model="filter.keyword"
            placeholder="搜索姓名 / 手机号"
            clearable
            style="width: 200px"
            @change="onFilterChange"
            @clear="onFilterChange"
          />
          <el-date-picker
            v-model="filter.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            @change="onFilterChange"
          />
          <el-button type="primary" plain @click="resetFilter">重置</el-button>
        </div>
      </div>

      <div v-loading="loading" class="table-wrapper">
        <el-table :data="applications" stripe style="width: 100%">
          <el-table-column label="申请人" width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.realName || row.ownName }}</template>
          </el-table-column>
          <el-table-column label="信用" width="130" align="center">
            <template #default="{ row }">
              <el-rate :model-value="row.credit || 0" disabled size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="联系电话" width="130" />
          <el-table-column label="融资产品" width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.productName || '—' }}</template>
          </el-table-column>
          <el-table-column label="申请金额" width="120" align="right">
            <template #default="{ row }">¥ {{ formatNumber(row.money) }}</template>
          </el-table-column>
          <el-table-column label="年利率" width="90" align="right">
            <template #default="{ row }">{{ row.rate != null ? row.rate + '%' : '—' }}</template>
          </el-table-column>
          <el-table-column label="期限" width="90" align="right">
            <template #default="{ row }">{{ row.repayment ? row.repayment + ' 个月' : '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" width="160">
            <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" type="success" link size="small" @click="openApprove(row, 1)">通过</el-button>
              <el-button v-if="row.status === 0" type="danger" link size="small" @click="openApprove(row, 2)">拒绝</el-button>
              <el-button type="primary" link size="small" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无融资申请" />
          </template>
        </el-table>

        <div v-if="pagination.total > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="loadApplications"
            @size-change="loadApplications"
          />
        </div>
      </div>
    </div>

    <!-- 审批对话框 -->
    <el-dialog v-model="approveDialogVisible" :title="approveForm.status === 1 ? '审核通过' : '审核拒绝'" width="520px">
      <div v-if="current" class="approve-summary">
        <p><span class="label">申请人</span>{{ current.realName || current.ownName }}</p>
        <p><span class="label">联系电话</span>{{ current.phone || '—' }}</p>
        <p><span class="label">融资产品</span>{{ current.productName || '—' }}</p>
        <p><span class="label">申请金额</span>¥ {{ formatNumber(current.money) }}</p>
        <p>
          <span class="label">信用分</span>
          <el-rate :model-value="current.credit || 0" disabled size="small" show-score score-template="{value} 星" style="vertical-align: middle" />
        </p>
        <p><span class="label">融资用途</span>{{ current.purpose || '—' }}</p>
      </div>
      <el-form label-width="90px" style="margin-top: 16px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="approveForm.status">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="approveForm.status === 1 ? '通过说明' : '拒绝原因'">
          <el-input
            v-model="approveForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="approveForm.status === 1 ? '通过备注（选填）' : '请填写拒绝原因'"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button
          :type="approveForm.status === 1 ? 'success' : 'danger'"
          :loading="approving"
          @click="submitApprove"
        >
          确认{{ approveForm.status === 1 ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="融资申请详情" width="680px">
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="申请人">{{ current.realName || current.ownName }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ current.ownName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ current.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ current.idNum || '—' }}</el-descriptions-item>
        <el-descriptions-item label="信用分">
          <el-rate :model-value="current.credit || 0" disabled size="small" show-score score-template="{value} 星" style="vertical-align: middle" />
        </el-descriptions-item>
        <el-descriptions-item label="融资产品">{{ current.productName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">¥ {{ formatNumber(current.money) }}</el-descriptions-item>
        <el-descriptions-item label="年利率">{{ current.rate != null ? current.rate + '%' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="还款期限">{{ current.repayment ? current.repayment + ' 个月' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="融资用途" :span="2">{{ current.purpose || '—' }}</el-descriptions-item>
        <el-descriptions-item label="还款来源" :span="2">{{ current.repaymentSource || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="hasCombination(current)" label="联合贷款人" :span="2">
          <div class="combination-block">
            <div v-if="current.combinationName1 || current.combinationPhone1 || current.combinationIdnum1" class="combination-person">
              <span class="cp-label">联合人1：</span>{{ current.combinationName1 || '—' }} · {{ current.combinationPhone1 || '—' }} · {{ current.combinationIdnum1 || '—' }}
            </div>
            <div v-if="current.combinationName2 || current.combinationPhone2 || current.combinationIdnum2" class="combination-person">
              <span class="cp-label">联合人2：</span>{{ current.combinationName2 || '—' }} · {{ current.combinationPhone2 || '—' }} · {{ current.combinationIdnum2 || '—' }}
            </div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="申请状态">
          <el-tag :type="statusType(current.status)" size="small">{{ statusText(current.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(current.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="current.remark" label="审批备注" :span="2">{{ current.remark }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="current && current.status === 0" type="success" @click="openApprove(current, 1); detailVisible = false">
          去审批
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Select, Close, Money } from '@element-plus/icons-vue'
import { getBankApplications, getBankStats, approveFinanceApplication, getBankProducts } from '@/api/finance'

const loading = ref(false)
const approving = ref(false)
const applications = ref([])
const financeProducts = ref([])
const current = ref(null)

const approveDialogVisible = ref(false)
const detailVisible = ref(false)

const filter = reactive({
  status: undefined,
  productId: undefined,
  keyword: '',
  dateRange: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const stats = reactive({
  pending: 0,
  approved: 0,
  rejected: 0,
  totalAmount: 0
})

const approveForm = reactive({
  status: 1,
  remark: ''
})

onMounted(() => {
  loadApplications()
  loadStats()
  loadFinanceProducts()
})

const loadFinanceProducts = async () => {
  try {
    // 拉取本行全部融资产品（含已暂停），供审批按产品筛选（含已暂停产品的历史申请）
    const res = await getBankProducts({ page: 1, pageSize: 200 })
    financeProducts.value = res.data?.records || []
  } catch (error) {
    console.error('加载融资产品失败:', error)
  }
}

const loadApplications = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }
    if (filter.status !== undefined && filter.status !== null && filter.status !== '') {
      params.status = filter.status
    }
    if (filter.productId !== undefined && filter.productId !== null && filter.productId !== '') {
      params.productId = filter.productId
    }
    if (filter.keyword && filter.keyword.trim()) {
      params.keyword = filter.keyword.trim()
    }
    if (filter.dateRange && filter.dateRange.length === 2) {
      params.startDate = filter.dateRange[0]
      params.endDate = filter.dateRange[1]
    }

    const res = await getBankApplications(params)
    applications.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error?.message || '加载融资申请失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const { data } = await getBankStats()
    stats.pending = data?.pendingCount || 0
    stats.approved = data?.approvedCount || 0
    stats.rejected = data?.rejectedCount || 0
    stats.totalAmount = data?.totalAmount || 0
  } catch (error) {
    // 静默：统计加载失败不打扰用户
    console.error('加载统计失败:', error)
  }
}

const onFilterChange = () => {
  pagination.page = 1
  loadApplications()
}

const resetFilter = () => {
  filter.status = undefined
  filter.productId = undefined
  filter.keyword = ''
  filter.dateRange = null
  pagination.page = 1
  loadApplications()
}

const openApprove = (row, status) => {
  current.value = row
  approveForm.status = status
  approveForm.remark = row.remark || ''
  approveDialogVisible.value = true
}

const submitApprove = async () => {
  if (!current.value) return
  approving.value = true
  try {
    await approveFinanceApplication(current.value.financeId, {
      status: approveForm.status,
      remark: approveForm.remark
    })
    ElMessage.success(approveForm.status === 1 ? '已通过该申请' : '已拒绝该申请')
    approveDialogVisible.value = false
    await Promise.all([loadApplications(), loadStats()])
  } catch (error) {
    ElMessage.error(error?.message || '操作失败，请重试')
  } finally {
    approving.value = false
  }
}

const openDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const hasCombination = (d) => !!(d && (d.combinationName1 || d.combinationPhone1 || d.combinationIdnum1 || d.combinationName2 || d.combinationPhone2 || d.combinationIdnum2))
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')
const statusText = (s) => ({ 0: '待审批', 1: '已通过', 2: '已拒绝' }[s] || '未知')

const formatNumber = (num) => {
  if (num === null || num === undefined || num === '') return '0'
  return Number(num).toLocaleString()
}

const formatTime = (t) => {
  if (!t) return '—'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

<style scoped>
.bank-approvals {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

/* 页面标题 */
.page-header {
  margin-bottom: var(--spacing-1, 4px);
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

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-4, 16px);
}
.stat-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-4, 16px);
  padding: var(--spacing-5, 20px);
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(31, 41, 35, 0.06));
  transition: all var(--transition-fast, 150ms ease);
}
.stat-card:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(31, 41, 35, 0.08));
  transform: translateY(-2px);
}
.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg, 12px);
  font-size: 24px;
  flex-shrink: 0;
}
.stat-card.warning .stat-icon { background: var(--color-warning-light, #fdf8e8); color: var(--color-warning, #c9a661); }
.stat-card.success .stat-icon { background: var(--color-success-light, #e8f0ec); color: var(--color-success, #4a7c59); }
.stat-card.error .stat-icon { background: var(--color-error-light, #fce8e0); color: var(--color-error, #b85c38); }
.stat-card.primary .stat-icon { background: rgba(45, 90, 61, 0.1); color: var(--color-bank, #2d5a3d); }
.stat-value {
  font-size: var(--font-size-2xl, 30px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  line-height: var(--line-height-tight, 1.25);
}
.stat-label {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin-top: var(--spacing-1, 4px);
}

/* 内容卡片 */
.content-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e5e0d8);
  box-shadow: var(--shadow-sm, 0 2px 4px rgba(31, 41, 35, 0.06));
  overflow: hidden;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px) var(--spacing-5, 20px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
  flex-wrap: wrap;
}
.card-title {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
  flex-wrap: wrap;
}
.table-wrapper {
  padding: var(--spacing-4, 16px);
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--spacing-3, 12px);
}

/* 审批摘要 */
.approve-summary {
  padding: var(--spacing-4, 16px);
  background: var(--color-bg-secondary, #f7f5f0);
  border-radius: var(--radius-base, 8px);
}
.approve-summary p {
  margin: var(--spacing-1, 4px) 0;
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-primary, #1f2923);
}
.approve-summary .label {
  display: inline-block;
  width: 80px;
  color: var(--color-text-tertiary, #6b7280);
}

.combination-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.combination-person {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
  line-height: 1.5;
}
.cp-label {
  color: var(--color-text-tertiary, #6b7280);
  margin-right: 4px;
}

@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
  .filter-bar { width: 100%; }
  .filter-bar .el-select,
  .filter-bar .el-input,
  .filter-bar .el-date-picker { width: 100% !important; }
}
</style>
