<template>
  <MainLayout
    sidebar-title="农户工作台"
    :menu-items="farmerMenuItems"
  />
</template>

<script setup>
import MainLayout from './MainLayout.vue'

const farmerMenuItems = [
  {
    path: '/farmer/dashboard',
    title: '数据概览',
    icon: 'DataAnalysis'
  },
  {
    path: '/farmer/products',
    title: '我的发布',
    icon: 'Goods'
  },
  {
    path: '/farmer/market',
    title: '农产品市场',
    icon: 'ShoppingBag'
  },
  {
    path: '/farmer/orders',
    title: '订单管理',
    icon: 'DocumentChecked'
  },
  {
    path: '/farmer/finance',
    title: '融资申请',
    icon: 'Coin'
  },
  {
    path: '/farmer/my-finance',
    title: '我的融资',
    icon: 'Wallet'
  },
  {
    path: '/farmer/expert',
    title: '专家咨询',
    icon: 'User'
  },
  {
    path: '/farmer/profile',
    title: '个人中心',
    icon: 'UserFilled'
  }
]
</script>
