<template>
  <div class="reserve-expert-page">
    <div class="container">
      <BackBar to="/expert-help" />
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">预约专家</h1>
        <p class="page-subtitle">与农业专家一对一咨询，获取专业指导</p>
      </div>

      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" class="expert-tabs">
        <!-- 专家列表 -->
        <el-tab-pane label="专家列表" name="experts">
          <div v-loading="loadingExperts" class="experts-section">
            <!-- 专家列表 -->
            <div v-if="experts.length > 0" class="experts-grid">
              <div
                v-for="expert in experts"
                :key="expert.userName"
                class="expert-card"
              >
                <div class="expert-avatar">
                  <el-avatar :size="80" :src="expert.avatar">
                    {{ expert.realName?.charAt(0) }}
                  </el-avatar>
                </div>
                <div class="expert-info">
                  <h3 class="expert-name">{{ expert.realName }}</h3>
                  <div class="expert-title">{{ expert.profession || '农业专家' }}</div>
                  <div class="expert-desc">{{ expert.position || '' }}{{ expert.belong ? ' · ' + expert.belong : '' }}</div>
                  <div class="expert-stats">
                    <span>
                      <el-icon><Phone /></el-icon>
                      {{ expert.phone || '—' }}
                    </span>
                  </div>
                </div>
                <div class="expert-actions">
                  <el-button
                    v-if="canConsultEntry"
                    type="primary"
                    @click="handleReserve(expert)"
                    :disabled="!userStore.isLoggedIn()"
                  >
                    立即预约
                  </el-button>
                  <span v-else class="consult-disabled-tip">仅农户/买家可预约</span>
                  <el-button @click="handleViewDetail(expert)">
                    查看详情
                  </el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无专家信息" />
          </div>
        </el-tab-pane>

        <!-- 我的预约 -->
        <el-tab-pane label="我的预约" name="my-reserves">
          <div v-loading="loadingReserves" class="reserves-section">
            <div v-if="userStore.isLoggedIn()">
              <div v-if="myReserves.length > 0" class="reserves-list">
                <div
                  v-for="reserve in myReserves"
                  :key="reserve.id"
                  class="reserve-card"
                >
                  <div class="reserve-header">
                    <div class="expert-mini">
                      <el-avatar :size="40" :src="reserve.expertAvatar">
                        {{ (reserve.expertRealName || reserve.expertName)?.charAt(0) }}
                      </el-avatar>
                      <span class="expert-name">{{ reserve.expertRealName || reserve.expertName }}</span>
                    </div>
                    <div
                      class="status-badge"
                      :class="reserve.status === 1 ? 'status-completed' : (reserve.status === 2 ? 'status-rejected' : 'status-pending')"
                    >
                      {{ getReserveStatusText(reserve.status) }}
                    </div>
                  </div>
                  <div class="reserve-info">
                    <div class="info-item">
                      <el-icon><Calendar /></el-icon>
                      作物：{{ reserve.plantName }}
                    </div>
                    <div class="info-item">
                      <el-icon><Document /></el-icon>
                      面积：{{ reserve.area }}
                    </div>
                    <div class="info-item">
                      <el-icon><Location /></el-icon>
                      地址：{{ reserve.address }}
                    </div>
                    <div class="info-item" v-if="reserve.preferredTime">
                      <el-icon><Calendar /></el-icon>
                      期望时间：{{ reserve.preferredTime }}
                    </div>
                    <div class="info-item" v-if="reserve.message">
                      <el-icon><ChatDotRound /></el-icon>
                      留言：{{ reserve.message }}
                    </div>
                    <div class="info-item" v-if="reserve.answer && reserve.status === 1">
                      <el-icon><Select /></el-icon>
                      专家回复：{{ reserve.answer }}
                    </div>
                  </div>
                  <div class="reserve-actions">
                    <el-button
                      v-if="reserve.status === 0"
                      type="danger"
                      size="small"
                      @click="handleCancelReserve(reserve)"
                    >
                      取消预约
                    </el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="您还没有预约记录，快去预约专家吧！" />
            </div>
            <div v-else class="login-prompt">
              <el-empty description="请先登录后查看预约记录">
                <el-button type="primary" @click="handleLogin">立即登录</el-button>
              </el-empty>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 预约对话框 -->
    <el-dialog
      v-model="reserveDialogVisible"
      title="预约专家"
      width="500px"
      :before-close="handleCloseDialog"
    >
      <el-form
        ref="reserveFormRef"
        :model="reserveForm"
        :rules="reserveFormRules"
        label-width="100px"
      >
        <el-form-item label="专家姓名">
          <el-input v-model="selectedExpert.realName" disabled />
        </el-form-item>
        <el-form-item label="农作物" prop="plantName">
          <el-input
            v-model="reserveForm.plantName"
            placeholder="请输入农作物名称"
          />
        </el-form-item>
        <el-form-item label="种植面积" prop="area">
          <el-input
            v-model="reserveForm.area"
            placeholder="请输入种植面积"
          >
            <template #suffix>亩</template>
          </el-input>
        </el-form-item>
        <el-form-item label="所在地区" prop="region">
          <el-cascader
            v-model="reserveForm.region"
            :options="regionOptions"
            :props="{ expandTrigger: 'hover', value: 'label', label: 'label' }"
            placeholder="请选择 省 / 市 / 县"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="addressDetail">
          <el-input
            v-model="reserveForm.addressDetail"
            placeholder="请输入街道、村组、门牌等详细地址"
          />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input
            v-model="reserveForm.phone"
            placeholder="请输入联系电话"
          />
        </el-form-item>
        <el-form-item label="期望时间">
          <el-date-picker
            v-model="reserveForm.preferredTime"
            type="datetime"
            placeholder="选择期望的咨询时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            :disabled-date="disabledPastDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="土壤条件" prop="soilCondition">
          <el-input
            v-model="reserveForm.soilCondition"
            type="textarea"
            :rows="2"
            placeholder="请描述土壤条件（如：沙壤土、肥沃等）"
          />
        </el-form-item>
        <el-form-item label="作物条件" prop="plantCondition">
          <el-input
            v-model="reserveForm.plantCondition"
            type="textarea"
            :rows="2"
            placeholder="请描述作物条件（如：生长阶段、长势等）"
          />
        </el-form-item>
        <el-form-item label="作物详情" prop="plantDetail">
          <el-input
            v-model="reserveForm.plantDetail"
            type="textarea"
            :rows="3"
            placeholder="请详细描述作物情况"
          />
        </el-form-item>
        <el-form-item label="留言" prop="message">
          <el-input
            v-model="reserveForm.message"
            type="textarea"
            :rows="2"
            placeholder="其他留言信息（可选）"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCloseDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmitReserve" :loading="submitting">
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Calendar,
  Location,
  Select,
  Phone
} from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { getExpertList } from '@/api/expert'
import { makeReservation, getMyReserves, cancelReserve } from '@/api/reserve'
import { regionData } from 'element-china-area-data'
import BackBar from '@/components/BackBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 仅农户/买家可预约专家；未登录时展示按钮（禁用）引导登录
const canConsultEntry = computed(() => !userStore.isLoggedIn() || ['farmer', 'buyer'].includes(userStore.role))

