<template>
  <MainLayout
    sidebar-title="买家工作台"
    :menu-items="buyerMenuItems"
  />
</template>

<script setup>
import MainLayout from './MainLayout.vue'

const buyerMenuItems = [
  {
    path: '/buyer/dashboard',
    title: '数据概览',
    icon: 'DataAnalysis'
  },
  {
    path: '/buyer/market',
    title: '农产品市场',
    icon: 'ShoppingBag'
  },
  {
    path: '/buyer/my-products',
    title: '我的发布',
    icon: 'Goods'
  },
  {
    path: '/buyer/cart',
    title: '购物车',
    icon: 'ShoppingCart'
  },
  {
    path: '/buyer/orders',
    title: '我的订单',
    icon: 'DocumentChecked'
  },
  {
    path: '/buyer/expert',
    title: '专家咨询',
    icon: 'User'
  },
  {
    path: '/buyer/profile',
    title: '个人中心',
    icon: 'UserFilled'
  }
]
</script>
