<template>
  <div class="admin-container">
    <el-card class="stats-card">
      <h2>平台统计</h2>
      <el-row :gutter="20" v-loading="loading">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.productCount || 0 }}</div>
            <div class="stat-label">商品数量</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.financeCount || 0 }}</div>
            <div class="stat-label">融资申请</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.knowledgeCount || 0 }}</div>
            <div class="stat-label">知识文章</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ stats.questionCount || 0 }}</div>
            <div class="stat-label">问答数量</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="menu-card" @click="$router.push('/admin/banners')">
          <div class="menu-item">
            <el-icon :size="32"><Picture /></el-icon>
            <span>轮播图管理</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="menu-card" @click="$router.push('/admin/products')">
          <div class="menu-item">
            <el-icon :size="32"><Box /></el-icon>
            <span>商品管理</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="menu-card" @click="$router.push('/admin/users')">
          <div class="menu-item">
            <el-icon :size="32"><User /></el-icon>
            <span>用户管理</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="menu-card" @click="$router.push('/admin/finances')">
          <div class="menu-item">
            <el-icon :size="32"><Coin /></el-icon>
            <span>融资管理</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="menu-card" @click="$router.push('/admin/experts')">
          <div class="menu-item">
            <el-icon :size="32"><Avatar /></el-icon>
            <span>专家管理</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="agent-toggle-card">
      <div class="agent-toggle">
        <div class="agent-toggle__info">
          <el-icon :size="20" color="var(--color-primary)"><Service /></el-icon>
          <div>
            <div class="agent-toggle__title">AI 智能助手</div>
            <div class="agent-toggle__desc">全局开关 — 关闭后农户/买家无法使用智能助手</div>
          </div>
        </div>
        <el-switch
          v-model="agentEnabled"
          :loading="agentToggleLoading"
          @change="handleToggleAgent"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, Box, User, Coin, Avatar, Service } from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/admin'
import { getAgentStatus, toggleAgent } from '@/api/agent'

const router = useRouter()
const loading = ref(false)
const stats = reactive({
  productCount: 0,
  financeCount: 0,
  knowledgeCount: 0,
  questionCount: 0,
  userCount: 0
})

onMounted(async () => {
  await loadStats()
  await loadAgentStatus()
})

const loadStats = async () => {
  loading.value = true
  try {
    const res = await getAdminStats()
    if (res.data) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// ===== AI 助手开关 =====
const agentEnabled = ref(false)
const agentToggleLoading = ref(false)

const loadAgentStatus = async () => {
  try {
    const res = await getAgentStatus()
    agentEnabled.value = res.data.enabled
  } catch (e) {
    // 静默失败(不影响管理员面板其它功能)
  }
}

const handleToggleAgent = async (val) => {
  agentToggleLoading.value = true
  try {
    await toggleAgent(val)
    ElMessage.success(val ? 'AI 助手已开启' : 'AI 助手已关闭')
  } catch (e) {
    agentEnabled.value = !val  // 回滚
    ElMessage.error('操作失败')
  } finally {
    agentToggleLoading.value = false
  }
}
</script>

<style scoped>
.admin-container {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.stats-card {
  margin-bottom: 20px;
}

.stats-card h2 {
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
}

.stat-item {
  text-align: center;
  padding: 20px;
}

.stat-value {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 10px;
}

.stat-label {
  color: #666;
  font-size: 14px;
}

.menu-card {
  cursor: pointer;
  transition: box-shadow 0.3s;
  margin-bottom: 20px;
}

.menu-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
}

.menu-item span {
  font-size: 14px;
  color: #333;
}

/* AI 助手开关卡片 */
.agent-toggle-card {
  margin-bottom: 20px;
}
.agent-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.agent-toggle__info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.agent-toggle__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}
.agent-toggle__desc {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}
</style>
