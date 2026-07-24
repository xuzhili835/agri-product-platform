<template>
  <MainLayout
    sidebar-title="专家工作台"
    :menu-items="expertMenuItems"
  />
</template>

<script setup>
import MainLayout from './MainLayout.vue'

const expertMenuItems = [
  {
    path: '/expert/dashboard',
    title: '数据概览',
    icon: 'DataAnalysis'
  },
  {
    path: '/expert/knowledge',
    title: '知识发布',
    icon: 'EditPen'
  },
  {
    path: '/expert/questions',
    title: '问答管理',
    icon: 'ChatDotRound'
  },
  {
    path: '/expert/reservations',
    title: '预约管理',
    icon: 'Calendar'
  },
  {
    path: '/expert/profile',
    title: '个人中心',
    icon: 'UserFilled'
  }
]
</script>
