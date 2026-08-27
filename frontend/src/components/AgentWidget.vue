<template>
  <!-- 浮动按钮(仅 farmer/buyer 且已登录时显示),可拖动 -->
  <div
    v-if="shouldShow"
    ref="fabEl"
    class="agent-fab"
    :class="{ 'agent-fab--open': isOpen }"
    :style="fabStyle"
    @pointerdown="startDrag($event, 'fab')"
    @click="togglePanel"
  >
    <el-icon :size="24">
      <ChatDotRound v-if="!isOpen" />
      <Close v-else />
    </el-icon>
  </div>

  <!-- 聊天面板,按住头部可拖动 -->
  <transition name="agent-slide">
    <div v-if="shouldShow && isOpen" ref="panelEl" class="agent-panel" :style="panelStyle">
      <!-- 头部 -->
      <div class="agent-header" @pointerdown="startDrag($event, 'panel')">
        <div class="agent-header__title">
          <el-icon :size="18"><Service /></el-icon>
          <span>智能助手</span>
        </div>
        <el-icon class="agent-header__close" :size="18" @pointerdown.stop @click="isOpen = false">
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
            <!-- 表单卡(表单工具待确认):已提取信息预填,缺失槽位用户在表单里补 -->
            <el-form
              v-if="msg.form && msg.form.length && !msg.resolved"
              :ref="el => setFormRef(msg, el)"
              :model="msg.formModel"
              :rules="msg.formRules"
              label-position="top"
              size="small"
              class="agent-form"
              @submit.prevent
            >
              <el-form-item
                v-for="f in msg.form"
                :key="f.key"
                :label="f.label + (f.required ? '' : '(选填)')"
                :prop="f.key"
              >
                <el-cascader
                  v-if="f.type === 'region'"
                  v-model="msg.formModel[f.key]"
                  :options="regionOptions"
                  :props="{ expandTrigger: 'hover', value: 'label', label: 'label' }"
                  placeholder="请选择省/市/区"
                  style="width: 100%"
                  clearable
                />
                <el-select
                  v-else-if="f.type === 'select'"
                  v-model="msg.formModel[f.key]"
                  placeholder="请选择"
                  style="width: 100%"
                  clearable
                >
                  <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
                <el-input-number
                  v-else-if="f.type === 'number'"
                  v-model="msg.formModel[f.key]"
                  :min="0"
                  :controls="false"
                  placeholder="请输入金额"
                  style="width: 100%"
                />
                <el-switch v-else-if="f.type === 'switch'" v-model="msg.formModel[f.key]" />
                <el-input
                  v-else-if="f.type === 'textarea'"
                  v-model="msg.formModel[f.key]"
                  type="textarea"
                  :rows="2"
                  :placeholder="f.placeholder || ''"
                />
                <el-input v-else v-model="msg.formModel[f.key]" :placeholder="f.placeholder || ''" />
                <div v-if="f.hint" class="agent-form__hint">{{ f.hint }}</div>
              </el-form-item>
            </el-form>
            <!-- 确认卡(写操作待确认且未处理) -->
            <div v-if="msg.needsConfirm && !msg.resolved" class="agent-confirm">
              <div v-if="msg.confirmError" class="agent-form__error">{{ msg.confirmError }}</div>
              <el-button
                type="primary"
                size="small"
                :loading="msg.confirming"
                @click="handleConfirm(msg, true)"
                >确认执行</el-button
              >
              <el-button
                size="small"
                :loading="msg.confirming"
                @click="handleConfirm(msg, false)"
                >取消</el-button
              >
            </div>
            <!-- 已处理标记 -->
            <div v-if="msg.needsConfirm && msg.resolved" class="agent-confirm__done">
              {{ msg.resolvedText }}
            </div>
            <!-- 恢复的旧确认卡时效提示 -->
            <div v-if="msg.needsConfirm && !msg.resolved && msg.expired" class="agent-confirm__hint">
              刷新前待确认的操作(5 分钟内有效,超时请重新发起)
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

      <!-- 输入区(有待确认卡时不锁输入:发新消息会作废旧卡并提示,由后端保证) -->
      <div class="agent-input" v-if="agentEnabled">
        <el-input
          v-model="inputText"
          placeholder="输入消息,按 Enter 发送"
          :disabled="loading"
          @keydown.enter="onEnterKey"
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
import { ref, reactive, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatDotRound, Close, Service, Loading,
  CircleClose, Promotion
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getAgentStatus, agentChat, agentConfirm, getAgentHistory } from '@/api/agent'
import { regionData } from 'element-china-area-data'

const userStore = useUserStore()

// 表单卡的 el-form 实例集合(按 pendingId 索引),确认前先做必填校验
const formRefs = {}
// 中国省市区数据(完整版),与地址管理页同源
const regionOptions = regionData

function setFormRef(msg, el) {
  if (el) formRefs[msg.pendingId] = el
}

/**
 * 后端下发的表单字段列表 → 前端表单状态。
 * switch 转 boolean、region 的"省 市 区"串拆成 cascader 路径数组、number 转数值;
 * required 生成必填规则,purpose/repaymentSource 追加"不少于15字"(后端 validate 同口径)。
 */
