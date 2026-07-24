<template>
  <aside class="app-sidebar" :class="{ 'collapsed': collapsed }">
    <div class="sidebar-header">
      <button class="collapse-btn" @click="toggleCollapse">
        <el-icon>
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
      </button>
      <div v-if="!collapsed" class="sidebar-title">{{ title }}</div>
    </div>

    <el-menu
      :default-active="activeMenu"
      :collapse="collapsed"
      :unique-opened="true"
      class="sidebar-menu"
      router
    >
      <template v-for="item in menuItems" :key="item.path">
        <el-menu-item v-if="!item.children" :index="item.path" :route="item.path">
          <el-icon v-if="item.icon">
            <component :is="item.icon" />
          </el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>

        <el-sub-menu v-else :index="item.path">
          <template #title>
            <el-icon v-if="item.icon">
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.title }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.path"
            :index="child.path"
            :route="child.path"
          >
            <el-icon v-if="child.icon">
              <component :is="child.icon" />
            </el-icon>
            <template #title>{{ child.title }}</template>
          </el-menu-item>
        </el-sub-menu>
      </template>
    </el-menu>

    <div v-if="!collapsed" class="sidebar-footer">
      <div class="footer-content">
        <div class="help-item" @click="showHelp">
          <el-icon><QuestionFilled /></el-icon>
          <span>帮助中心</span>
        </div>
        <div class="help-item" @click="showContact">
          <el-icon><Service /></el-icon>
          <span>联系客服</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  title: {
    type: String,
    default: '工作台'
  },
  menuItems: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['toggle-collapse'])

const route = useRoute()
const collapsed = ref(false)

const activeMenu = computed(() => {
  return route.path
})

const toggleCollapse = () => {
  collapsed.value = !collapsed.value
  emit('toggle-collapse', collapsed.value)
}

// 帮助中心：按用户建议做成小提示弹窗，给出各角色使用指引
const showHelp = () => {
  ElMessageBox.alert(
    '农户：发布农产品 · 申请融资 · 预约专家咨询<br/>' +
    '买家：浏览市场 · 发布求购 · 下单采购<br/>' +
    '专家：在线答疑 · 处理预约<br/>' +
    '银行：发布融资产品 · 审批申请 · 还款核验<br/><br/>' +
    '遇到问题可在对应页面联系发布方，或通过「联系客服」反馈。',
    '帮助中心',
    { confirmButtonText: '我知道了', type: 'info', dangerouslyUseHTMLString: true }
  )
}

// 联系客服：弹窗给出联系方式
const showContact = () => {
  ElMessageBox.alert(
    '客服热线：400-888-0000（工作日 9:00–18:00）<br/>' +
    '客服邮箱：support@nongronghui.com<br/><br/>' +
    '请描述您的问题，客服将在 1 个工作日内回复。',
    '联系客服',
    { confirmButtonText: '关闭', type: 'info', dangerouslyUseHTMLString: true }
  )
}
</script>

<style scoped>
.app-sidebar {
  position: fixed;
  top: var(--header-height, 64px);
  left: 0;
  bottom: 0;
  width: var(--sidebar-width, 240px);
  background: var(--color-bg-primary, #ffffff);
  border-right: 1px solid var(--color-border, #e5e0d8);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-base, 250ms ease);
  z-index: var(--z-sticky, 1020);
}

.app-sidebar.collapsed {
  width: var(--sidebar-collapsed-width, 64px);
}

/* ===== 侧边栏头部 ===== */
.sidebar-header {
  display: flex;
  align-items: center;
  padding: var(--spacing-4, 16px);
  border-bottom: 1px solid var(--color-divider, #edebe6);
  min-height: 52px;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-base, 8px);
  cursor: pointer;
  color: var(--color-text-secondary, #4a5249);
  transition: all var(--transition-fast, 150ms ease);
}

.collapse-btn:hover {
  background: var(--color-bg-secondary, #f7f5f0);
  color: var(--color-primary, #2d5a3d);
}

.sidebar-title {
  flex: 1;
  margin-left: var(--spacing-3, 12px);
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: var(--font-size-lg, 20px);
  font-weight: var(--font-weight-semibold, 600);
  color: var(--color-text-primary, #1f2923);
}

/* ===== 菜单 ===== */
.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border: none;
  padding: var(--spacing-3, 12px) 0;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: var(--sidebar-width, 240px);
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 44px;
  margin: 0 var(--spacing-3, 12px);
  padding: 0 var(--spacing-3, 12px);
  border-radius: var(--radius-base, 8px);
  color: var(--color-text-secondary, #4a5249);
  font-size: var(--font-size-sm, 14px);
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: var(--color-bg-secondary, #f7f5f0);
  color: var(--color-primary, #2d5a3d);
}

:deep(.el-menu-item.is-active) {
  background: rgba(45, 90, 61, 0.1);
  color: var(--color-primary, #2d5a3d);
  font-weight: var(--font-weight-medium, 500);
}

:deep(.el-sub-menu .el-menu-item) {
  padding-left: var(--spacing-10, 40px) !important;
}

:deep(.el-icon) {
  font-size: 18px;
}

/* ===== 侧边栏底部 ===== */
.sidebar-footer {
  padding: var(--spacing-4, 16px);
  border-top: 1px solid var(--color-divider, #edebe6);
}

.footer-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2, 8px);
}

.help-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2, 8px);
  padding: var(--spacing-2, 8px) var(--spacing-3, 12px);
  font-size: var(--font-size-sm, 14px);
  color: var(--color-text-tertiary, #6b7280);
  border-radius: var(--radius-base, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
}

.help-item:hover {
  background: var(--color-bg-secondary, #f7f5f0);
  color: var(--color-primary, #2d5a3d);
}

.help-item .el-icon {
  font-size: 16px;
}

/* ===== 滚动条样式 ===== */
.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: var(--color-border, #e5e0d8);
  border-radius: 3px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-tertiary, #6b7280);
}

/* ===== 折叠状态 ===== */
.collapsed .sidebar-footer {
  display: none;
}
</style>
