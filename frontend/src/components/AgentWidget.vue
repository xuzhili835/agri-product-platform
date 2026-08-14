<template>
  <!-- 浮动按钮(仅 farmer/buyer 且已登录时显示) -->
  <div
    v-if="shouldShow"
    class="agent-fab"
    :class="{ 'agent-fab--open': isOpen }"
    @click="togglePanel"
  >
    <el-icon :size="24">
      <ChatDotRound v-if="!isOpen" />
      <Close v-else />
    </el-icon>
  </div>

  <!-- 聊天面板 -->
  <transition name="agent-slide">
    <div v-if="shouldShow && isOpen" class="agent-panel">
      <!-- 头部 -->
      <div class="agent-header">
        <div class="agent-header__title">
          <el-icon :size="18"><Service /></el-icon>
          <span>智能助手</span>
        </div>
        <el-icon class="agent-header__close" :size="18" @click="isOpen = false">
          <Close />
        </el-icon>
      </div>

      <!-- 消息区 -->
      <div class="agent-messages" ref="messagesEl">
        <template v-if="agentEnabled">
          <!-- 欢迎语(首次打开) -->
          <div v-if="messages.length === 0" class="agent-welcome">
            <p>👋 你好!我是农融汇智能助手。</p>
            <p>可以问我:信用分、融资套餐、市场行情、农技知识,也可以直接申请融资或预约专家。</p>
          </div>

          <!-- 消息列表 -->
          <div
            v-for="(msg, i) in messages"
            :key="i"
            :class="['agent-msg', `agent-msg--${msg.role}`]"
          >
            <div class="agent-msg__content">{{ msg.content }}</div>
            <!-- 确认卡(写操作待确认且未处理) -->
            <div v-if="msg.needsConfirm && !msg.resolved" class="agent-confirm">
              <el-button
                type="primary"
                size="small"
                :loading="confirming"
                @click="handleConfirm(msg, true)"
              >确认执行</el-button>
              <el-button
                size="small"
                :loading="confirming"
                @click="handleConfirm(msg, false)"
              >取消</el-button>
            </div>
            <!-- 已处理标记 -->
            <div v-if="msg.needsConfirm && msg.resolved" class="agent-confirm__done">
              {{ msg.resolvedText }}
            </div>
          </div>

          <!-- 加载态 -->
          <div v-if="loading" class="agent-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>正在思考...</span>
          </div>
        </template>

        <!-- 助手已停用 -->
        <div v-else class="agent-disabled">
          <el-icon :size="32"><CircleClose /></el-icon>
          <p>智能助手已停用</p>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="agent-input" v-if="agentEnabled">
        <el-input
          v-model="inputText"
          placeholder="输入消息,按 Enter 发送"
          :disabled="loading || confirming"
          @keyup.enter="handleSend"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          :loading="loading"
          :disabled="!inputText.trim()"
          @click="handleSend"
        />
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound, Close, Service, Loading,
  CircleClose, Promotion
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getAgentStatus, agentChat, agentConfirm, getAgentHistory } from '@/api/agent'

const userStore = useUserStore()

// ===== 显示控制 =====
const isOpen = ref(false)
const shouldShow = computed(() => {
  return userStore.isLoggedIn() && userStore.hasRole(['farmer', 'buyer'])
})

// ===== 状态 =====
const agentEnabled = ref(true)
const sessionId = ref(localStorage.getItem('agent_session_id') || '')
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const confirming = ref(false)
const messagesEl = ref(null)

// ===== 方法 =====

/** 加载历史消息 */
async function loadHistory() {
  try {
    const res = await getAgentHistory(sessionId.value)
    if (res.data && Array.isArray(res.data)) {
      messages.value = res.data.map(m => ({
        role: m.direction,
        content: m.content,
        needsConfirm: false,
        pendingId: null,
        resolved: true
      }))
      scrollToBottom()
    }
  } catch (e) {
    // 历史加载失败不阻断,清掉旧 sessionId
    sessionId.value = ''
    localStorage.removeItem('agent_session_id')
  }
}

/** 切换面板开关 */
async function togglePanel() {
  isOpen.value = !isOpen.value
  if (!isOpen.value) return
  // 每次打开都查状态(admin 可能在后台切了开关)
  try {
    const res = await getAgentStatus()
    agentEnabled.value = res.data.enabled
  } catch (e) {
    agentEnabled.value = false
  }
  // 首次打开加载历史(只在 messages 为空时)
  if (messages.value.length === 0 && agentEnabled.value && sessionId.value) {
    await loadHistory()
  }
  scrollToBottom()
}

