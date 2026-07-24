<template>
  <div ref="container" class="turnstile-box"></div>
</template>

<script setup>
/**
 * Cloudflare Turnstile 人机验证组件（可复用）
 * - 用户通过验证后 emit('verified', token)，父组件保存 token 并随业务请求带到后端
 * - token 一次性、约 5 分钟过期：失败/过期后父组件需调用 reset() 重置，才能再次验证
 * - api.js 在 index.html 以 async defer 加载，挂载时可能未就绪，故轮询到就绪再渲染
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  // Cloudflare Site Key；不传则读 Vite 环境变量 VITE_TURNSTILE_SITE_KEY
  siteKey: {
    type: String,
    default: () => import.meta.env.VITE_TURNSTILE_SITE_KEY || ''
  }
})

const emit = defineEmits(['verified', 'expired', 'error'])

const container = ref(null)
let widgetId = null
let pollTimer = null

const render = () => {
  if (!window.turnstile || !container.value || widgetId !== null) return
  widgetId = window.turnstile.render(container.value, {
    sitekey: props.siteKey,
    callback: (token) => emit('verified', token),
    'expired-callback': () => emit('expired'),
    'error-callback': () => emit('error')
  })
}

// 暴露给父组件：失败后重置组件（token 一次性，必须重置才能再次提交）
defineExpose({
  reset() {
    if (window.turnstile && widgetId !== null) {
      window.turnstile.reset(widgetId)
    }
  }
})

onMounted(() => {
  if (window.turnstile) {
    render()
    return
  }
  pollTimer = setInterval(() => {
    if (window.turnstile) {
      clearInterval(pollTimer)
      pollTimer = null
      render()
    }
  }, 200)
  // 兜底：10s 后停止轮询
  setTimeout(() => {
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  }, 10000)
})

onBeforeUnmount(() => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
  if (window.turnstile && widgetId !== null) {
    window.turnstile.remove(widgetId)
    widgetId = null
  }
})
</script>

<style scoped>
.turnstile-box {
  display: flex;
  justify-content: center;
  margin-bottom: 4px;
}
</style>
