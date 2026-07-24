<template>
  <!-- 买家"我的发布"：主要管理自己发布的求购需求，复用共享组件 -->
  <MyProducts role="buyer" title="我的发布" default-type="demand" />
</template>

<script setup>
import MyProducts from '@/views/common/MyProducts.vue'
</script>