/** 发送消息 */
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  // 追加用户消息
  messages.value.push({ role: 'user', content: text, needsConfirm: false, pendingId: null, resolved: true })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await agentChat({
      message: text,
      sessionId: sessionId.value || undefined
    })
    const d = res.data
    // 保存 sessionId
    if (d.sessionId && d.sessionId !== sessionId.value) {
      sessionId.value = d.sessionId
      localStorage.setItem('agent_session_id', d.sessionId)
    }
    // 追加助手消息
    messages.value.push({
      role: 'assistant',
      content: d.reply,
      needsConfirm: d.needsConfirm,
      pendingId: d.pendingId || null,
      resolved: !d.needsConfirm
    })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉,出了点问题:' + (e.message || '请稍后重试'),
      needsConfirm: false, pendingId: null, resolved: true
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/** 确认/取消写操作 */
async function handleConfirm(msg, accept) {
  confirming.value = true
  try {
    const res = await agentConfirm({
      pendingId: msg.pendingId,
      accept: accept,
      sessionId: sessionId.value
    })
    msg.resolved = true
    msg.resolvedText = accept ? '✅ 已执行' : '❌ 已取消'
    // 追加执行结果消息
    messages.value.push({
      role: 'assistant',
      content: res.data.reply,
      needsConfirm: false, pendingId: null, resolved: true
    })
  } catch (e) {
    ElMessage.error('确认失败:' + (e.message || '请重试'))
  } finally {
    confirming.value = false
    scrollToBottom()
  }
}

/** 滚动到底部 */
function scrollToBottom() {
  nextTick(() => {
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight
    }
  })
}

// 登出时清理
watch(() => userStore.isLoggedIn(), (loggedIn) => {
  if (!loggedIn) {
    isOpen.value = false
    messages.value = []
    sessionId.value = ''
    localStorage.removeItem('agent_session_id')
  }
})
</script>

<style scoped>
/* ===== 浮动按钮 ===== */
.agent-fab {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-lg);
  z-index: 9998;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}
.agent-fab:hover {
  transform: scale(1.08);
  box-shadow: var(--shadow-xl);
}
.agent-fab--open {
  background: var(--color-text-tertiary);
}

/* ===== 面板 ===== */
.agent-panel {
  position: fixed;
  right: 24px;
  bottom: 24px;
  width: 380px;
  height: 520px;
  background: var(--color-bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 头部 */
.agent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3) var(--spacing-4);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: #fff;
}
.agent-header__title {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}
.agent-header__close {
  cursor: pointer;
  opacity: 0.8;
  transition: opacity var(--transition-fast);
}
.agent-header__close:hover {
  opacity: 1;
}

/* 消息区 */
.agent-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-3);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  background: var(--color-bg-secondary);
}

/* 欢迎语 */
.agent-welcome {
  text-align: center;
  padding: var(--spacing-6) var(--spacing-3);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-relaxed);
}
.agent-welcome p {
  margin-bottom: var(--spacing-2);
}

/* 单条消息 */
.agent-msg {
  max-width: 85%;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-1);
}
.agent-msg--user {
  align-self: flex-end;
}
.agent-msg--assistant {
  align-self: flex-start;
}
.agent-msg__content {
  padding: var(--spacing-2) var(--spacing-3);
  border-radius: var(--radius-base);
  font-size: var(--font-size-sm);
  line-height: var(--line-height-normal);
  white-space: pre-wrap;
  word-break: break-word;
}
.agent-msg--user .agent-msg__content {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: var(--radius-sm);
}
.agent-msg--assistant .agent-msg__content {
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: var(--radius-sm);
}

/* 确认卡 */
.agent-confirm {
  display: flex;
  gap: var(--spacing-2);
  padding: var(--spacing-1) 0;
}
.agent-confirm__done {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

/* 加载态 */
.agent-loading {
  align-self: flex-start;
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
  padding: var(--spacing-2);
}

/* 停用态 */
.agent-disabled {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-3);
  color: var(--color-text-tertiary);
}

/* 输入区 */
.agent-input {
  display: flex;
  gap: var(--spacing-2);
  padding: var(--spacing-3);
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-primary);
}
.agent-input .el-input {
  flex: 1;
}

/* ===== 动画 ===== */
.agent-slide-enter-active,
.agent-slide-leave-active {
  transition: opacity var(--transition-base), transform var(--transition-base);
}
.agent-slide-enter-from,
.agent-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .agent-panel {
    width: calc(100vw - 32px);
    height: calc(100vh - 120px);
    right: 16px;
    bottom: 88px;
  }
  .agent-fab {
    right: 16px;
    bottom: 16px;
  }
}
</style>
