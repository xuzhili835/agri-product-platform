<template>
  <div class="bank-repayments">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">还款审核</h1>
      <p class="page-subtitle">核验农户提交的各期还款（流水号 / 凭证），确认后计入农户信用</p>
    </div>

    <!-- 提示 -->
    <div class="tip-card">
      <el-icon class="tip-icon"><InfoFilled /></el-icon>
      <div class="tip-text">
        农户在「我的融资」逐期提交还款（填流水号 + 上传凭证）后，状态为<span class="em">「待确认」</span>。
        银行核验后：<span class="em">确认</span>则置为已还且农户信用 +1；<span class="em">驳回</span>则退回，农户可重新还款。
      </div>
    </div>

    <!-- 列表 -->
    <div v-loading="loading" class="content-card">
      <div class="card-header">
        <h3 class="card-title">还款列表</h3>
        <el-radio-group v-model="filterStatus" @change="onFilterChange">
          <el-radio-button :label="2">待确认</el-radio-button>
          <el-radio-button :label="1">已确认</el-radio-button>
          <el-radio-button :label="3">已驳回</el-radio-button>
        </el-radio-group>
      </div>

      <div class="table-wrapper">
        <el-table :data="rows" stripe style="width: 100%">
          <el-table-column label="农户" min-width="110" show-overflow-tooltip>
            <template #default="{ row }">{{ row.farmerName || '—' }}</template>
          </el-table-column>
          <el-table-column label="融资申请" width="100" align="center">
            <template #default="{ row }">#{{ row.financeId }}</template>
          </el-table-column>
          <el-table-column label="期数" width="80" align="center">
            <template #default="{ row }">第 {{ row.periodIndex }} 期</template>
          </el-table-column>
          <el-table-column label="应还金额" width="120" align="right">
            <template #default="{ row }">¥ {{ formatNumber(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="流水号/备注" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.transactionNo || '—' }}</template>
          </el-table-column>
          <el-table-column label="凭证" width="90" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.payProof"
                :src="row.payProof"
                :preview-src-list="[row.payProof]"
                fit="cover"
                style="width: 44px; height: 44px; border-radius: 6px"
                preview-teleported
              />
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ formatTime(row.paidTime) }}</template>
          </el-table-column>
          <el-table-column v-if="filterStatus === 3" label="驳回原因" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 2">
                <el-button type="primary" link size="small" :loading="acting === row.id" @click="onConfirm(row)">确认</el-button>
                <el-button type="danger" link size="small" :loading="acting === row.id" @click="onReject(row)">驳回</el-button>
              </template>
              <span v-else class="muted">{{ row.status === 1 ? '已确认' : '已驳回' }}</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :description="emptyText" />
          </template>
        </el-table>

        <div v-if="total > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadList"
            @size-change="loadList"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getBankRepayments, confirmRepayment, rejectRepayment } from '@/api/finance'

const loading = ref(false)
const acting = ref(null)
const rows = ref([])

// 默认看待确认队列：0未还 / 1已还 / 2待确认 / 3已驳回
const filterStatus = ref(2)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const emptyText = computed(() => {
  if (filterStatus.value === 2) return '暂无待确认的还款'
  if (filterStatus.value === 1) return '暂无已确认的还款记录'
  return '暂无已驳回的还款记录'
})

onMounted(() => {
  loadList()
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await getBankRepayments({
      status: filterStatus.value,
      page: page.value,
      pageSize: pageSize.value
    })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error(e?.message || '加载还款列表失败')
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  page.value = 1
  loadList()
}

const onConfirm = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认 ${row.farmerName || ''} 的第 ${row.periodIndex} 期还款（¥${formatNumber(row.totalAmount)}）？确认后农户信用 +1。`,
      '确认还款',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  acting.value = row.id
  try {
    await confirmRepayment(row.id)
    ElMessage.success('已确认还款')
    await loadList()
  } catch (e) {
    ElMessage.error(e?.message || '确认失败')
  } finally {
    acting.value = null
  }
}

const onReject = async (row) => {
  let reason = ''
  try {
    const res = await ElMessageBox.prompt('请输入驳回原因（将通知农户）', '驳回还款', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '如：凭证不清晰 / 流水号核对不上',
      inputValidator: (v) => (v && v.trim()) ? true : '请填写驳回原因'
    })
    reason = res.value
  } catch (e) {
    return
  }
  acting.value = row.id
  try {
    await rejectRepayment(row.id, { reason: reason.trim() })
    ElMessage.success('已驳回还款')
    await loadList()
  } catch (e) {
    ElMessage.error(e?.message || '驳回失败')
  } finally {
    acting.value = null
  }
}

const formatNumber = (num) => {
  if (num === null || num === undefined || num === '') return '0'
  return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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
.bank-repayments {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

.page-header { margin-bottom: var(--spacing-1, 4px); }
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

.tip-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px) var(--spacing-5, 20px);
  background: var(--color-warning-light, #fdf8e8);
  border: 1px solid #f0e1b8;
  border-radius: var(--radius-lg, 12px);
  color: var(--color-gold-dark, #a6873f);
}
.tip-icon { font-size: 22px; flex-shrink: 0; }
.tip-text { font-size: var(--font-size-sm, 14px); line-height: var(--line-height-normal, 1.5); }
.tip-text .em { font-weight: var(--font-weight-semibold, 600); }

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
.table-wrapper { padding: var(--spacing-4, 16px); }
.muted { color: var(--color-text-tertiary, #909399); font-size: 13px; }
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--spacing-3, 12px);
}
</style>
