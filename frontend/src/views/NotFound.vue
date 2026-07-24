<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="error-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"/>
          <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
          <line x1="12" y1="17" x2="12.01" y2="17"/>
        </svg>
      </div>
      <h1 class="error-title">404</h1>
      <p class="error-message">抱歉，您访问的页面不存在</p>
      <div class="error-actions">
        <router-link to="/home" class="btn-home">
          返回首页
        </router-link>
        <button @click="goBack" class="btn-back">
          返回上一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-6, 24px);
}

.not-found-content {
  text-align: center;
  max-width: 400px;
}

.error-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--spacing-6, 24px);
  color: var(--color-text-tertiary, #6b7280);
}

.error-icon svg {
  width: 100%;
  height: 100%;
}

.error-title {
  font-family: var(--font-family-display, 'Noto Serif SC', serif);
  font-size: 80px;
  font-weight: var(--font-weight-bold, 700);
  color: var(--color-text-primary, #1f2923);
  margin: 0 0 var(--spacing-4, 16px);
  line-height: 1;
}

.error-message {
  font-size: var(--font-size-lg, 20px);
  color: var(--color-text-tertiary, #6b7280);
  margin: 0 0 var(--spacing-8, 32px);
}

.error-actions {
  display: flex;
  gap: var(--spacing-3, 12px);
  justify-content: center;
}

.btn-home,
.btn-back {
  padding: var(--spacing-3, 12px) var(--spacing-5, 20px);
  font-size: var(--font-size-base, 16px);
  font-weight: var(--font-weight-medium, 500);
  border-radius: var(--radius-base, 8px);
  cursor: pointer;
  transition: all var(--transition-fast, 150ms ease);
  text-decoration: none;
}

.btn-home {
  color: var(--color-text-inverse, #ffffff);
  background: var(--color-primary, #2d5a3d);
  border: none;
}

.btn-home:hover {
  background: var(--color-primary-dark, #1f4229);
}

.btn-back {
  color: var(--color-text-primary, #1f2923);
  background: transparent;
  border: 1px solid var(--color-border, #e5e0d8);
}

.btn-back:hover {
  background: var(--color-bg-secondary, #f7f5f0);
  border-color: var(--color-primary, #2d5a3d);
}
</style>
