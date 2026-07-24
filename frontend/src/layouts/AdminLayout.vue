<template>
  <MainLayout
    sidebar-title="管理后台"
    :menu-items="adminMenuItems"
  />
</template>

<script setup>
import MainLayout from './MainLayout.vue'

const adminMenuItems = [
  {
    path: '/admin/dashboard',
    title: '数据概览',
    icon: 'DataAnalysis'
  },
  {
    path: '/admin/users',
    title: '用户管理',
    icon: 'User'
  },
  {
    path: '/admin/products',
    title: '商品管理',
    icon: 'Goods'
  },
  {
    path: '/admin/finances',
    title: '融资管理',
    icon: 'Coin'
  },
  {
    path: '/admin/experts',
    title: '专家管理',
    icon: 'Avatar'
  },
  {
    path: '/admin/applications',
    title: '申请审核',
    icon: 'Stamp'
  },
  {
    path: '/admin/banners',
    title: '轮播图管理',
    icon: 'Picture'
  }
]
</script>