const activeTab = ref('experts')
const loadingExperts = ref(false)
const loadingReserves = ref(false)

const experts = ref([])
const myReserves = ref([])
const submitting = ref(false)
const reserveDialogVisible = ref(false)
const selectedExpert = ref({})
const reserveFormRef = ref(null)

const reserveForm = reactive({
  expertName: '',
  plantName: '',
  area: '',
  region: [],          // 省/市/县（label 数组）
  addressDetail: '',   // 详细地址（街道、村组、门牌等）
  phone: '',
  preferredTime: '',
  soilCondition: '',
  plantCondition: '',
  plantDetail: '',
  message: ''
})

const reserveFormRules = {
  plantName: [
    { required: true, message: '请输入农作物名称', trigger: 'blur' }
  ],
  area: [
    { required: true, message: '请输入种植面积', trigger: 'blur' }
  ],
  region: [
    // el-cascader v-model 为数组；空数组 [] 是 truthy，须用 type:'array' 才能校验"未选"
    { type: 'array', required: true, message: '请选择所在地区', trigger: 'change' }
  ],
  addressDetail: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  soilCondition: [
    { required: true, message: '请描述土壤条件', trigger: 'blur' }
  ],
  plantCondition: [
    { required: true, message: '请描述作物条件', trigger: 'blur' }
  ],
  plantDetail: [
    { required: true, message: '请详细描述作物情况', trigger: 'blur' },
    { min: 10, message: '作物详情至少10个字符', trigger: 'blur' }
  ]
}