function buildFormState(fields) {
  const model = {}
  const rules = {}
  for (const f of fields) {
    if (f.type === 'switch') {
      model[f.key] = f.value === 'true' || f.value === true
    } else if (f.type === 'region') {
      model[f.key] = f.value && typeof f.value === 'string' && f.value.trim()
        ? f.value.trim().split(/\s+/)
        : []
    } else if (f.type === 'number') {
      const n = f.value !== null && f.value !== undefined && f.value !== '' ? Number(f.value) : NaN
      model[f.key] = Number.isFinite(n) ? n : undefined
    } else {
      model[f.key] = f.value || ''
    }
    const rs = []
    if (f.required && f.type !== 'switch') {
      const isPick = f.type === 'region' || f.type === 'select'
      rs.push({
        required: true,
        message: `请${isPick ? '选择' : '填写'}${f.label}`,
        trigger: isPick ? 'change' : 'blur'
      })
    }
    if (f.key === 'purpose' || f.key === 'repaymentSource') {
      rs.push({ min: 15, message: `${f.label}不能少于15个字`, trigger: 'blur' })
    }
    if (rs.length) rules[f.key] = rs
  }
  return { model, rules }
}

/** 收集表单值为 confirm 的 args:region 拆回省/市/区,switch 转 "true"/"false",空值不传。 */
function collectFormArgs(msg) {
  const args = {}
  for (const f of msg.form) {
    const v = msg.formModel[f.key]
    if (f.type === 'region') {
      if (Array.isArray(v) && v.length >= 2) {
        args.province = v[0]
        args.city = v[1]
        args.area = v[2] || ''
      }
    } else if (f.type === 'switch') {
      args[f.key] = String(v === true)
    } else if (f.type === 'number') {
      if (v !== undefined && v !== null && v !== '') args[f.key] = String(v)
    } else if (v !== null && v !== undefined && String(v).trim()) {
      args[f.key] = String(v).trim()
    }
  }
  return args
}

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
const messagesEl = ref(null)

/** Enter 发送(过滤输入法组合键:中文输入法选词的回车不应发送) */
function onEnterKey(e) {
  if (e.isComposing || e.keyCode === 229) return
  handleSend()
}

// ===== 拖动(浮窗按钮 + 面板头部) =====
const fabEl = ref(null)
const panelEl = ref(null)
const fabPos = ref(null)    // 首次拖动后才启用 left/top 定位,之前用 CSS 默认角落
const panelPos = ref(null)
const drag = reactive({ active: false, moved: false, startX: 0, startY: 0, origX: 0, origY: 0, target: null })

const fabStyle = computed(() => fabPos.value
  ? { left: fabPos.value.x + 'px', top: fabPos.value.y + 'px', right: 'auto', bottom: 'auto' }
  : null)
const panelStyle = computed(() => panelPos.value
  ? { left: panelPos.value.x + 'px', top: panelPos.value.y + 'px', right: 'auto', bottom: 'auto' }
  : null)

function startDrag(e, target) {
  const el = target === 'fab' ? fabEl.value : panelEl.value
  if (!el || !e.clientX) return
  const rect = el.getBoundingClientRect()
  drag.active = true
  drag.moved = false
  drag.target = target
  drag.startX = e.clientX
  drag.startY = e.clientY
  drag.origX = rect.left
  drag.origY = rect.top
  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', endDrag)
}

function onDragMove(e) {
  if (!drag.active) return
  const dx = e.clientX - drag.startX
  const dy = e.clientY - drag.startY
  // 位移小于 3px 视为点击,不进入拖动
  if (!drag.moved && Math.abs(dx) < 3 && Math.abs(dy) < 3) return
  drag.moved = true
  const pos = clampPos(drag.origX + dx, drag.origY + dy, drag.target)
  if (drag.target === 'fab') fabPos.value = pos
  else panelPos.value = pos
}

function endDrag() {
  drag.active = false
  drag.target = null
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', endDrag)
}

/** 拖动范围限制在视口内 */
function clampPos(x, y, target) {
  const el = target === 'fab' ? fabEl.value : panelEl.value
  const w = el ? el.offsetWidth : 0
  const h = el ? el.offsetHeight : 0
  const maxX = Math.max(0, window.innerWidth - w)
  const maxY = Math.max(0, window.innerHeight - h)
  return { x: Math.min(Math.max(x, 0), maxX), y: Math.min(Math.max(y, 0), maxY) }
}

// ===== 方法 =====

