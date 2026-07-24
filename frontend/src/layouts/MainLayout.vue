<template>
  <div class="main-layout">
    <AppHeader />
    <div class="layout-container">
      <AppSidebar
        :title="sidebarTitle"
        :menu-items="menuItems"
        @toggle-collapse="handleCollapse"
      />
      <main class="layout-main" :class="{ 'expanded': isCollapsed }">
        <div class="main-content">
          <router-view v-slot="{ Component, route }">
            <component :is="Component" :key="route.path" />
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import AppSidebar from './components/AppSidebar.vue'

const props = defineProps({
  sidebarTitle: {
    type: String,
    default: '工作台'
  },
  menuItems: {
    type: Array,
    default: () => []
  }
})

const route = useRoute()
const isCollapsed = ref(false)

const handleCollapse = (collapsed) => {
  isCollapsed.value = collapsed
}
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: var(--color-bg-secondary, #f7f5f0);
}

.layout-container {
  display: flex;
  padding-top: var(--header-height, 64px);
}

.layout-main {
  flex: 1;
  margin-left: var(--sidebar-width, 240px);
  transition: margin-left var(--transition-base, 250ms ease);
  min-height: calc(100vh - var(--header-height, 64px));
}

.layout-main.expanded {
  margin-left: var(--sidebar-collapsed-width, 64px);
}

.main-content {
  padding: var(--spacing-6, 24px);
  max-width: 1440px;
  margin: 0 auto;
}

/* ===== 页面切换动画 ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-base, 250ms ease);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .layout-main {
    margin-left: 0;
  }

  .main-content {
    padding: var(--spacing-4, 16px);
  }
}
</style>