// 省市县三级下拉数据（element-china-area-data）
const regionOptions = regionData

// 期望时间不可选当前日期之前（今天可选）
const disabledPastDate = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

// 获取预约状态文本 - 使用整数状态码与后端保持一致
const getReserveStatusText = (status) => {
  const map = {
    0: '待处理',
    1: '已完成',
    2: '已拒绝'
  }
  return map[status] || '未知'
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 登录
const handleLogin = () => {
  router.push({ path: '/login', query: { redirect: '/reserve' } })
}

// 查看专家详情
const handleViewDetail = (expert) => {
  router.push({ path: `/expert/${expert.userName}`, query: { from: route.fullPath } })
}

// 预约专家
const handleReserve = (expert) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录后预约')
    router.push({ path: '/login', query: { redirect: `/reserve?expert=${expert.userName}` } })
    return
  }
  if (!['farmer', 'buyer'].includes(userStore.role)) {
    ElMessage.info('仅农户与买家可预约专家')
    return
  }

  selectedExpert.value = expert
  // expert_name 存储专家 userName
  reserveForm.expertName = expert.userName
  reserveForm.plantName = ''
  reserveForm.area = ''
  reserveForm.region = []                // 省市县三级下拉（label 数组）
  reserveForm.addressDetail = ''         // 详细地址
  reserveForm.phone = userStore.userInfo?.phone || ''
  reserveForm.preferredTime = ''
  reserveForm.soilCondition = ''
  reserveForm.plantCondition = ''
  reserveForm.plantDetail = ''
  reserveForm.message = ''
  reserveDialogVisible.value = true
}

// 关闭对话框
const handleCloseDialog = () => {
  reserveDialogVisible.value = false
  reserveFormRef.value?.resetFields()
}

// 提交预约
const handleSubmitReserve = async () => {
  try {
    await reserveFormRef.value.validate()

    submitting.value = true

    await makeReservation({
      expertName: reserveForm.expertName,
      plantName: reserveForm.plantName,
      area: reserveForm.area,
      address: [...(reserveForm.region || []), (reserveForm.addressDetail || '').trim()]
        .filter(Boolean).join(''),
      phone: reserveForm.phone,
      preferredTime: reserveForm.preferredTime,
      soilCondition: reserveForm.soilCondition,
      plantCondition: reserveForm.plantCondition,
      plantDetail: reserveForm.plantDetail,
      message: reserveForm.message
    })

    ElMessage.success('预约成功，请等待专家处理')
    reserveDialogVisible.value = false
    activeTab.value = 'my-reserves'
    await loadMyReserves()
  } catch (error) {
    if (error !== false) {
      ElMessage.error(error.message || '预约失败')
    }
  } finally {
    submitting.value = false
  }
}

// 取消预约
const handleCancelReserve = async (reserve) => {
  try {
    await ElMessageBox.confirm('确定要取消这个预约吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelReserve(reserve.id)
    ElMessage.success('已取消预约')
    await loadMyReserves()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}

// 加载专家列表
const loadExperts = async () => {
  loadingExperts.value = true
  try {
    const res = await getExpertList()
    experts.value = res.data || []
  } catch (error) {
    ElMessage.error('加载专家列表失败')
  } finally {
    loadingExperts.value = false
  }
}

// 加载我的预约
const loadMyReserves = async () => {
  if (!userStore.isLoggedIn()) return

  loadingReserves.value = true
  try {
    const res = await getMyReserves()
    myReserves.value = res.data?.records || []
  } catch (error) {
    ElMessage.error('加载预约记录失败')
  } finally {
    loadingReserves.value = false
  }
}

// 监听Tab切换
watch(activeTab, (newVal) => {
  if (newVal === 'my-reserves' && userStore.isLoggedIn()) {
    loadMyReserves()
  }
})

// 从路由参数预选专家并打开预约对话框
const handleExpertQuery = (userName) => {
  const expert = experts.value.find(e => e.userName === userName)
  if (expert) {
    handleReserve(expert)
  }
}

onMounted(async () => {
  await loadExperts()
  if (userStore.isLoggedIn()) {
    loadMyReserves()
    if (route.query.expert) {
      handleExpertQuery(String(route.query.expert))
    }
  } else if (route.query.expert) {
    ElMessage.info('请先登录后预约')
    router.push({ path: '/login', query: { redirect: `/reserve?expert=${route.query.expert}` } })
  }
})
</script>

<style scoped>
.reserve-expert-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f5f3ef);
  padding: var(--spacing-6, 24px) 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--spacing-6, 24px);
}

