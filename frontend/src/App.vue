<template>
  <div id="app">
    <router-view />
    <AgentWidget />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserInfo } from '@/api/user'
import AgentWidget from '@/components/AgentWidget.vue'

const router = useRouter()
const userStore = useUserStore()

// 应用启动时验证 token 有效性
onMounted(async () => {
  const token = localStorage.getItem('token')

  if (token) {
    try {
      // 验证 token 是否有效
      await getUserInfo()
    } catch (error) {
      // token 无效，清除登录状态
      console.log('Token 已失效，清除登录状态')
      userStore.logout()
    }
  }
})
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

:root {
  --color-primary: #2D5A3D;
  --color-primary-dark: #1F4229;
  --color-gold: #C9A661;
  --color-bg: #F7F5F0;
  --color-text: #1F2923;
  --color-text-light: #6B7280;
  --font-serif: 'Noto Serif SC', serif;
  --font-sans: 'Noto Sans SC', sans-serif;
}

body {
  font-family: var(--font-sans), -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
  background-color: var(--color-bg);
}

/* 全局链接样式 */
a {
  color: inherit;
  text-decoration: none;
}

/* 全局按钮重置 */
button {
  font-family: inherit;
  font-size: inherit;
}

/* Element Plus 样式覆盖 */
.el-dropdown-menu__item {
  font-family: var(--font-sans);
}
</style>
