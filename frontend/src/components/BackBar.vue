<template>
  <!--
    统一返回栏：固定左上。
    优先“回到来路”——若地址栏带 ?from= 则跳 from（进入此页时由来源页写入），
    否则回退到 props.to（逻辑父级）。这样无论从哪个页面进来都能原路返回，
    直接刷新 / 外链打开（无 from）时也有确定去处，避免 router.back() 的循环与空跳。
  -->
  <div class="back-bar">
    <el-button class="back-btn" link @click="goBack">← {{ label }}</el-button>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
  // 兜底目标：没有 from 时跳这里（字符串路径或路由对象）
  to: { type: [String, Object], default: '/' },
  // 按钮文案，如“返回”/“返回列表”
  label: { type: String, default: '返回' }
})

const router = useRouter()
const route = useRoute()

const goBack = () => {
  const from = route.query.from
  router.push(from ? from : props.to)
}
</script>

<style scoped>
.back-bar {
  text-align: left;
  margin-bottom: 16px;
}
.back-btn {
  padding: 0;
  font-size: 14px;
  font-weight: 500;
}
.back-btn:hover {
  opacity: 0.75;
}
</style>
