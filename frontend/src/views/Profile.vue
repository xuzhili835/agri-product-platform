<template>
  <div class="profile-container">
    <h1 class="page-title">个人中心</h1>

    <!-- 用户信息卡：头像 + 姓名/角色/联系方式 + 编辑资料（flex 流式，去绝对定位） -->
    <el-card class="user-card" shadow="never">
      <div class="user-info">
        <el-avatar :size="84" :src="userStore.userAvatar() || '/images/default-avatar.png'" />
        <div class="info">
          <div class="info-head">
            <h2>{{ userStore.displayName() }}</h2>
            <span class="role-badge">{{ getRoleText(userStore.role) }}</span>
          </div>
          <p class="meta">{{ userStore.userInfo?.phone || '未设置电话' }}</p>
          <p class="meta">身份证号：{{ maskIdNum(userStore.userInfo?.identityNum) || '未设置' }}</p>
          <p class="credit" v-if="isFarmer">
            信用分
            <el-rate :model-value="userStore.userInfo?.credit || 0" disabled size="small" show-score score-template="{value} 星" style="vertical-align: middle; margin-left: 6px" />
          </p>
        </div>
        <el-button type="primary" @click="goToEdit" class="edit-btn">编辑资料</el-button>
      </div>
    </el-card>

    <h3 class="section-title">快捷功能</h3>
    <!-- 统一响应式网格：auto-fill + minmax，所有角色卡片宽度一致、自动换行，
         不再出现 span 8/12 混用导致的锯齿排列与半行空缺 -->
    <div class="menu-grid">
      <div
        v-for="item in menuItems"
        :key="item.label"
        class="menu-card"
        @click="handleMenu(item)"
      >
        <div class="menu-icon" :style="{ color: item.color, background: item.color + '1a' }">
          <el-icon :size="28"><component :is="item.icon" /></el-icon>
        </div>
        <span class="menu-label">{{ item.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Document,
  ShoppingCart,
  Location,
  Money,
  Plus,
  Edit,
  Calendar,
  Briefcase,
  ChatLineSquare,
  Reading,
  DataAnalysis,
  Connection,
  Coin
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const isFarmer = computed(() => userStore.role === 'farmer')
const isBuyer = computed(() => userStore.role === 'buyer')
const isExpert = computed(() => userStore.role === 'expert')
const isBank = computed(() => userStore.role === 'bank')
const isAdmin = computed(() => userStore.role === 'admin')

// 统一构造快捷功能列表（按角色过滤），交给网格渲染，避免多段 el-row/span 拼接
const menuItems = computed(() => {
  const items = []
  if (isFarmer.value || isBuyer.value) {
    items.push({ label: '我的订单', icon: Document, color: '#409eff', handler: goToOrders })
  }
  if (isBuyer.value) {
    items.push({ label: '购物车', icon: ShoppingCart, color: '#67c23a', path: '/cart' })
  }
  items.push({ label: '地址管理', icon: Location, color: '#e6a23c', path: '/address' })
  if (isFarmer.value || isBuyer.value) {
    items.push({ label: '专家咨询', icon: ChatLineSquare, color: '#909399', handler: goToExpertConsult })
    items.push({ label: '农业知识', icon: Reading, color: '#f56c6c', handler: goToKnowledge })
  }
  if (isFarmer.value) {
    items.push({ label: '我的融资', icon: Money, color: '#409eff', path: '/farmer/my-finance' })
    items.push({ label: '联合贷款邀请', icon: Connection, color: '#9c27b0', path: '/farmer/my-finance' })
    items.push({ label: '发布商品', icon: Plus, color: '#67c23a', path: '/farmer/products' })
  }
  if (isExpert.value) {
    items.push({ label: '发布知识', icon: Edit, color: '#409eff', path: '/knowledge/publish' })
    items.push({ label: '预约管理', icon: Calendar, color: '#e6a23c', path: '/expert/reservations' })
  }
  if (isBank.value) {
    items.push({ label: '融资审批', icon: Briefcase, color: '#409eff', path: '/bank/approvals' })
    items.push({ label: '融资产品', icon: Coin, color: '#67c23a', path: '/bank/products' })
  }
  if (isAdmin.value) {
    items.push({ label: '管理后台', icon: DataAnalysis, color: '#909399', path: '/admin/dashboard' })
  }
  return items
})

const handleMenu = (item) => {
  if (item.handler) item.handler()
  else if (item.path) router.push(item.path)
}

const getRoleText = (role) => {
  const map = {
    farmer: '农户',
    buyer: '买家',
    expert: '专家',
    bank: '银行',
    admin: '管理员'
  }
  return map[role] || role
}

// 身份证号脱敏展示（仅本人个人中心可见）
const maskIdNum = (id) => {
  if (!id) return ''
  if (id.length <= 6) return id
  return id.slice(0, 6) + '********' + id.slice(-4)
}

const goToEdit = () => {
  router.push('/profile/edit')
}

// 角色感知跳转：我的订单（农户→农户订单页，买家→买家订单页）
// /order 与 /cart 路由带 roles:['buyer']，非买家会被全局守卫弹回首页，故按角色分流
const goToOrders = () => {
  if (isFarmer.value) router.push('/farmer/orders')
  else router.push('/order')
}

// 专家咨询：跳农户/买家各自的咨询工作台（/farmer/expert、/buyer/expert）
// 注意：/expert-help 是"专家助力"列表页，不是咨询工作台，此前错指过去
const goToExpertConsult = () => {
  router.push(isFarmer.value ? '/farmer/expert' : '/buyer/expert')
}

// 农业知识：进入「专家咨询」工作台的「知识库」标签页（侧边栏布局内），与其他功能一致；
// 不再跳独立的公开 /knowledge 列表页
const goToKnowledge = () => {
  router.push({
    path: isFarmer.value ? '/farmer/expert' : '/buyer/expert',
    query: { tab: 'knowledge' }
  })
}
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 24px auto;
  padding: 0 20px 48px;
}

.page-title {
  margin: 0 0 20px;
  font-size: 24px;
  font-weight: 600;
  color: #1f2923;
}

/* ===== 用户信息卡 ===== */
.user-card {
  margin-bottom: 28px;
  border-radius: 12px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.info {
  flex: 1;
  min-width: 0;
}

.info-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.info h2 {
  margin: 0;
  font-size: 20px;
  color: #1f2923;
}

.role-badge {
  display: inline-block;
  padding: 2px 12px;
  font-size: 12px;
  line-height: 18px;
  color: #fff;
  background: #2d5a3d;
  border-radius: 10px;
}

.info .meta {
  margin: 4px 0;
  color: #909399;
  font-size: 14px;
}

.info .credit {
  margin: 8px 0 0;
  color: #606266;
  font-size: 14px;
}

.edit-btn {
  flex-shrink: 0;
}

/* ===== 快捷功能 ===== */
.section-title {
  margin: 8px 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #606266;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.menu-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 26px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;
}

.menu-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  border-color: transparent;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
}

.menu-label {
  font-size: 15px;
  color: #303133;
}

/* ===== 响应式：窄屏下用户卡折行，按钮不再被挤 ===== */
@media (max-width: 640px) {
  .user-info {
    flex-wrap: wrap;
  }
  .edit-btn {
    margin-left: auto;
  }
}
</style>