/** 加载历史消息 */
async function loadHistory() {
  try {
    const res = await getAgentHistory(sessionId.value)
    if (res.data && Array.isArray(res.data)) {
      // 从 toolEvent("confirm:<pendingId>")里恢复未决确认卡——但只恢复最后一张:
      // 更早的 pending 已被后端作废(同 session 挂新 pending 时清除),点了也只能拿到超时
      let lastConfirmIdx = -1
      const list = res.data.map((m, i) => {
        const pendingId = m.toolEvent && m.toolEvent.startsWith('confirm:')
          ? m.toolEvent.slice('confirm:'.length)
          : null
        if (pendingId) lastConfirmIdx = i
        return {
          role: m.direction,
          content: m.content,
          needsConfirm: !!pendingId,
          pendingId,
          resolved: true
        }
      })
      if (lastConfirmIdx >= 0) {
        list[lastConfirmIdx].resolved = false
        list[lastConfirmIdx].expired = true   // 刷新回来的卡大概率已过期,文案提示 5 分钟时效
      }
      messages.value = list
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
  // 刚发生拖动时不算点击,避免拖完误开关面板
  if (drag.moved) {
    drag.moved = false
    return
  }
  isOpen.value = !isOpen.value
  if (!isOpen.value) return
  // 每次打开都查状态(admin 可能在后台切了开关)
  try {
    const res = await getAgentStatus()
    agentEnabled.value = res.data.enabled
  } catch (e) {
    // 查询失败(网络抖动)不代表已停用:保持可用,后续 chat 失败会给出真实错误
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
    // 同一会话任何时刻至多一张有效确认卡:新一轮对话已让后端作废旧 pending,
    // 旧的未决卡统一标记失效,避免之后误点旧卡(点了也只会超时,但界面先说清)
    messages.value.forEach(m => {
      if (m.needsConfirm && !m.resolved) {
        m.resolved = true
        m.resolvedText = '已失效(发起了新请求)'
      }
    })
    // 追加助手消息(表单卡:字段预填进可编辑表单,缺失槽位用户补)
    const formState = d.form && d.form.length ? buildFormState(d.form) : null
    messages.value.push({
      role: 'assistant',
      content: d.reply,
      needsConfirm: d.needsConfirm,
      pendingId: d.pendingId || null,
      resolved: !d.needsConfirm,
      form: formState ? d.form : null,
      formModel: formState ? formState.model : null,
      formRules: formState ? formState.rules : null
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

/** 后端 confirm 状态 → 确认卡结果文案。只有 executed 才是"已执行"。 */
const CONFIRM_STATUS_TEXT = {
  executed: '✅ 已执行',
  cancelled: '已取消',
  timeout: '⏰ 已超时,请重新发起',
  rejected: '⚠️ 无权确认该操作',
  error: '❌ 执行失败,请查看下方原因',
  disabled: '智能助手已停用,操作未执行'
}

/** 确认/取消写操作(表单卡先做必填校验,提交编辑值作为 args) */
async function handleConfirm(msg, accept) {
  let args
  if (accept && msg.form && msg.form.length) {
    const formRef = formRefs[msg.pendingId]
    if (formRef) {
      try {
        await formRef.validate()
      } catch (e) {
        ElMessage.warning('请先补全表单中的必填项')
        return
      }
    }
    args = collectFormArgs(msg)
  }
  msg.confirming = true
  msg.confirmError = null
  try {
    const res = await agentConfirm({
      pendingId: msg.pendingId,
      accept: accept,
      sessionId: sessionId.value,
      args: accept ? args : undefined
    })
    const status = res.data.status || (res.data.success ? 'executed' : 'error')
    // 校验未通过(pending 未被后端消费):表单卡保持可编辑并展示原因,改完可再提交;
    // 执行失败(pending 已消费)则正常关闭卡片,重试需重新发起
    if (status === 'error' && msg.form && msg.form.length
        && typeof res.data.reply === 'string' && res.data.reply.includes('[校验未通过]')) {
      msg.confirmError = res.data.reply
      return
    }
    msg.resolved = true
    msg.resolvedText = CONFIRM_STATUS_TEXT[status] || ('操作结束:' + status)
    // 追加执行结果消息
    messages.value.push({
      role: 'assistant',
      content: res.data.reply,
      needsConfirm: false, pendingId: null, resolved: true
    })
  } catch (e) {
    ElMessage.error('确认失败:' + (e.message || '请重试'))
  } finally {
    msg.confirming = false
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
  cursor: grab;
  box-shadow: var(--shadow-lg);
  z-index: 9998;
  cursor: grab;
  touch-action: none;   /* 触屏拖动时不滚动页面 */
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

/* 头部(可拖动) */
.agent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-3) var(--spacing-4);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: #fff;
  cursor: grab;
  user-select: none;      /* 拖动时不选中文本 */
  touch-action: none;     /* 触屏拖动时不滚动页面 */
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
  flex-wrap: wrap;
}
.agent-confirm__done {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}
.agent-confirm__hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

/* 表单卡 */
.agent-form {
  width: 100%;
  padding: var(--spacing-2) var(--spacing-3);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-base);
}
.agent-form :deep(.el-form-item) {
  margin-bottom: var(--spacing-2);
}
.agent-form :deep(.el-form-item__label) {
  font-size: var(--font-size-xs);
  padding-bottom: 2px;
}
.agent-form__hint {
  width: 100%;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  line-height: 1.4;
}
.agent-form__error {
  width: 100%;
  font-size: var(--font-size-xs);
  color: var(--color-danger, #f56c6c);
  line-height: 1.4;
  margin-bottom: var(--spacing-1);
  word-break: break-all;
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
