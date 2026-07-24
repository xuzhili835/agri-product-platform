<template>
  <div class="expert-reservations-page">
    <div class="page-header">
      <h1>预约管理</h1>
      <p class="subtitle">管理农户和买家的预约请求</p>
    </div>

    <!-- 筛选区 -->
    <div class="filter-section">
      <el-radio-group v-model="filterStatus" @change="loadReservations">
        <el-radio-button :value="-1">全部</el-radio-button>
        <el-radio-button :value="0">待处理</el-radio-button>
        <el-radio-button :value="1">已完成</el-radio-button>
        <el-radio-button :value="2">已拒绝</el-radio-button>
        <el-radio-button :value="3">已过期</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 预约列表 -->
    <div class="reservations-list" v-loading="loading">
      <div v-for="reserve in reserveList" :key="reserve.id" class="reserve-card">
        <div class="reserve-header">
          <div class="reserve-info">
            <h3 class="reserve-title">预约 {{ reserve.questionerRealName || reserve.questioner }}</h3>
            <div class="reserve-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                预约人：{{ reserve.questionerRealName || reserve.questioner }}
              </span>
              <span class="meta-item" v-if="reserve.phone">
                <el-icon><Phone /></el-icon>
                {{ reserve.phone }}
              </span>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(reserve.createTime) }}
              </span>
            </div>
          </div>
          <el-tag
            :type="getStatusType(reserve.status)"
            size="large"
          >
            {{ getStatusText(reserve.status) }}
          </el-tag>
        </div>

        <div class="reserve-details">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="label">农作物：</span>
              <span>{{ reserve.plantName }}</span>
            </div>
            <div class="detail-item">
              <span class="label">面积：</span>
              <span>{{ reserve.area }}</span>
            </div>
            <div class="detail-item">
              <span class="label">地址：</span>
              <span>{{ reserve.address }}</span>
            </div>
            <div class="detail-item full-width">
              <span class="label">土壤条件：</span>
              <span>{{ reserve.soilCondition }}</span>
            </div>
            <div class="detail-item full-width">
              <span class="label">作物条件：</span>
              <span>{{ reserve.plantCondition }}</span>
            </div>
            <div class="detail-item full-width">
              <span class="label">作物详情：</span>
              <span>{{ reserve.plantDetail }}</span>
            </div>
            <div class="detail-item full-width" v-if="reserve.message">
              <span class="label">留言：</span>
              <span>{{ reserve.message }}</span>
            </div>
            <div class="detail-item full-width" v-if="reserve.preferredTime">
              <span class="label">期望时间：</span>
              <span>{{ reserve.preferredTime }}</span>
            </div>
            <div class="detail-item full-width" v-if="reserve.answer && reserve.status === 1">
              <span class="label">我的回复：</span>
              <span class="answer-text">{{ reserve.answer }}</span>
            </div>
          </div>
        </div>

        <div class="reserve-actions">
          <template v-if="reserve.status === 0">
            <el-button type="success" @click="handleConfirm(reserve)">
              <el-icon><Select /></el-icon>
              确认预约
            </el-button>
            <el-button type="danger" @click="handleReject(reserve)">
              <el-icon><Close /></el-icon>
              拒绝预约
            </el-button>
          </template>
          <template v-else-if="reserve.status === 1">
            <el-button @click="handleViewAnswer(reserve)">
              <el-icon><View /></el-icon>
              查看回复
            </el-button>
          </template>
        </div>
      </div>

      <el-empty v-if="!loading && reserveList.length === 0" description="暂无预约记录" />
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadReservations"
        @size-change="loadReservations"
      />
    </div>

    <!-- 确认预约对话框 -->
    <el-dialog
      v-model="confirmDialogVisible"
      title="确认预约"
      width="500px"
    >
      <el-form :model="confirmForm" label-width="80px">
        <el-form-item label="预约人">
          <el-input :value="currentReserve?.questionerRealName || currentReserve?.questioner" disabled />
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="confirmForm.answer"
            type="textarea"
            :rows="4"
            placeholder="请输入您的回复内容（可选）"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDialogVisible = false">取消</el-button>
        <el-button type="success" @click="submitConfirm" :loading="submitting">
          确认完成
        </el-button>
      </template>
    </el-dialog>

    <!-- 拒绝预约对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝预约"
      width="500px"
    >
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="预约人">
          <el-input :value="currentReserve?.questionerRealName || currentReserve?.questioner" disabled />
        </el-form-item>
        <el-form-item label="拒绝原因">
          <el-input
            v-model="rejectForm.answer"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因（可选）"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="submitting">
          确认拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看回复对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="预约详情"
      width="600px"
    >
      <div class="reserve-detail-view" v-if="currentReserve">
        <h3>预约信息</h3>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="label">预约人：</span>
            <span>{{ currentReserve.questionerRealName || currentReserve.questioner }}</span>
          </div>
          <div class="detail-item">
            <span class="label">电话：</span>
            <span>{{ currentReserve.phone }}</span>
          </div>
          <div class="detail-item">
            <span class="label">农作物：</span>
            <span>{{ currentReserve.plantName }}</span>
          </div>
          <div class="detail-item">
            <span class="label">面积：</span>
            <span>{{ currentReserve.area }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">地址：</span>
            <span>{{ currentReserve.address }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">土壤条件：</span>
            <span>{{ currentReserve.soilCondition }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">作物条件：</span>
            <span>{{ currentReserve.plantCondition }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="label">作物详情：</span>
            <span>{{ currentReserve.plantDetail }}</span>
          </div>
          <div class="detail-item full-width" v-if="currentReserve.message">
            <span class="label">留言：</span>
            <span>{{ currentReserve.message }}</span>
          </div>
          <div class="detail-item full-width" v-if="currentReserve.preferredTime">
            <span class="label">期望时间：</span>
            <span>{{ currentReserve.preferredTime }}</span>
          </div>
        </div>
        <div v-if="currentReserve.answer" class="answer-section">
          <h4>我的回复：</h4>
          <p>{{ currentReserve.answer }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { User, Phone, Calendar, Select, Close, View } from '@element-plus/icons-vue'
import { getExpertReserveList, updateReserveStatus } from '@/api/expert'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const filterStatus = ref(-1)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const reserveList = ref([])

const confirmDialogVisible = ref(false)
const rejectDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const currentReserve = ref(null)

const confirmForm = reactive({
  reserveId: null,
  status: 1,
  answer: ''
})

const rejectForm = reactive({
  reserveId: null,
  status: 2,
  answer: ''
})

// 加载预约列表
const loadReservations = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }

    if (filterStatus.value !== -1) {
      params.status = filterStatus.value
    }

    const res = await getExpertReserveList(params)
    if (res.code === 200) {
      reserveList.value = (res.data.records || []).map(r => ({
        id: r.id,
        questioner: r.questioner,
        questionerRealName: r.questionerRealName,
        phone: r.phone,
        address: r.address,
        area: r.area,
        plantName: r.plantName,
        soilCondition: r.soilCondition,
        plantCondition: r.plantCondition,
        plantDetail: r.plantDetail,
        message: r.message,
        preferredTime: r.preferredTime,
        answer: r.answer,
        status: r.status,
        createTime: r.createTime
      }))
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 获取状态类型
const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const texts = { 0: '待处理', 1: '已完成', 2: '已拒绝', 3: '已过期' }
  return texts[status] || '未知'
}

// 确认预约
const handleConfirm = (reserve) => {
  currentReserve.value = reserve
  confirmForm.reserveId = reserve.id
  confirmForm.answer = ''
  confirmDialogVisible.value = true
}

// 拒绝预约
const handleReject = (reserve) => {
  currentReserve.value = reserve
  rejectForm.reserveId = reserve.id
  rejectForm.answer = ''
  rejectDialogVisible.value = true
}

// 提交确认
const submitConfirm = async () => {
  submitting.value = true
  try {
    await updateReserveStatus(confirmForm.reserveId, confirmForm.status, confirmForm.answer)
    ElMessage.success('已确认预约')
    confirmDialogVisible.value = false
    loadReservations()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// 提交拒绝
const submitReject = async () => {
  submitting.value = true
  try {
    await updateReserveStatus(rejectForm.reserveId, rejectForm.status, rejectForm.answer)
    ElMessage.success('已拒绝预约')
    rejectDialogVisible.value = false
    loadReservations()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// 查看回复
const handleViewAnswer = (reserve) => {
  currentReserve.value = reserve
  viewDialogVisible.value = true
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadReservations()
})
</script>

<style scoped>
.expert-reservations-page {
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

.filter-section {
  margin-bottom: var(--spacing-4, 16px);
}

.reservations-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
}

.reserve-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-5, 20px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

.reserve-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-4, 16px);
  padding-bottom: var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
}

.reserve-info {
  flex: 1;
}

.reserve-title {
  font-size: var(--font-size-xl, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.reserve-meta {
  display: flex;
  gap: var(--spacing-4, 16px);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.reserve-details {
  margin-bottom: var(--spacing-4, 16px);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-3, 12px);
}

.detail-item {
  display: flex;
  gap: var(--spacing-2, 8px);
  font-size: var(--font-size-base, 16px);
  color: var(--color-text-secondary, #4a5249);
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-item .label {
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-tertiary, #6b7280);
  min-width: 80px;
}

.answer-text {
  color: var(--color-success, #4a7c59);
  font-weight: var(--font-weight-medium, 500);
}

.reserve-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-8, 32px);
}

.reserve-detail-view h3 {
  font-size: var(--font-size-lg, 18px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-3, 12px);
}

.answer-section {
  margin-top: var(--spacing-4, 16px);
  padding-top: var(--spacing-4, 16px);
  border-top: 1px solid var(--color-divider, #edebe6);
}

.answer-section h4 {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-secondary, #4a5249);
  margin: 0 0 var(--spacing-2, 8px);
}

.answer-section p {
  font-size: var(--font-size-base, 16px);
  line-height: 1.6;
  color: var(--color-success, #4a7c59);
  margin: 0;
  white-space: pre-wrap;
}
</style>
