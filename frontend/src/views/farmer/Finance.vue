<template>
  <div class="finance-page">
    <div class="page-header">
      <h1 class="page-title">融资申请</h1>
      <el-button type="primary" @click="showApplyDialog">
        <el-icon><Plus /></el-icon>
        申请融资
      </el-button>
    </div>

    <!-- 融资产品推荐 -->
    <el-card class="products-card" v-if="financeProducts.length > 0">
      <template #header>
        <span>推荐融资产品</span>
      </template>
      <div class="products-grid">
        <div v-for="product in financeProducts" :key="product.productId" class="product-item">
          <div class="product-name">{{ product.productName || product.bankName }}</div>
          <div class="product-bank-row">{{ product.bankName }}</div>
          <div class="product-desc">{{ product.introduce }}</div>
          <div class="product-terms">
            <div class="term">
              <span class="label">年利率：</span>
              <span class="value">{{ product.rate }}%</span>
            </div>
            <div class="term">
              <span class="label">期限：</span>
              <span class="value">{{ product.repayment }}个月</span>
            </div>
            <div class="term">
              <span class="label">额度：</span>
              <span class="value">¥{{ product.money }}</span>
            </div>
          </div>
          <el-button type="primary" size="small" @click="applyForProduct(product)">
            立即申请
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 我的融资申请 -->
    <el-card class="applications-card" v-loading="loading">
      <template #header>
        <span>我的融资申请</span>
      </template>
      <el-table :data="applications" style="width: 100%">
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="money" label="申请金额(元)" width="140">
          <template #default="{ row }">
            ¥{{ row.money }}
          </template>
        </el-table-column>
        <el-table-column prop="repayment" label="期限(月)" width="100" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="purpose" label="申请原因" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="150">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              link
              type="primary"
              @click="editApplication(row)"
            >
              修改
            </el-button>
            <el-button
              v-if="row.status === 0"
              link
              type="danger"
              @click="cancelApplication(row)"
            >
              撤销
            </el-button>
            <el-button link type="primary" @click="viewDetail(row)">
              详情
            </el-button>
            <!-- 已被拒绝的申请允许删除（清理记录）；已通过的进入放款/还款流程不可删 -->
            <el-button
              v-if="row.status === 2"
              link
              type="danger"
              @click="deleteApplication(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadApplications"
          @current-change="loadApplications"
        />
      </div>

      <el-empty v-if="!loading && applications.length === 0" description="暂无融资申请" />
    </el-card>

    <!-- 申请融资对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="申请融资"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="融资产品" prop="productId">
          <el-select v-model="form.productId" placeholder="请选择融资产品" style="width: 100%">
            <el-option
              v-for="product in financeProducts"
              :key="product.productId"
              :label="`${product.productName || product.bankName}${product.bankName ? ' · ' + product.bankName : ''}`"
              :value="product.productId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idNum">
          <el-input v-model="form.idNum" placeholder="请输入身份证号" maxlength="18" />
        </el-form-item>
        <el-form-item label="申请金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="1"
            :max="500"
            :step="1"
            :precision="2"
            style="width: 100%"
          />
          <span class="unit-text">万元</span>
        </el-form-item>
        <el-form-item label="申请原因" prop="purpose">
          <el-input
            v-model="form.purpose"
            type="textarea"
            :rows="3"
            placeholder="请说明融资用途，如：购买农机具、建设大棚、采购农资等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="还款来源" prop="repaymentSource">
          <el-input
            v-model="form.repaymentSource"
            type="textarea"
            :rows="3"
            placeholder="请说明还款来源，如：农产品销售收入、经营收入等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-divider content-position="left">联合贷款人（选填）</el-divider>
        <div class="combination-tip">
          选择<b>农户</b>联系人作为联合贷款人，提交后对方会收到邀请通知，<b>对方同意后</b>其姓名/电话/身份证号将自动绑定到本申请。最多 2 人，有助于提升审批通过率。
        </div>
        <el-form-item label="联合人1">
          <el-select
            v-model="form.jointUserName1"
            filterable
            clearable
            placeholder="选择联系人（姓名 + 电话）"
            style="width: 100%"
          >
            <el-option
              v-for="c in contactOptions"
              :key="c.userName"
              :label="`${c.realName || c.userName}（${c.phone || '无电话'}）`"
              :value="c.userName"
              :disabled="c.userName === form.jointUserName2"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="联合人2">
          <el-select
            v-model="form.jointUserName2"
            filterable
            clearable
            placeholder="选择联系人（姓名 + 电话）"
            style="width: 100%"
          >
            <el-option
              v-for="c in contactOptions"
              :key="c.userName"
              :label="`${c.realName || c.userName}（${c.phone || '无电话'}）`"
              :value="c.userName"
              :disabled="c.userName === form.jointUserName1"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApplication" :loading="submitting">
          提交申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框（与"我的融资"详情一致） -->
    <el-dialog v-model="detailVisible" title="融资申请详情" width="680px">
      <el-descriptions v-if="currentDetail" :column="2" border>
        <el-descriptions-item label="申请人">{{ currentDetail.realName || currentDetail.ownName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentDetail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentDetail.idNum || '—' }}</el-descriptions-item>
        <el-descriptions-item label="融资产品">
          <span>{{ resolveProductName(currentDetail) }}</span>
          <!-- 该申请对应产品若已被银行暂停供应，给出提示（衔接银行"只能暂停不可删"的改动） -->
          <el-tag v-if="currentDetail.productStatus === 1" type="warning" size="small" style="margin-left: 8px">已暂停供应</el-tag>
          <div v-if="currentProduct?.introduce" class="product-introduce">{{ currentProduct.introduce }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="申请金额">¥ {{ formatNumber(currentDetail.money) }}</el-descriptions-item>
        <el-descriptions-item label="年利率">{{ currentDetail.rate != null ? currentDetail.rate + '%' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="还款期限">{{ currentDetail.repayment ? currentDetail.repayment + ' 个月' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="申请状态">
          <el-tag :type="getStatusType(currentDetail.status)" size="small">{{ getStatusText(currentDetail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="融资用途" :span="2">{{ currentDetail.purpose || '—' }}</el-descriptions-item>
        <el-descriptions-item label="还款来源" :span="2">{{ currentDetail.repaymentSource || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentInvitations.length" label="联合贷款人" :span="2">
          <div class="combination-block">
            <div v-for="inv in currentInvitations" :key="inv.id" class="combination-person">
              <span class="cp-label">联合人{{ inv.slot }}：</span>
              {{ inv.jointRealName || inv.jointUserName }} · {{ inv.jointPhone || '—' }}
              <el-tag size="small" :type="inviteStatusType(inv.status)" style="margin-left: 8px">{{ inviteStatusText(inv.status) }}</el-tag>
            </div>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ formatDetailTime(currentDetail.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentDetail.remark" label="审批备注" :span="2">{{ currentDetail.remark }}</el-descriptions-item>
      </el-descriptions>

      <!-- 还款计划：仅已通过融资展示 -->
      <div v-if="currentDetail?.status === 1" class="repayment-section">
        <h4 class="repayment-title">还款计划（等额本息）</h4>
        <el-table :data="currentRepayments" stripe size="small" style="width: 100%">
          <el-table-column label="期数" width="80" align="center">
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
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">{{ repayStatusText(row) }}</template>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getFinanceProducts, applyFinance, cancelFinance, getMyFinanceApplications, getFinanceProductDetail, getFinanceJointInvitations, getFinanceRepayments } from '@/api/finance'
import { getContacts } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const financeProducts = ref([])
const applications = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const formRef = ref(null)
const editingApplicationId = ref(null)

// 详情对话框（与"我的融资"详情一致）
const detailVisible = ref(false)
const currentDetail = ref(null)
const currentProduct = ref(null)      // 详情对应融资产品（用于展示介绍；推荐列表只含在售产品，暂停产品需单独拉取）
const currentInvitations = ref([])    // 详情对应的联合贷款人邀请
const currentRepayments = ref([])     // 已通过融资的还款计划

// 联系人列表（农户/买家/专家，排除自己），用于选择联合贷款人
const contactOptions = ref([])

// 申请表单：字段与后端 FinanceRequest / tb_finance 对齐
// amount 为页面输入(万元)，提交时换算为 money(元)
const form = reactive({
  productId: null,
  realName: '',
  phone: '',
  idNum: '',
  amount: 10,
  purpose: '',
  repaymentSource: '',
  // 联合贷款人（选填，最多 2 人）—— 选择联系人(userName)，提交后由后端创建邀请，对方同意后回填资料
  jointUserName1: '',
  jointUserName2: ''
})

const rules = {
  productId: [{ required: true, message: '请选择融资产品', trigger: 'change' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  idNum: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入申请金额', trigger: 'blur' }],
  purpose: [{ required: true, message: '请输入申请原因', trigger: 'blur' }],
  repaymentSource: [{ required: true, message: '请输入还款来源', trigger: 'blur' }]
}

// 加载融资产品
const loadFinanceProducts = async () => {
  try {
    const res = await getFinanceProducts({ page: 1, pageSize: 10 })
    if (res.code === 200) {
      financeProducts.value = res.data.records || []
    }
  } catch (error) {
    ElMessage.error('加载融资产品失败')
  }
}

// 加载我的融资申请
const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getMyFinanceApplications({
      page: currentPage.value,
      pageSize: pageSize.value
    })
    if (res.code === 200) {
      applications.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('加载融资申请失败')
  } finally {
    loading.value = false
  }
}

// 加载联系人列表（供联合贷款人选择；联合贷款人只能选农户）
const loadContacts = async () => {
  try {
    const res = await getContacts({ role: 'farmer' })
    contactOptions.value = res.data || []
  } catch (e) {
    // 忽略：加载失败只是没有联系人可选
  }
}

// 打开表单时用个人资料预填 申请人 姓名/电话/身份证号（仅空值时填，不覆盖已输入）
const ensureApplicantDefaults = () => {
  const info = userStore.userInfo
  if (!info) return
  if (!form.realName && info.realName) form.realName = info.realName
  if (!form.phone && info.phone) form.phone = info.phone
  if (!form.idNum && info.identityNum) form.idNum = info.identityNum
}

// 显示申请对话框
const showApplyDialog = () => {
  ensureApplicantDefaults()
  dialogVisible.value = true
}

// 针对产品申请
const applyForProduct = (product) => {
  form.productId = product.productId
  ensureApplicantDefaults()
  dialogVisible.value = true
}

// 编辑申请
const editApplication = (application) => {
  editingApplicationId.value = application.financeId || application.id
  Object.assign(form, {
    productId: application.productId,
    realName: application.realName || '',
    phone: application.phone || '',
    idNum: application.idNum || '',
    amount: application.money ? application.money / 10000 : 10, // 元换算回万元
    purpose: application.purpose || '',
    repaymentSource: application.repaymentSource || '',
    // 编辑时不回填联合人（联合人由邀请流程管理，不在表单里直接改）
    jointUserName1: '',
    jointUserName2: ''
  })
  dialogVisible.value = true
}

// 提交申请
const submitApplication = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      // 与后端 FinanceRequest 字段一致：money 为元(NOT NULL)
      const data = {
        productId: form.productId,
        realName: form.realName,
        phone: form.phone,
        idNum: form.idNum,
        money: form.amount * 10000, // 万元转换为元
        purpose: form.purpose,
        repaymentSource: form.repaymentSource,
        // 联合贷款人：传所选联系人 userName，后端创建邀请；combination 资料由对方同意后回填
        jointUserName1: form.jointUserName1 || null,
        jointUserName2: form.jointUserName2 || null
      }

      if (editingApplicationId.value) {
        // TODO: 更新申请API
        ElMessage.success('申请修改成功')
      } else {
        await applyFinance(data)
        ElMessage.success('融资申请已提交，请等待银行审核')
      }

      dialogVisible.value = false
      await loadApplications()
    } catch (error) {
      // 展示后端返回的真实失败原因，便于定位（业务错误/HTTP错误均覆盖）
      const msg = error?.response?.data?.message || error?.message || '提交申请失败'
      ElMessage.error(msg)
    } finally {
      submitting.value = false
    }
  })
}

// 撤销申请
const cancelApplication = async (application) => {
  try {
    await ElMessageBox.confirm('撤销后该申请将被永久删除，确定要撤销吗？', '撤销确认', {
      type: 'warning'
    })
    await cancelFinance(application.financeId)
    ElMessage.success('申请已撤销')
    await loadApplications()
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error?.response?.data?.message || error?.message || '撤销申请失败'
      ElMessage.error(msg)
    }
  }
}

// 查看详情：与"我的融资"详情一致 —— 回填产品(含暂停状态)/联合贷款人/还款计划
const viewDetail = async (application) => {
  currentDetail.value = application
  currentProduct.value = null
  currentInvitations.value = []
  currentRepayments.value = []
  detailVisible.value = true
  // 产品介绍单独拉取：推荐列表只含在售产品，被银行暂停的产品不会在其中
  if (application.productId) {
    try {
      const res = await getFinanceProductDetail(application.productId)
      if (res.code === 200) currentProduct.value = res.data
    } catch (e) { /* 忽略：仅影响介绍展示 */ }
  }
  // 联合贷款人邀请
  try {
    const res = await getFinanceJointInvitations(application.financeId)
    currentInvitations.value = res.data || []
  } catch (e) { /* 忽略：老申请可能无邀请记录 */ }
  // 已通过融资：等额本息还款计划
  if (application.status === 1) {
    try {
      const res = await getFinanceRepayments(application.financeId)
      currentRepayments.value = res.data || []
    } catch (e) { /* 忽略 */ }
  }
}

// 删除已被拒绝的申请（status=2）；后端仅允许删除 申请中(0)/已拒绝(2)，已通过(1)不可删
const deleteApplication = async (application) => {
  try {
    await ElMessageBox.confirm('确定要删除这条已拒绝的申请吗？删除后不可恢复。', '删除确认', {
      type: 'warning'
    })
    await cancelFinance(application.financeId)
    ElMessage.success('已删除')
    await loadApplications()
  } catch (error) {
    if (error !== 'cancel') {
      const msg = error?.response?.data?.message || error?.message || '删除失败'
      ElMessage.error(msg)
    }
  }
}

// 详情/格式化辅助
const resolveProductName = (row) => row?.productName || '—'
const formatNumber = (num) => {
  if (num === null || num === undefined || num === '') return '0'
  return Number(num).toLocaleString()
}
const formatDetailTime = (t) => {
  if (!t) return '—'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
const repayStatusText = (rp) => {
  if (rp.status === 1) return '已还款'
  if (rp.status === 2) return '待确认'
  if (rp.status === 3) return '已驳回'
  return rp.overdue ? '已逾期' : '待还款'
}
const inviteStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')
const inviteStatusText = (s) => ({ 0: '待确认', 1: '已同意', 2: '已拒绝' }[s] || '未知')

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString('zh-CN')
}

// 获取状态类型（后端 status 为整数：0申请中 1已通过 2已驳回）
const getStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    0: '待审核',
    1: '已批准',
    2: '已拒绝'
  }
  return textMap[status] || status
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(form, {
    productId: null,
    realName: '',
    phone: '',
    idNum: '',
    amount: 10,
    purpose: '',
    repaymentSource: '',
    jointUserName1: '',
    jointUserName2: ''
  })
  editingApplicationId.value = null
}

onMounted(() => {
  loadFinanceProducts()
  loadApplications()
  loadContacts()
})
</script>

<style scoped>
.finance-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.product-item {
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-name {
  font-weight: 600;
  font-size: 16px;
}

.product-bank-row {
  font-size: 13px;
  color: #2d5a3d;
  margin-top: -6px;
  margin-bottom: 4px;
}

.product-desc {
  font-size: 14px;
  color: #666;
  flex: 1;
}

.product-terms {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
}

.term .label {
  color: #666;
}

.term .value {
  font-weight: 500;
}

.unit-text {
  margin-left: 8px;
  color: #666;
  font-size: 14px;
}

.combination-tip {
  margin: 0 0 16px 120px;
  padding: 8px 12px;
  font-size: 13px;
  color: #8c6d2f;
  background: #fdf6e3;
  border-left: 3px solid #e6a23c;
  border-radius: 4px;
  line-height: 1.5;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 详情弹窗：产品介绍 / 联合贷款人 / 还款计划 */
.product-introduce {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
.combination-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.combination-person {
  font-size: 14px;
  color: #4a5249;
  line-height: 1.5;
}
.cp-label {
  color: #909399;
  margin-right: 4px;
}
.repayment-section {
  margin-top: 20px;
}
.repayment-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2923;
  margin: 0 0 12px;
}
</style>