.page-header {
  text-align: center;
  margin-bottom: var(--spacing-6, 24px);
}

.page-title {
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: var(--font-size-4xl, 48px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-2, 8px);
}

.page-subtitle {
  font-size: var(--font-size-lg, 20px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0;
}

.expert-tabs {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-6, 24px);
  box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,0.05));
}

/* ===== 筛选区 ===== */
.filter-section {
  margin-bottom: var(--spacing-6, 24px);
}

.filter-tabs {
  display: flex;
  gap: var(--spacing-2, 8px);
  flex-wrap: wrap;
}

.filter-tab {
  padding: var(--spacing-2, 8px) var(--spacing-4, 16px);
  font-size: var(--font-size-sm, 14px);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text-secondary, #4a5249);
  background: transparent;
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-full, 9999px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.filter-tab:hover {
  color: var(--color-primary, #2d5a3d);
  border-color: var(--color-primary, #2d5a3d);
}

.filter-tab.active {
  color: var(--color-text-inverse, #ffffff);
  background: var(--color-primary, #2d5a3d);
  border-color: var(--color-primary, #2d5a3d);
}

/* ===== 专家列表 ===== */
.experts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-5, 20px);
}

.expert-card {
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-lg, 12px);
  padding: var(--spacing-5, 20px);
  text-align: center;
  transition: all var(--transition-fast, 150ms ease);
}

.expert-card:hover {
  box-shadow: var(--shadow-base, 0 4px 12px rgba(0,0,0,0.1));
}

.expert-avatar {
  position: relative;
  display: inline-block;
  margin-bottom: var(--spacing-3, 12px);
}

.expert-status {
  position: absolute;
  bottom: 0;
  right: 0;
  padding: var(--spacing-1, 4px) var(--spacing-2, 8px);
  font-size: var(--font-size-xs, 10px);
  color: var(--color-text-inverse, #ffffff);
  background: #67c23a;
  border-radius: var(--radius-full, 9999px);
}

.expert-name {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-1, 4px);
}

.expert-title {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-primary, #2d5a3d);
  margin-bottom: var(--spacing-3, 12px);
}

.expert-desc {
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
  line-height: 1.6;
  margin-bottom: var(--spacing-3, 12px);
  min-height: 60px;
}

.expert-stats {
  display: flex;
  justify-content: center;
  gap: var(--spacing-4, 16px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  margin-bottom: var(--spacing-4, 16px);
}

.expert-stats span {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
}

.expert-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
  justify-content: center;
  align-items: center;
}

.consult-disabled-tip {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
  white-space: nowrap;
}

/* ===== 预约列表 ===== */
.reserves-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 16px);
}

.reserve-card {
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
  padding: var(--spacing-4, 16px);
}

.reserve-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-3, 12px);
}

.expert-mini {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
}

.expert-mini .expert-name {
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
}

.status-badge {
  padding: var(--spacing-1, 4px) var(--spacing-3, 12px);
  font-size: var(--font-size-xs, 12px);
  font-weight: var(--font-weight-medium, 500);
  border-radius: var(--radius-full, 9999px);
}

.status-pending {
  color: #e6a23c;
  background: #fdf6ec;
}

.status-confirmed {
  color: #67c23a;
  background: #f0f9ff;
}

.status-rejected {
  color: #f56c6c;
  background: #fef0f0;
}

.status-completed {
  color: #909399;
  background: #f4f4f5;
}

.status-cancelled {
  color: #909399;
  background: #f4f4f5;
}

.reserve-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2, 8px);
  margin-bottom: var(--spacing-3, 12px);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-1, 4px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}

.info-item a {
  color: var(--color-primary, #2d5a3d);
  text-decoration: none;
}

.reserve-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .experts-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .experts-grid {
    grid-template-columns: 1fr;
  }

  .filter-tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .page-title {
    font-size: var(--font-size-3xl, 38px);
  }

  .expert-tabs {
    padding: var(--spacing-4, 16px);
  }
}
</style>
