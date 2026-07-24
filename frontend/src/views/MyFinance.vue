<template>
  <div class="my-finance">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">我的融资</h1>
        <p class="page-subtitle">查看我提交的全部融资申请及审批进度</p>
      </div>
      <el-button type="primary" @click="$router.push('/farmer/finance')">
        <el-icon><Plus /></el-icon>
        申请新融资
      </el-button>
    </div>

    <!-- 我收到的联合贷款邀请：作为被邀请人同意/拒绝。
         邀请较多时可折叠，避免数据栏不断叠加占据过多界面空间。 -->
    <div v-if="receivedInvitations.length" class="content-card invitation-card">
      <div class="card-header">
        <h3 class="card-title">
          我收到的联合贷款邀请
          <el-tag v-if="pendingInviteCount > 0" type="danger" size="small" effect="dark" class="invite-count">
            {{ pendingInviteCount }} 条待处理
          </el-tag>
        </h3>
        <el-button link type="primary" size="small" @click="invitationCollapsed = !invitationCollapsed">
          {{ invitationCollapsed ? '展开' : '收起' }}
          <el-icon class="el-icon--right">
            <ArrowDown v-if="invitationCollapsed" /><ArrowUp v-else />
          </el-icon>
        </el-button>
      </div>
      <div v-show="!invitationCollapsed" class="invitation-list">
        <div v-for="inv in receivedInvitations" :key="inv.id" class="invitation-item">
          <div class="invitation-main">
            <div class="invitation-top">
              <span class="cp-name">{{ inv.applicantRealName || inv.applicantUserName }}</span>
              <el-tag :type="inviteStatusType(inv.status)" size="small">{{ inviteStatusText(inv.status) }}</el-tag>
            </div>
            <div class="invitation-meta">
              <span>产品：{{ inv.productName || '—' }}</span>
              <span>金额：¥ {{ formatNumber(inv.amount) }}</span>
              <span>时间：{{ formatTime(inv.createTime) }}</span>
            </div>
          </div>
          <div v-if="inv.status === 0" class="invitation-actions">
            <el-button type="primary" size="small" :loading="inviting" @click="onAcceptInvitation(inv)">同意</el-button>
            <el-button type="danger" plain size="small" :loading="inviting" @click="onDeclineInvitation(inv)">拒绝</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card warning">
        <div class="stat-icon"><el-icon><Clock /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待审核</div>
        </div>
      </div>
      <div class="stat-card success">
        <div class="stat-icon"><el-icon><Select /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.approved }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </div>
      <div class="stat-card error">
        <div class="stat-icon"><el-icon><Close /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.rejected }}</div>
          <div class="stat-label">已拒绝</div>
        </div>
      </div>
      <div class="stat-card primary">
        <div class="stat-icon"><el-icon><Document /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">累计申请</div>
        </div>
      </div>
    </div>

    <!-- 申请记录 -->
    <div v-loading="loading" class="content-card">
      <div class="card-header">
        <h3 class="card-title">申请记录</h3>
        <div class="filter-bar">
          <el-select
            v-model="filterStatus"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @change="onFilterChange"
          >
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
          </el-select>
          <el-button plain @click="resetFilter">重置</el-button>
        </div>
      </div>

      <div class="table-wrapper">
        <el-table :data="pagedList" stripe style="width: 100%">
          <el-table-column label="融资产品" width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ resolveProductName(row) }}</template>
          </el-table-column>
          <el-table-column label="申请金额" width="130" align="right">
            <template #default="{ row }">¥ {{ formatNumber(row.money) }}</template>
          </el-table-column>
          <el-table-column prop="purpose" label="融资用途" min-width="160" show-overflow-tooltip />
          <el-table-column label="还款期限" width="100" align="right">
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
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="viewDetail(row)">详情</el-button>
              <!-- 已被拒绝的申请允许删除；已通过的进入放款/还款流程不可删 -->
              <el-button v-if="row.status === 2" type="danger" link size="small" @click="deleteApplication(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无融资申请记录" />
          </template>
        </el-table>

        <div v-if="filteredList.length > pageSize" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="filteredList.length"
            layout="total, prev, pager, next"
          />
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="融资申请详情" width="680px">
      <el-descriptions v-if="currentDetail" :column="2" border>
        <el-descriptions-item label="申请人">{{ currentDetail.realName || currentDetail.ownName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentDetail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentDetail.idNum || '—' }}</el-descriptions-item>
        <el-descriptions-item label="融资产品">
          <span>{{ resolveProductName(currentDetail) }}</span>
          <div v-if="currentProduct?.introduce" class="product-introduce">{{ currentProduct.introduce }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="申请金额">¥ {{ formatNumber(currentDetail.money) }}</el-descriptions-item>
        <el-descriptions-item label="年利率">{{ currentDetail.rate != null ? currentDetail.rate + '%' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="还款期限">{{ currentDetail.repayment ? currentDetail.repayment + ' 个月' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="申请状态">
          <el-tag :type="statusType(currentDetail.status)" size="small">{{ statusText(currentDetail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="融资用途" :span="2">{{ currentDetail.purpose || '—' }}</el-descriptions-item>
        <el-descriptions-item label="还款来源" :span="2">{{ currentDetail.repaymentSource || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentInvitations.length" label="联合贷款人" :span="2">
          <div class="combination-block">
            <div v-for="inv in currentInvitations" :key="inv.id" class="combination-person">
              <span class="cp-label">联合人{{ inv.slot }}：</span>
              {{ inv.jointRealName || inv.jointUserName }} · {{ inv.jointPhone || '—' }}
              <el-tag size="small" :type="inviteStatusType(inv.status)" style="margin-left: 8px">
                {{ inviteStatusText(inv.status) }}
              </el-tag>
            </div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item v-else-if="hasCombination(currentDetail)" label="联合贷款人" :span="2">
          <div class="combination-block">
            <div v-if="currentDetail.combinationName1 || currentDetail.combinationPhone1 || currentDetail.combinationIdnum1" class="combination-person">
              <span class="cp-label">联合人1：</span>{{ currentDetail.combinationName1 || '—' }} · {{ currentDetail.combinationPhone1 || '—' }} · {{ currentDetail.combinationIdnum1 || '—' }}
            </div>
            <div v-if="currentDetail.combinationName2 || currentDetail.combinationPhone2 || currentDetail.combinationIdnum2" class="combination-person">
              <span class="cp-label">联合人2：</span>{{ currentDetail.combinationName2 || '—' }} · {{ currentDetail.combinationPhone2 || '—' }} · {{ currentDetail.combinationIdnum2 || '—' }}
            </div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.remark" label="审批备注" :span="2">{{ currentDetail.remark }}</el-descriptions-item>
      </el-descriptions>

      <!-- 还款计划：仅已通过融资展示，逐期可还款 -->
      <div v-if="currentDetail.status === 1" class="repayment-section">
        <h4 class="repayment-title">还款计划（等额本息）</h4>
        <el-table :data="currentRepayments" stripe size="small" style="width: 100%">
          <el-table-column label="期数" width="70" align="center">
            <template #default="{ row }">第 {{ row.periodIndex }} 期</template>
          </el-table-column>
          <el-table-column label="到期日" width="120">
            <template #default="{ row }">{{ row.dueDate || '—' }}</template>
          </el-table-column>
          <el-table-column label="本金" width="110" align="right">
            <template #default="{ row }">¥ {{ formatNumber(row.principal) }}</template>
          </el-table-column>
          <el-table-column label="利息" width="100" align="right">
            <template #default="{ row }">¥ {{ formatNumber(row.interest) }}</template>
          </el-table-column>
          <el-table-column label="本期合计" width="120" align="right">
            <template #default="{ row }">¥ {{ formatNumber(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="repayStatusType(row)" size="small">{{ repayStatusText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center">
            <template #default="{ row }">
              <template v-if="row.status === 0 || row.status === 3">
                <el-button type="primary" link size="small" @click="openRepayDialog(row)">
                  {{ row.status === 3 ? '重新还款' : '还款' }}
                </el-button>
                <div v-if="row.status === 3 && row.rejectReason" class="repay-reject" :title="row.rejectReason">
                  驳回：{{ row.rejectReason }}
                </div>
              </template>
              <span v-else-if="row.status === 2" class="repay-pending">审核中</span>
              <span v-else class="repay-done">{{ formatTime(row.paidTime) }}</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无还款计划" :image-size="60" />
          </template>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 还款提交对话框：填流水号 + 上传凭证 → 提交后进入「待确认」，等银行核验 -->
    <el-dialog v-model="repayDialogVisible" :title="`还款 · 第 ${repayForm.periodIndex} 期`" width="460px">
      <el-form label-width="84px">
        <el-form-item label="应还金额">
          <span class="repay-amount">¥ {{ formatNumber(repayForm.totalAmount) }}</span>
        </el-form-item>
        <el-form-item label="流水号/备注">
          <el-input
            v-model="repayForm.transactionNo"
            placeholder="如银行转账流水号 / 支付单号"
            maxlength="64"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="还款凭证">
          <el-upload
            action="/api/upload"
            :on-success="onProofUploaded"
            :on-remove="onProofRemoved"
            :before-upload="beforeProofUpload"
            :file-list="repayFileList"
            list-type="picture"
            accept="image/*"
            :limit="1"
            :on-exceed="onProofExceed"
          >
            <el-button type="primary" link>上传凭证图片</el-button>
            <template #tip>
              <div class="repay-tip">转账截图 / 回单，供银行核验（可选）</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repayDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="repaying" @click="doSubmitRepayment">提交还款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Clock, Select, Close, Document, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import {
  getMyFinanceApplications,
  getFinanceProducts,
  getFinanceJointInvitations,
  getFinanceRepayments,
  submitRepayment,
  cancelFinance,
  getMyJointInvitations,
  acceptJointInvitation,
  declineJointInvitation
} from '@/api/finance'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const detailVisible = ref(false)
const currentDetail = ref(null)
// 当前详情对应的联合贷款人邀请（按 slot 排序），用于展示各联合人确认状态
const currentInvitations = ref([])
// 当前详情对应的还款计划（仅 status==1 已通过融资有），含动态逾期判定
const currentRepayments = ref([])
const repaying = ref(false)

// 还款提交对话框：填流水号 + 上传凭证（status 0未还/3已驳回 可提交）
const repayDialogVisible = ref(false)
const repayForm = reactive({
  id: null,
  periodIndex: null,
  totalAmount: 0,
  transactionNo: '',
  payProof: ''
})
const repayFileList = ref([])

// 我收到的联合贷款邀请（作为被邀请人，本页为农户，需同意/拒绝）
const receivedInvitations = ref([])
const inviting = ref(false)
// 邀请较多时折叠展示，避免数据栏不断叠加占据过多界面空间
const invitationCollapsed = ref(false)
const pendingInviteCount = computed(() => receivedInvitations.value.filter(i => i.status === 0).length)

// 个人申请数量通常较少，一次性拉取后在前端做统计/筛选/分页
const allList = ref([])
const filterStatus = ref(undefined)
const page = ref(1)
const pageSize = 10

// 融资产品映射表（productId -> 产品对象），用于前端补全“融资产品”名称
const productMap = ref({})

const stats = reactive({
  pending: 0,
  approved: 0,
  rejected: 0,
  total: 0
})

// 按状态筛选（前端）
const filteredList = computed(() => {
  if (filterStatus.value === undefined || filterStatus.value === null) return allList.value
  return allList.value.filter(a => a.status === filterStatus.value)
})

// 前端分页
const pagedList = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})

onMounted(() => {
  loadApplications()
  loadProducts()
  loadReceivedInvitations()
})

// 拉取我收到的联合贷款邀请（同意/拒绝后刷新）
const loadReceivedInvitations = async () => {
  try {
    const res = await getMyJointInvitations()
    receivedInvitations.value = res.data || []
    // 待处理邀请 >2 条时默认折叠，避免占据过多界面
    invitationCollapsed.value = pendingInviteCount.value > 2
  } catch (e) {
    // 忽略
  }
}

const onAcceptInvitation = async (inv) => {
  // 同意前必须已填写身份证号（同意后姓名/电话/身份证号会绑定到融资申请；后端也会校验）
  // 缺失则不进入同意流程，引导去个人资料补填
  if (!userStore.userInfo?.identityNum) {
    try {
      await ElMessageBox.confirm(
        '同意联合贷款邀请前，请先在个人资料中填写您的身份证号（同意后将绑定到该融资申请）。是否现在前往填写？',
        '请先填写身份证号',
        { confirmButtonText: '去填写', cancelButtonText: '稍后', type: 'warning' }
      )
      router.push('/profile/edit')
    } catch (e) {
      // 用户选择"稍后"，不做任何操作
    }
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认作为 ${inv.applicantRealName || inv.applicantUserName} 的联合贷款人？同意后您的姓名/电话/身份证号将绑定到该融资申请。`,
      '同意邀请',
      { type: 'warning' }
    )
    inviting.value = true
    await acceptJointInvitation(inv.id)
    ElMessage.success('已同意')
    await loadReceivedInvitations()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    inviting.value = false
  }
}

const onDeclineInvitation = async (inv) => {
  try {
    await ElMessageBox.confirm('确定要拒绝该联合贷款邀请吗？', '拒绝邀请', { type: 'warning' })
    inviting.value = true
    await declineJointInvitation(inv.id)
    ElMessage.success('已拒绝')
    await loadReceivedInvitations()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    inviting.value = false
  }
}

// 拉取融资产品列表，建立 productId -> 产品 映射，便于前端补全产品名称
const loadProducts = async () => {
  try {
    const res = await getFinanceProducts({ page: 1, pageSize: 1000 })
    const records = res.data?.records || []
    const map = {}
    records.forEach(p => {
      if (p.productId != null) map[p.productId] = p
    })
    productMap.value = map
  } catch (e) {
    // 忽略：加载失败时仍可回退到后端回填的 productName
  }
}

// 解析融资产品名称：优先用后端回填的 productName，缺失则按 productId 在前端补全；
// 若该产品已被银行暂停供应(status===1)，追加提示（衔接银行"只能暂停不可删"的改动）
const resolveProductName = (row) => {
  if (!row) return '—'
  let name = row.productName
  if (!name) {
    const p = productMap.value[row.productId]
    name = p ? p.bankName : '—'
  }
  return row.productStatus === 1 ? `${name}（已暂停供应）` : name
}

// 当前详情对应的产品对象（用于展示产品介绍）
const currentProduct = computed(() => productMap.value[currentDetail.value?.productId] || null)

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getMyFinanceApplications({ page: 1, pageSize: 1000 })
    allList.value = res.data?.records || []
    computeStats()
  } catch (error) {
    ElMessage.error(error?.message || '加载融资申请失败')
  } finally {
    loading.value = false
  }
}

const computeStats = () => {
  const all = allList.value
  stats.pending = all.filter(a => a.status === 0).length
  stats.approved = all.filter(a => a.status === 1).length
  stats.rejected = all.filter(a => a.status === 2).length
  stats.total = all.length
}

const onFilterChange = () => {
  page.value = 1
}

const resetFilter = () => {
  filterStatus.value = undefined
  page.value = 1
}

const viewDetail = async (row) => {
  currentDetail.value = row
  currentInvitations.value = []
  currentRepayments.value = []
  detailVisible.value = true
  // 拉取该申请下的联合贷款人邀请，展示各联合人确认状态（待确认/已同意/已拒绝）
  try {
    const res = await getFinanceJointInvitations(row.financeId)
    currentInvitations.value = res.data || []
  } catch (e) {
    // 忽略：老申请可能没有邀请记录
  }
  // 已通过融资：拉取等额本息还款计划，展示各期到期日/本金/利息/总额/状态
  if (row.status === 1) {
    try {
      const res = await getFinanceRepayments(row.financeId)
      currentRepayments.value = res.data || []
    } catch (e) {
      // 忽略：早期通过但未生成计划的记录
    }
  }
}

// 删除已被拒绝的申请（status=2）；后端仅允许删除 申请中(0)/已拒绝(2)，已通过(1)不可删
const deleteApplication = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条已拒绝的申请吗？删除后不可恢复。', '删除确认', { type: 'warning' })
    await cancelFinance(row.financeId)
    ElMessage.success('已删除')
    await loadApplications()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')
  }
}

// 打开还款对话框（status 0未还 / 3已驳回 可提交）
const openRepayDialog = (rp) => {
  repayForm.id = rp.id
  repayForm.periodIndex = rp.periodIndex
  repayForm.totalAmount = rp.totalAmount
  // 重新提交(3)时清空上次凭证；首次还款(0)自然为空
  repayForm.transactionNo = ''
  repayForm.payProof = ''
  repayFileList.value = []
  repayDialogVisible.value = true
}

// 凭证上传成功：/upload 返回 { url, filename }
const onProofUploaded = (res, file) => {
  if (res && res.data && res.data.url) {
    repayForm.payProof = res.data.url
    repayFileList.value = [{ name: file.name, url: res.data.url }]
  }
}
const onProofRemoved = () => {
  repayForm.payProof = ''
  repayFileList.value = []
}
const onProofExceed = () => {
  ElMessage.warning('凭证最多上传 1 张，请先删除再重新上传')
}
const beforeProofUpload = (file) => {
  const isImg = file.type.startsWith('image/')
  const under5M = file.size / 1024 / 1024 < 5
  if (!isImg) {
    ElMessage.error('凭证仅支持图片')
    return false
  }
  if (!under5M) {
    ElMessage.error('图片不能超过 5MB')
    return false
  }
  return true
}

// 提交还款：填流水号 + 凭证 → 进入「待确认」，等待银行核验
const doSubmitRepayment = async () => {
  repaying.value = true
  try {
    await submitRepayment(repayForm.id, {
      transactionNo: repayForm.transactionNo.trim(),
      payProof: repayForm.payProof
    })
    ElMessage.success('还款已提交，等待银行确认')
    repayDialogVisible.value = false
    const res = await getFinanceRepayments(currentDetail.value.financeId)
    currentRepayments.value = res.data || []
  } catch (e) {
    ElMessage.error(e?.message || '提交还款失败')
  } finally {
    repaying.value = false
  }
}

// 还款状态文案/类型：0未还(逾期则危险)、1已还(成功)、2待确认(主色)、3已驳回(危险)
const repayStatusType = (rp) => {
  if (rp.status === 1) return 'success'
  if (rp.status === 2) return 'primary'
  if (rp.status === 3) return 'danger'
  return rp.overdue ? 'danger' : 'warning'
}
const repayStatusText = (rp) => {
  if (rp.status === 1) return '已还款'
  if (rp.status === 2) return '待确认'
  if (rp.status === 3) return '已驳回'
  return rp.overdue ? '已逾期' : '待还款'
}

// 是否含联合贷款人信息（任一字段非空即认为填写了）
const hasCombination = (d) => !!(d && (d.combinationName1 || d.combinationPhone1 || d.combinationIdnum1 || d.combinationName2 || d.combinationPhone2 || d.combinationIdnum2))

const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')
const statusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' }[s] || '未知')

// 联合贷款人邀请状态
const inviteStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')
const inviteStatusText = (s) => ({ 0: '待确认', 1: '已同意', 2: '已拒绝' }[s] || '未知')

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
.my-finance {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6, 24px);
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-4, 16px);
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
.stat-card.primary .stat-icon { background: rgba(74, 124, 89, 0.1); color: var(--color-farmer, #4a7c59); }
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
}
.product-introduce {
  margin-top: var(--spacing-1, 4px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  line-height: 1.5;
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
.repayment-section {
  margin-top: var(--spacing-5, 20px);
}
.repayment-title {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-3, 12px);
}
.repay-done {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-text-tertiary, #6b7280);
}
.repay-pending {
  font-size: var(--font-size-xs, 12px);
  color: var(--color-primary, #2d5a3d);
}
.repay-reject {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-error, #b85c38);
  line-height: 1.3;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.repay-amount {
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-bank, #2d5a3d);
}
.repay-tip {
  font-size: 12px;
  color: var(--color-text-tertiary, #6b7280);
  line-height: 1.4;
}

/* 我收到的联合贷款邀请 */
.invitation-card .invitation-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-4, 16px);
  /* 邀请较多时限制高度并内部滚动，避免占据过多界面 */
  max-height: 360px;
  overflow-y: auto;
}
.invite-count {
  margin-left: var(--spacing-2, 8px);
  vertical-align: middle;
}
.invitation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-3, 12px);
  padding: var(--spacing-3, 12px) var(--spacing-4, 16px);
  border: 1px solid var(--color-border, #e5e0d8);
  border-radius: var(--radius-base, 8px);
  background: var(--color-bg-secondary, #faf8f4);
  flex-wrap: wrap;
}
.invitation-top {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
  margin-bottom: 6px;
}
.cp-name {
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
}
.invitation-meta {
  display: flex;
  gap: var(--spacing-4, 16px);
  flex-wrap: wrap;
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-secondary, #4a5249);
}
.invitation-actions {
  display: flex;
  gap: var(--spacing-2, 8px);
}
.table-wrapper {
  padding: var(--spacing-4, 16px);
}
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--spacing-3, 12px);
}

@media (max-width: 1024px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .stats-grid { grid-template-columns: 1fr; }
  .page-header { flex-direction: column; align-items: stretch; }
}
</style>
