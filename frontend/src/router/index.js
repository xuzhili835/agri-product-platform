/**
 * 路由配置
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/home',
    redirect: '/'
  },
  // 农户路由
  {
    path: '/farmer',
    component: () => import('@/layouts/FarmerLayout.vue'),
    meta: { requiresAuth: true, roles: ['farmer'] },
    children: [
      {
        path: '',
        redirect: '/farmer/dashboard'
      },
      {
        path: 'dashboard',
        name: 'FarmerDashboard',
        component: () => import('@/views/farmer/Dashboard.vue')
      },
      {
        path: 'products',
        name: 'FarmerProducts',
        component: () => import('@/views/farmer/Products.vue')
      },
      {
        path: 'market',
        name: 'FarmerMarket',
        component: () => import('@/views/Market.vue')
      },
      {
        path: 'orders',
        name: 'FarmerOrders',
        component: () => import('@/views/farmer/Orders.vue')
      },
      {
        path: 'finance',
        name: 'FarmerFinance',
        component: () => import('@/views/farmer/Finance.vue')
      },
      {
        path: 'my-finance',
        name: 'FarmerMyFinance',
        component: () => import('@/views/MyFinance.vue')
      },
      {
        path: 'expert',
        name: 'FarmerExpert',
        component: () => import('@/views/common/ExpertWorkspace.vue')
      },
      {
        path: 'profile',
        name: 'FarmerProfile',
        component: () => import('@/views/farmer/Profile.vue')
      }
    ]
  },
  // 买家路由
  {
    path: '/buyer',
    component: () => import('@/layouts/BuyerLayout.vue'),
    meta: { requiresAuth: true, roles: ['buyer'] },
    children: [
      {
        path: '',
        redirect: '/buyer/dashboard'
      },
      {
        path: 'dashboard',
        name: 'BuyerDashboard',
        component: () => import('@/views/buyer/Dashboard.vue')
      },
      {
        path: 'market',
        name: 'BuyerMarket',
        component: () => import('@/views/Market.vue')
      },
      {
        path: 'cart',
        name: 'BuyerCart',
        component: () => import('@/views/buyer/Cart.vue')
      },
      {
        path: 'checkout',
        name: 'BuyerCheckout',
        component: () => import('@/views/buyer/Checkout.vue')
      },
      {
        path: 'orders',
        name: 'BuyerOrders',
        component: () => import('@/views/OrderList.vue')
      },
      {
        path: 'my-products',
        name: 'BuyerMyProducts',
        component: () => import('@/views/buyer/Products.vue')
      },
      {
        path: 'expert',
        name: 'BuyerExpert',
        component: () => import('@/views/common/ExpertWorkspace.vue')
      },
      {
        path: 'profile',
        name: 'BuyerProfile',
        component: () => import('@/views/buyer/Profile.vue')
      }
    ]
  },
  // 专家路由
  {
    path: '/expert',
    component: () => import('@/layouts/ExpertLayout.vue'),
    meta: { requiresAuth: true, roles: ['expert'] },
    children: [
      {
        path: '',
        redirect: '/expert/dashboard'
      },
      {
        path: 'dashboard',
        name: 'ExpertDashboard',
        component: () => import('@/views/expert/Dashboard.vue')
      },
      {
        path: 'knowledge',
        name: 'ExpertKnowledge',
        component: () => import('@/views/expert/Knowledge.vue')
      },
      {
        path: 'questions',
        name: 'ExpertQuestions',
        component: () => import('@/views/expert/Questions.vue')
      },
      {
        path: 'reservations',
        name: 'ExpertReservations',
        component: () => import('@/views/expert/Reservations.vue')
      },
      {
        path: 'profile',
        name: 'ExpertProfile',
        component: () => import('@/views/expert/Profile.vue')
      }
    ]
  },
  // 银行路由
  {
    path: '/bank',
    component: () => import('@/layouts/BankLayout.vue'),
    meta: { requiresAuth: true, roles: ['bank'] },
    children: [
      {
        path: '',
        redirect: '/bank/dashboard'
      },
      {
        path: 'dashboard',
        name: 'BankDashboard',
        component: () => import('@/views/bank/Dashboard.vue')
      },
      {
        path: 'approvals',
        name: 'BankApprovals',
        component: () => import('@/views/bank/Approvals.vue')
      },
      {
        path: 'matching',
        name: 'BankMatching',
        component: () => import('@/views/bank/Matching.vue')
      },
      {
        path: 'repayments',
        name: 'BankRepayments',
        component: () => import('@/views/bank/Repayments.vue')
      },
      {
        path: 'products',
        name: 'BankProducts',
        component: () => import('@/views/bank/Products.vue')
      }
    ]
  },
  // 管理员路由
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: ['admin'] },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboard.vue')
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUser.vue')
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/AdminProduct.vue')
      },
      {
        path: 'finances',
        name: 'AdminFinances',
        component: () => import('@/views/admin/AdminFinance.vue')
      },
      {
        path: 'experts',
        name: 'AdminExperts',
        component: () => import('@/views/admin/Experts.vue')
      },
      {
        path: 'applications',
        name: 'AdminApplications',
        component: () => import('@/views/admin/Applications.vue')
      },
      {
        path: 'banners',
        name: 'AdminBanners',
        component: () => import('@/views/admin/AdminBanner.vue')
      }
    ]
  },
  // 公共交易页面（所有角色可见）
  {
    path: '/market',
    name: 'Market',
    component: () => import('@/views/Market.vue'),
    meta: { requiresAuth: false }
  },
  // 商品页面（所有角色可见）
  {
    path: '/product',
    name: 'ProductList',
    component: () => import('@/views/Product.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/ProductDetail.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/finance-products',
    name: 'FinanceProducts',
    component: () => import('@/views/FinanceProducts.vue'),
    meta: { requiresAuth: false }
  },
  // 融资服务页面
  {
    path: '/finance',
    name: 'Finance',
    component: () => import('@/views/Finance.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/finance/apply',
    name: 'FinanceApply',
    component: () => import('@/views/FinanceApply.vue'),
    meta: { requiresAuth: true, roles: ['farmer'] }
  },
  {
    path: '/finance/my',
    redirect: '/farmer/my-finance'
  },
  {
    path: '/expert-help',
    name: 'ExpertHelp',
    component: () => import('@/views/ExpertHelp.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/expert/:userName',
    name: 'ExpertDetail',
    component: () => import('@/views/ExpertDetail.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/knowledge',
    name: 'KnowledgeList',
    component: () => import('@/views/KnowledgeList.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/knowledge/:id',
    name: 'KnowledgeDetail',
    component: () => import('@/views/KnowledgeDetail.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/knowledge/publish',
    name: 'KnowledgePublish',
    component: () => import('@/views/KnowledgePublish.vue'),
    meta: { requiresAuth: true, roles: ['expert', 'admin'] }
  },
  // 问答页面
  {
    path: '/question',
    name: 'QuestionList',
    component: () => import('@/views/QuestionList.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/question/ask',
    name: 'QuestionAsk',
    component: () => import('@/views/QuestionAsk.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/question/:id',
    name: 'QuestionDetail',
    component: () => import('@/views/QuestionDetail.vue'),
    meta: { requiresAuth: true }
  },
  // 预约专家页面
  {
    path: '/reserve',
    name: 'ReserveExpert',
    component: () => import('@/views/ReserveExpert.vue'),
    meta: { requiresAuth: false }
  },
  // 购物车和订单页面
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/Cart.vue'),
    meta: { requiresAuth: true, roles: ['buyer'] }
  },
  {
    path: '/order',
    name: 'OrderList',
    component: () => import('@/views/OrderList.vue'),
    meta: { requiresAuth: true, roles: ['buyer'] }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetail.vue'),
    meta: { requiresAuth: true, roles: ['buyer'] }
  },
  // 个人中心页面（所有认证用户）
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile/edit',
    name: 'ProfileEdit',
    component: () => import('@/views/ProfileEdit.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/role-application',
    name: 'RoleApplication',
    component: () => import('@/views/RoleApplication.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/address',
    name: 'AddressManage',
    component: () => import('@/views/AddressManage.vue'),
    meta: { requiresAuth: true }
  },
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn()) {
      // 未登录，跳转到登录页
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else if (to.meta.roles && !userStore.hasRole(to.meta.roles)) {
      // 已登录但没有权限
      next({ name: 'Home' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
