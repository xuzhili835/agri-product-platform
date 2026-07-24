<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header class="home-header">
      <div class="header-inner">
        <div class="logo" @click="goTo('/')">
          <div class="logo-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2L12 12M12 12L8 8M12 12L16 8M12 12L12 22M12 22L8 18M12 22L16 18" />
              <circle cx="12" cy="3" r="1.5" fill="currentColor"/>
            </svg>
          </div>
          <span class="logo-text">农融汇</span>
        </div>

        <nav class="main-nav">
          <a href="#products" @click.prevent="scrollToSection('products')">农产品</a>
          <a href="#financing" @click.prevent="scrollToSection('financing')">融资服务</a>
          <a v-if="showExpertSection" href="#experts" @click.prevent="scrollToSection('experts')">专家助力</a>
          <a href="#knowledge" @click.prevent="scrollToSection('knowledge')">农业知识</a>
        </nav>

        <div class="header-actions">
          <template v-if="!userStore.isLoggedIn()">
            <button class="btn-text" @click="goTo('/login')">登录</button>
            <button class="btn-primary" @click="goTo('/register')">免费注册</button>
          </template>
          <template v-else>
            <el-dropdown @command="handleUserCommand" trigger="click">
              <div class="user-chip">
                <span class="user-name">{{ getUserDisplayName() }}</span>
                <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                  <path d="M4 6l4 4 4-4"/>
                </svg>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="getDashboardPath()">
                    个人中心
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </header>

    <!-- Hero Section：首页永久标题/导语区，始终显示。
         此前 hero 与轮播图用 v-if/v-else 互斥，刷新时 banners 初始为空会先渲染 hero、
         banners 回来后又被轮播图替换 → 闪现。现改为 hero 常驻，轮播图作为其下方独立区块。 -->
    <section class="hero-section">
      <div class="hero-bg">
        <div class="hero-pattern"></div>
      </div>
      <div class="hero-content">
        <div class="hero-text">
          <h1 class="hero-title">
            <span class="title-line">连接农户与市场</span>
            <span class="title-line title-accent">助力农业发展</span>
          </h1>
          <p class="hero-subtitle">
            融销一体的农产品服务平台 — 源源直达，融资无忧，专家伴您丰收
          </p>
          <div class="hero-actions">
            <button class="btn-hero btn-hero-primary" @click="goTo('/register')">
              <span>立即开始</span>
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M5 10h10M10 5l5 5-5 5"/>
              </svg>
            </button>
            <button class="btn-hero btn-hero-secondary" @click="scrollToSection('products')">
              浏览产品
            </button>
          </div>
        </div>
        <div class="hero-visual">
          <div class="visual-card visual-card-1">
            <div class="card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2L12 12M12 12L8 8M12 12L16 8M12 12L12 22M12 22L8 18M12 22L16 18" />
                <circle cx="12" cy="3" r="1.5" fill="currentColor"/>
                <ellipse cx="12" cy="18" rx="3" ry="5" fill="currentColor" opacity="0.3"/>
              </svg>
            </div>
            <div class="card-label">农产品交易</div>
          </div>
          <div class="visual-card visual-card-2">
            <div class="card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
                <circle cx="12" cy="12" r="2" fill="currentColor"/>
                <path d="M8 8l2-2M8 16l2 2M16 8l-2-2M16 16l-2 2" stroke-width="1"/>
              </svg>
            </div>
            <div class="card-label">融资服务</div>
          </div>
          <div class="visual-card visual-card-3">
            <div class="card-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="12" cy="8" r="3"/>
                <path d="M12 11v4M12 15l-3 3M12 15l3 3"/>
                <path d="M8 21h8a2 2 0 002-2v-5a2 2 0 00-2-2H8a2 2 0 00-2 2v5a2 2 0 002 2z"/>
                <path d="M9 6l1.5-1.5M15 6l-1.5-1.5" stroke-width="1"/>
              </svg>
            </div>
            <div class="card-label">专家咨询</div>
          </div>
        </div>
      </div>

    </section>

    <!-- Banner Carousel：有轮播图时作为 hero 下方独立区块显示（与 hero 非互斥） -->
    <!-- 仅一张轮播图时固定展示、不轮播（无箭头/指示点/自动播放），避免单图也出现空转的轮播组件 -->
    <section v-if="banners.length > 0" class="banner-section">
      <div v-if="banners.length === 1" class="banner-static">
        <img :src="banners[0].picPath" class="banner-image" :alt="banners[0].title || 'Banner'" />
      </div>
      <el-carousel v-else height="400px" :interval="5000" arrow="hover">
        <el-carousel-item v-for="banner in banners" :key="banner.bannerId">
          <img :src="banner.picPath" class="banner-image" :alt="banner.title || 'Banner'" />
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- Products Section -->
    <section id="products" class="products-section">
      <div class="section-inner">
        <div class="section-header">
          <div class="section-eyebrow">优质货源</div>
          <h2 class="section-title">精选农产品</h2>
          <p class="section-subtitle">直接从农户到您的手中，新鲜、优质、价格透明</p>
        </div>

        <div class="products-grid">
          <div v-for="product in featuredProducts" :key="product.productId"
               class="product-card"
               @click="viewProductDetail(product)">
            <div class="product-image">
              <img v-if="product.picPath" :src="product.picPath" :alt="product.title" />
              <div v-else class="image-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7,10 12,15 17,10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
              </div>
              <span class="product-tag supply">供</span>
            </div>
            <div class="product-content">
              <h3 class="product-name">{{ product.title }}</h3>
              <p class="product-desc">{{ product.content }}</p>
              <div class="product-footer">
                <div class="product-price">¥{{ product.price }}</div>
                <div class="product-seller">
                  {{ product.ownName || '农户' }}
                  <span v-if="product.ownPhone" class="home-seller-phone">· {{ product.ownPhone }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="section-more">
          <button class="btn-link" @click="goTo('/market')">
            查看更多产品
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 8h10M10 5l3 3-3 3"/>
            </svg>
          </button>
        </div>
      </div>
    </section>

    <!-- Financing Section -->
    <section id="financing" class="financing-section">
      <div class="section-inner">
        <div class="section-header">
          <div class="section-eyebrow">金融支持</div>
          <h2 class="section-title">融资服务</h2>
          <p class="section-subtitle">与多家银行合作，为农户提供便捷的融资渠道</p>
        </div>

        <div class="financing-grid">
          <div v-for="item in financingItems" :key="item.id" class="financing-card">
            <div class="financing-icon">
              <svg :width="40" :height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
              </svg>
            </div>
            <h3 class="financing-title">{{ item.title }}</h3>
            <p v-if="item.bank" class="financing-bank">{{ item.bank }}</p>
            <p class="financing-desc">{{ item.description }}</p>
            <div class="financing-features">
              <span v-for="feature in item.features" :key="feature" class="feature-tag">
                {{ feature }}
              </span>
            </div>
            <div class="financing-range">
              <span class="range-label">可贷额度</span>
              <span class="range-value">{{ item.amount }}</span>
            </div>
          </div>
        </div>

        <div class="financing-cta">
          <div class="cta-content">
            <h3>需要融资支持？</h3>
            <p>智能匹配最适合您的融资产品，快速审批，放款便捷</p>
          </div>
          <button class="btn-primary" @click="handleFinancingClick">
            申请融资
          </button>
        </div>
      </div>
    </section>

    <!-- Experts Section -->
    <section v-if="showExpertSection" id="experts" class="experts-section">
      <div class="section-inner">
        <div class="section-header">
          <div class="section-eyebrow">专业指导</div>
          <h2 class="section-title">专家助力</h2>
          <p class="section-subtitle">农业专家在线答疑，为您提供专业的技术指导</p>
        </div>

        <div class="experts-list">
          <div v-for="expert in featuredExperts" :key="expert.id" class="expert-card">
            <div class="expert-avatar">
              <img v-if="expert.avatar" :src="expert.avatar" :alt="expert.name" />
              <span v-else class="expert-avatar-fallback">{{ (expert.name || '专').charAt(0) }}</span>
              <span class="expert-status" :class="{ online: expert.online }"></span>
            </div>
            <div class="expert-info">
              <h3 class="expert-name">{{ expert.name }}</h3>
              <p class="expert-title">{{ expert.title }}</p>
              <p class="expert-specialty">{{ expert.specialty }}</p>
              <div class="expert-stats">
                <span class="stat-item">{{ expert.answers }} 回答</span>
              </div>
            </div>
            <button v-if="canConsultExpert" class="expert-action" @click="handleExpertClick(expert)">
              咨询
            </button>
          </div>
        </div>

        <div class="section-more">
          <button class="btn-link" @click="goTo('/expert-help')">
            查看所有专家
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 8h10M10 5l3 3-3 3"/>
            </svg>
          </button>
        </div>
      </div>
    </section>

    <!-- Knowledge Section -->
    <section id="knowledge" class="knowledge-section">
      <div class="section-inner">
        <div class="section-header">
          <div class="section-eyebrow">知识库</div>
          <h2 class="section-title">农业知识与问答</h2>
          <p class="section-subtitle">专业农业知识分享，专家在线答疑解惑</p>
        </div>

        <div class="knowledge-grid">
          <!-- Knowledge Articles -->
          <div class="knowledge-articles">
            <h3 class="subsection-title">最新文章</h3>
            <div class="article-list">
              <div v-for="article in knowledgeArticles" :key="article.id"
                   class="article-card" @click="viewArticle(article)">
                <div class="article-image">
                  <img :src="article.image" :alt="article.title" />
                </div>
                <div class="article-content">
                  <span class="article-category">{{ article.category }}</span>
                  <h4 class="article-title">{{ article.title }}</h4>
                  <p class="article-excerpt">{{ article.excerpt }}</p>
                  <div class="article-meta">
                    <span>{{ article.author }}</span>
                    <span>{{ article.date }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        <div class="section-more">
          <button class="btn-link" @click="goTo('/knowledge')">
            浏览更多知识
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 8h10M10 5l3 3-3 3"/>
            </svg>
          </button>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="cta-section">
      <div class="cta-inner">
        <div class="cta-content">
          <h2 class="cta-title">开始您的农业之旅</h2>
          <p class="cta-subtitle">无论您是农户还是买家，我们都能为您提供专业的服务</p>
        </div>
        <div class="cta-actions">
          <button class="btn-cta btn-cta-primary" @click="goTo('/register')">
            免费注册
          </button>
          <button class="btn-cta btn-cta-secondary" @click="goTo('/market')">
            浏览产品
          </button>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="home-footer">
      <div class="footer-inner">
        <div class="footer-section">
          <div class="footer-logo">
            <span class="logo-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M12 2v10M12 12c0 4 2 6 4 8M12 12c0 4-2 6-4 8"/>
                <path d="M12 2a3 3 0 013 3c0 1.5-1 2.5-3 3s-3 1.5-3 3a3 3 0 013 3z" fill="currentColor" opacity="0.2"/>
                <path d="M16 8a2 2 0 01-2 2h-4a2 2 0 01-2-2"/>
                <path d="M12 12v5M10 17h4"/>
                <path d="M8 4s2 1 4 1 4-1 4-1"/>
              </svg>
            </span>
            <span>农融汇</span>
          </div>
          <p class="footer-desc">连接农户与市场，助力农业发展</p>
        </div>
        <div class="footer-links">
          <div class="footer-link-group">
            <h4>产品服务</h4>
            <a href="#products">农产品交易</a>
            <a href="#financing">融资服务</a>
            <a href="#experts">专家咨询</a>
          </div>
          <div class="footer-link-group">
            <h4>关于我们</h4>
            <a href="#" @click.prevent="aboutVisible = true">平台介绍</a>
            <a href="#" @click.prevent="contactVisible = true">联系我们</a>
            <a href="#" @click.prevent="helpVisible = true">帮助中心</a>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 农融汇. All rights reserved.</p>
      </div>
    </footer>

    <!-- 平台介绍 -->
    <el-dialog v-model="aboutVisible" title="平台介绍" width="560px">
      <div class="footer-modal-content">
        <p><b>农融汇</b>是一个连接农户与市场、融销一体的农产品服务平台。</p>
        <p>我们为农户提供农产品上架与交易、融资贷款对接、农业专家咨询与农业知识等一站式服务，助力农业生产数字化、农产品销售便捷化、农村融资普惠化。</p>
        <ul>
          <li>📦 农产品交易：货源直达买家，价格透明</li>
          <li>💰 融资服务：对接合作银行，便捷申请贷款</li>
          <li>🌾 专家助力：农业专家在线答疑、预约指导</li>
          <li>📚 农业知识：专业知识库持续更新</li>
        </ul>
      </div>
      <template #footer>
        <el-button type="primary" @click="aboutVisible = false">我知道了</el-button>
      </template>
    </el-dialog>

    <!-- 联系我们 -->
    <el-dialog v-model="contactVisible" title="联系我们" width="480px">
      <div class="footer-modal-content">
        <p><span class="contact-label">客服电话：</span>400-888-0000</p>
        <p><span class="contact-label">客服邮箱：</span>support@nongronghui.com</p>
        <p><span class="contact-label">办公地址：</span>山东省青岛市农业科技产业园</p>
        <p><span class="contact-label">服务时间：</span>周一至周日 9:00 - 18:00</p>
        <p class="contact-tip">如您在使用过程中遇到任何问题，欢迎通过以上方式与我们联系，我们将尽快为您处理。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="contactVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 帮助中心 -->
    <el-dialog v-model="helpVisible" title="帮助中心" width="560px">
      <div class="footer-modal-content">
        <el-collapse>
          <el-collapse-item title="如何注册账号？" name="1">
            点击页面右上角「免费注册」，选择角色（农户 / 买家 / 专家 / 银行）并填写信息即可。专家与银行角色需上传资质材料，待管理员审核通过后即可登录。
          </el-collapse-item>
          <el-collapse-item title="农户如何申请融资？" name="2">
            登录后进入「我的融资 → 申请新融资」，选择银行融资产品并填写申请信息，提交后等待银行审批。
          </el-collapse-item>
          <el-collapse-item title="如何购买农产品？" name="3">
            在「农产品」市场浏览商品，加入购物车或直接购买，确认订单并完成支付即可。
          </el-collapse-item>
          <el-collapse-item title="忘记密码怎么办？" name="4">
            请联系平台客服（400-888-0000）核实身份后协助重置密码。
          </el-collapse-item>
        </el-collapse>
      </div>
      <template #footer>
        <el-button type="primary" @click="helpVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getBanners } from '@/api/banner'
import { getProducts } from '@/api/product'
import { getFinanceProducts } from '@/api/finance'
import { getKnowledgeList } from '@/api/knowledge'
import { getExpertList } from '@/api/expert'

const router = useRouter()
const userStore = useUserStore()

// 只有农户/买家可以咨询提问与预约专家；银行/管理员/专家在首页不展示咨询入口
const canConsultExpert = computed(() => ['farmer', 'buyer'].includes(userStore.role))
// 首页「专家助力」区块：未登录可见（对外宣传/引导注册），登录后仅农户/买家可见。
// 银行/管理员/专家不展示该区块——其角色无需浏览与咨询专家。
const showExpertSection = computed(() => !userStore.isLoggedIn() || canConsultExpert.value)

// 页脚弹窗：平台介绍 / 联系我们 / 帮助中心
const aboutVisible = ref(false)
const contactVisible = ref(false)
const helpVisible = ref(false)

// 获取用户显示名称：优先真实姓名 realName
// 此前 farmer 无 farmName / expert 无 expertName 时会落到 userName（登录账号），不符合"显示真实姓名"
const getUserDisplayName = () => {
  const userInfo = userStore.userInfo
  if (!userInfo) return ''
  const role = userStore.role
  switch (role) {
    case 'bank':
      // 银行角色显示银行名称更合适
      return userInfo.bankName || userInfo.realName || userInfo.userName
    case 'admin':
      return '管理员'
    default:
      return userInfo.realName || userInfo.userName
  }
}

// 获取用户主页路径
const getDashboardPath = () => {
  const role = userStore.role
  switch (role) {
    case 'farmer':
      return '/farmer/dashboard'
    case 'buyer':
      return '/buyer/dashboard'
    case 'expert':
      return '/expert/dashboard'
    case 'bank':
      return '/bank/dashboard'
    case 'admin':
      return '/admin/dashboard'
    default:
      return '/'
  }
}

// 处理用户命令
const handleUserCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    location.reload()
  } else {
    router.push(command)
  }
}

// 页面跳转
const goTo = (path) => {
  router.push(path)
}

// 滚动到指定区域
const scrollToSection = (id) => {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
  }
}

// 轮播图数据（来自 API）；hero 已常驻首页标题区，轮播图仅作为其下方独立区块，
// 二者不再互斥 → 刷新时无 banners 也不会闪现/替换 hero
const banners = ref([])

// 精选产品（来自 API）
const featuredProducts = ref([])

// 融资项目（来自 API）
const financingItems = ref([])

// 专家列表（来自 API）
const featuredExperts = ref([])

// 知识文章（来自 API）
const knowledgeArticles = ref([])

// 问答（来自 API）
const featuredQA = ref([])

// 点击处理
const viewProductDetail = (product) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录')
    router.push('/login')
    return
  }
  // 跳转到市场页面并查看产品详情
  router.push('/market?productId=' + product.productId)
}

const handleFinancingClick = () => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录')
    router.push('/login')
    return
  }
  if (userStore.role === 'farmer') {
    router.push('/farmer/finance')
  } else {
    ElMessage.info('融资服务仅对农户开放')
  }
}

const handleExpertClick = (expert) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录')
    router.push('/login')
    return
  }
  if (!canConsultExpert.value) {
    ElMessage.info('仅农户与买家可咨询和预约专家')
    return
  }
  // 跳转到专家详情页（可提问/预约）
  if (expert.userName) {
    router.push('/expert/' + expert.userName)
  } else {
    router.push('/expert-help')
  }
}

const viewArticle = (article) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录')
    router.push('/login')
    return
  }
  router.push('/knowledge/' + article.id)
}

const viewQA = (qa) => {
  if (!userStore.isLoggedIn()) {
    ElMessage.info('请先登录')
    router.push('/login')
    return
  }
  // TODO: 跳转到问答详情页
  ElMessage.info('查看问答：' + qa.title)
}

// 去除 HTML 标签，用于知识摘要纯文本展示（避免列表里出现 <p> 等标签）
const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// Load data from API
const loadHomeData = async () => {
  // hero 已常驻首页标题区，不再有降级页/加载占位；各区块独立 try，互不影响。
  // 轮播图拉取失败（如后端未启动）仅意味着该区块不显示，hero 标题与页面骨架照常在。
  try {
    const bannerRes = await getBanners()
    if (bannerRes.code === 200 && bannerRes.data && bannerRes.data.length > 0) {
      banners.value = bannerRes.data
    } else {
      banners.value = []
    }
  } catch (e) {
    banners.value = []
    console.error('轮播图加载失败:', e)
  }

  // 继续加载其余区块（各自独立 try，互不影响）
  try {
    // Load products (8 items as per brief)
    const productRes = await getProducts({ page: 1, pageSize: 8, type: 'goods' })
    if (productRes.code === 200 && productRes.data) {
      featuredProducts.value = productRes.data.records || productRes.data || []
    }

    // Load finance products —— 首页融资服务最多展示 3 个
    // 后端 FinanceProduct 字段为 productId/productName/bankName/introduce/money/rate/repayment，
    // 产品名作为卡片标题、银行名作为副标题分别展示
    const financeRes = await getFinanceProducts({ page: 1, pageSize: 6 })
    if (financeRes.code === 200 && financeRes.data) {
      const records = financeRes.data.records || financeRes.data || []
      financingItems.value = records.slice(0, 3).map(p => ({
        id: p.productId,
        title: p.productName || '融资产品',
        bank: p.bankName || '',
        description: p.introduce || '',
        features: [
          p.rate ? `年利率 ${p.rate}%` : null,
          p.repayment ? `${p.repayment}个月` : null
        ].filter(Boolean),
        amount: p.money ? `¥${p.money}` : '面议'
      }))
    }

    // Load knowledge articles (5 items)
    // 映射 picPath→image、content→excerpt、ownRealName→author，并只取已发布(status===1)
    const knowledgeRes = await getKnowledgeList({ page: 1, pageSize: 5 })
    if (knowledgeRes.code === 200 && knowledgeRes.data) {
      const records = knowledgeRes.data.records || knowledgeRes.data || []
      knowledgeArticles.value = records
        .filter(k => k.status === 1)
        .slice(0, 5)
        .map(k => ({
          id: k.knowledgeId || k.id,
          title: k.title,
          excerpt: stripHtml(k.content).substring(0, 80),
          image: k.picPath || '',
          category: '农业知识',
          author: k.ownRealName || k.ownName || '专家',
          date: formatDate(k.createTime)
        }))
    }

    // Load experts (4 items) —— 此前 featuredExperts 从未赋值，专家区块一直空白
    const expertRes = await getExpertList()
    if (expertRes.code === 200 && expertRes.data) {
      const list = expertRes.data || []
      featuredExperts.value = list.slice(0, 4).map(e => ({
        id: e.userName,
        userName: e.userName,
        name: e.realName || e.userName || '专家',
        title: e.position || '农业专家',
        specialty: e.profession || e.belong || '',
        // 头像来自 tb_user.avatar；无图时模板用首字母兜底，避免 broken image
        avatar: e.avatar || '',
        online: false,
        // 回答数=该专家已回答问题数（后端回填）；专家不借款，不展示星级
        answers: e.answerCount || 0
      }))
    }
  } catch (error) {
    console.error('加载首页部分数据失败:', error)
  }
}

onMounted(() => {
  loadHomeData()
})
</script>

<style scoped>
/* ===== 全局变量扩展 ===== */
.home-page {
  --color-primary: #2D5A3D;
  --color-primary-light: #4A7C59;
  --color-secondary: #C9A661;
  --color-accent: #B85C38;
  --color-bg: #F7F5F0;
  --color-text: #1F2923;
  --color-text-light: #6B7280;
  --color-border: #E5E0D8;
  --color-white: #FFFFFF;
  --font-display: 'Noto Serif SC', serif;
  --font-body: 'Noto Sans SC', sans-serif;
}

/* ===== Banner Section ===== */
.banner-section {
  width: 100%;
  margin-bottom: 0;
}

/* 单张轮播图：固定展示，高度与轮播模式一致，图片撑满 */
.banner-static {
  width: 100%;
  height: 400px;
  overflow: hidden;
}

.banner-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  image-rendering: -webkit-optimize-contrast;
  image-rendering: crisp-edges;
  image-rendering: high-quality;
}


/* ===== Header ===== */
.home-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 72px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--color-border);
  z-index: 1000;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.logo:hover {
  opacity: 0.8;
}

.logo-icon {
  width: 36px;
  height: 36px;
  color: var(--color-primary);
}

.logo-text {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  color: var(--color-primary);
}

.main-nav {
  display: flex;
  gap: 40px;
}

.main-nav a {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-light);
  transition: color 0.2s;
  position: relative;
}

.main-nav a:hover {
  color: var(--color-primary);
}

.main-nav a::after {
  content: '';
  position: absolute;
  bottom: -28px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-primary);
  transform: scaleX(0);
  transition: transform 0.2s;
}

.main-nav a:hover::after {
  transform: scaleX(1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.btn-text {
  padding: 8px 20px;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}

.btn-text:hover {
  color: var(--color-primary);
}

.btn-primary {
  padding: 10px 24px;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-white);
  background: var(--color-primary);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: var(--color-primary-dark, #1F4229);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--color-bg);
  border-radius: 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-chip:hover {
  background: var(--color-border);
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

/* ===== Hero Section ===== */
.hero-section {
  position: relative;
  padding-top: 72px;
  background: linear-gradient(180deg, #EBF5EE 0%, var(--color-bg) 100%);
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-pattern {
  position: absolute;
  top: 0;
  right: 0;
  width: 60%;
  height: 100%;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(45, 90, 61, 0.05) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(201, 166, 97, 0.08) 0%, transparent 50%);
}

.hero-content {
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 80px 32px 60px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: center;
}

.hero-text {
  max-width: 560px;
}

.hero-title {
  font-family: var(--font-display);
  font-size: 52px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--color-text);
  margin: 0 0 24px;
}

.title-line {
  display: block;
}

.title-accent {
  color: var(--color-primary);
}

.hero-subtitle {
  font-size: 18px;
  line-height: 1.6;
  color: var(--color-text-light);
  margin: 0 0 32px;
}

.hero-actions {
  display: flex;
  gap: 16px;
}

.btn-hero {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 32px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-hero-primary {
  color: var(--color-white);
  background: var(--color-primary);
  border: none;
}

.btn-hero-primary:hover {
  background: var(--color-primary-dark, #1F4229);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(45, 90, 61, 0.3);
}

.btn-hero-secondary {
  color: var(--color-primary);
  background: transparent;
  border: 2px solid var(--color-primary);
}

.btn-hero-secondary:hover {
  background: rgba(45, 90, 61, 0.1);
}

.hero-visual {
  position: relative;
  height: 400px;
}

.visual-card {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
  background: var(--color-white);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease;
}

.visual-card:hover {
  transform: translateY(-8px);
}

.visual-card-1 {
  top: 20%;
  left: 10%;
  animation: float 6s ease-in-out infinite;
}

.visual-card-2 {
  top: 40%;
  right: 15%;
  animation: float 6s ease-in-out infinite 2s;
}

.visual-card-3 {
  bottom: 10%;
  left: 30%;
  animation: float 6s ease-in-out infinite 4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.card-icon {
  width: 64px;
  height: 64px;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45, 90, 61, 0.1);
  border-radius: 16px;
}

.card-icon svg {
  width: 36px;
  height: 36px;
}

.card-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.trust-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
  padding: 32px;
  background: var(--color-white);
  border-top: 1px solid var(--color-border);
}

.trust-item {
  text-align: center;
}

.trust-value {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
}

.trust-label {
  font-size: 14px;
  color: var(--color-text-light);
  margin-top: 4px;
}

.trust-divider {
  width: 1px;
  height: 40px;
  background: var(--color-border);
}

/* ===== Sections Common ===== */
.products-section,
.financing-section,
.experts-section,
.knowledge-section {
  padding: 80px 32px;
}

.section-inner {
  max-width: 1280px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-eyebrow {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--color-secondary);
  margin-bottom: 12px;
}

.section-title {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 12px;
}

.section-subtitle {
  font-size: 16px;
  color: var(--color-text-light);
  margin: 0;
}

.section-more {
  text-align: center;
  margin-top: 32px;
}

.btn-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-primary);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 8px 16px;
  transition: gap 0.2s;
}

.btn-link:hover {
  gap: 12px;
}

/* ===== Products Section ===== */
.products-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.product-card {
  background: var(--color-white);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.product-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.product-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.product-card:hover .product-image img {
  transform: scale(1.05);
}

.product-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-white);
  background: var(--color-accent);
  border-radius: 4px;
}

.product-content {
  padding: 20px;
}

.product-category {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-secondary);
  margin-bottom: 8px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-origin {
  font-size: 14px;
  color: var(--color-text-light);
  margin: 0 0 16px;
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-price {
  display: flex;
  align-items: baseline;
}

.price-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-accent);
}

.price-unit {
  font-size: 14px;
  color: var(--color-text-light);
}

.product-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: var(--color-primary);
  background: rgba(45, 90, 61, 0.1);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.product-action:hover {
  background: var(--color-primary);
  color: var(--color-white);
}

/* ===== Financing Section ===== */
.financing-section {
  background: linear-gradient(180deg, rgba(74, 124, 89, 0.05) 0%, transparent 100%);
}

.financing-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 48px;
}

.financing-card {
  padding: 32px;
  background: var(--color-white);
  border-radius: 12px;
  border: 1px solid var(--color-border);
  transition: all 0.3s;
}

.financing-card:hover {
  border-color: var(--color-secondary);
  box-shadow: 0 8px 24px rgba(201, 166, 97, 0.15);
}

.financing-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  color: var(--color-secondary);
  background: rgba(201, 166, 97, 0.1);
  border-radius: 12px;
  margin-bottom: 20px;
}

.financing-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 4px;
}

.financing-bank {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-secondary);
  margin: 0 0 12px;
}

.financing-desc {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-light);
  margin: 0 0 16px;
}

.financing-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.feature-tag {
  padding: 4px 12px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(45, 90, 61, 0.1);
  border-radius: 4px;
}

.financing-range {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.range-label {
  font-size: 14px;
  color: var(--color-text-light);
}

.range-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-accent);
}

.financing-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32px;
  background: var(--color-primary);
  border-radius: 12px;
  color: var(--color-white);
}

.cta-content h3 {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 8px;
}

.cta-content p {
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
}

.financing-cta .btn-primary {
  background: var(--color-white);
  color: var(--color-primary);
}

.financing-cta .btn-primary:hover {
  background: rgba(255, 255, 255, 0.9);
}

/* ===== Experts Section ===== */
.experts-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.expert-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--color-white);
  border-radius: 12px;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: all 0.3s;
}

.expert-card:hover {
  border-color: var(--color-primary-light);
  box-shadow: 0 4px 16px rgba(45, 90, 61, 0.1);
}

.expert-avatar {
  position: relative;
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}

.expert-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.expert-status {
  position: absolute;
  bottom: 4px;
  right: 4px;
  width: 12px;
  height: 12px;
  background: var(--color-text-light);
  border: 2px solid var(--color-white);
  border-radius: 50%;
}

.expert-status.online {
  background: #10B981;
}

/* 无头像时用首字母圆形兜底，避免 broken image */
.expert-avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--color-primary);
  color: var(--color-white);
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 600;
}

.expert-info {
  flex: 1;
}

.expert-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 4px;
}

.expert-title {
  font-size: 13px;
  color: var(--color-secondary);
  margin: 0 0 8px;
}

.expert-specialty {
  font-size: 14px;
  color: var(--color-text-light);
  margin: 0 0 12px;
}

.expert-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-accent);
}

.expert-action {
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-white);
  background: var(--color-primary);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.expert-action:hover {
  background: var(--color-primary-dark, #1F4229);
}

/* ===== Knowledge Section ===== */
.knowledge-section {
  background: linear-gradient(180deg, rgba(201, 166, 97, 0.05) 0%, transparent 100%);
}

.knowledge-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 48px;
}

.subsection-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--color-secondary);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: var(--color-white);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.article-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.article-image {
  width: 120px;
  height: 80px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 8px;
}

.article-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.article-category {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-secondary);
  margin-bottom: 6px;
}

.article-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-excerpt {
  font-size: 13px;
  color: var(--color-text-light);
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-light);
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qa-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--color-white);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.qa-card:hover {
  border-color: var(--color-primary);
}

.qa-status {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: var(--color-text-light);
  background: var(--color-bg);
  border-radius: 50%;
  flex-shrink: 0;
}

.qa-status.answered {
  color: var(--color-primary);
  background: rgba(45, 90, 61, 0.1);
}

.qa-content {
  flex: 1;
}

.qa-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 6px;
}

.qa-question {
  font-size: 13px;
  color: var(--color-text-light);
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
}

.qa-expert {
  color: var(--color-secondary);
}

.qa-answers {
  color: var(--color-text-light);
}

/* ===== CTA Section ===== */
.cta-section {
  padding: 80px 32px;
  background: var(--color-primary);
  color: var(--color-white);
}

.cta-inner {
  max-width: 1280px;
  margin: 0 auto;
  text-align: center;
}

.cta-title {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 16px;
}

.cta-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 32px;
}

.cta-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.btn-cta {
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cta-primary {
  color: var(--color-primary);
  background: var(--color-white);
  border: none;
}

.btn-cta-primary:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
}

.btn-cta-secondary {
  color: var(--color-white);
  background: transparent;
  border: 2px solid var(--color-white);
}

.btn-cta-secondary:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* ===== Footer ===== */
.home-footer {
  padding: 48px 32px 24px;
  background: var(--color-text);
  color: var(--color-white);
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  margin-bottom: 32px;
}

.footer-section {
  max-width: 300px;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 16px;
}

.footer-logo .logo-icon {
  width: 32px;
  height: 32px;
  color: var(--color-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.footer-logo .logo-icon svg {
  width: 100%;
  height: 100%;
}

.footer-desc {
  font-size: 14px;
  opacity: 0.8;
  line-height: 1.6;
  margin: 0;
}

.footer-links {
  display: flex;
  gap: 64px;
}

.footer-link-group h4 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 16px;
}

.footer-link-group a {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 12px;
  transition: color 0.2s;
}

.footer-link-group a:hover {
  color: var(--color-white);
}

.footer-bottom {
  max-width: 1280px;
  margin: 0 auto;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  text-align: center;
}

.footer-bottom p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
}

/* 页脚弹窗内容 */
.footer-modal-content p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text, #1f2923);
}
.footer-modal-content ul {
  margin: 12px 0;
  padding-left: 20px;
}
.footer-modal-content li {
  font-size: 14px;
  line-height: 2;
  color: var(--color-text-light, #6b7280);
}
.footer-modal-content .contact-label {
  display: inline-block;
  width: 84px;
  color: var(--color-text-light, #6b7280);
}
.footer-modal-content .contact-tip {
  margin-top: 16px;
  padding: 12px;
  background: #f7f5f0;
  border-radius: 8px;
  color: var(--color-text-light, #6b7280);
  font-size: 13px;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .products-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .financing-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .experts-list {
    grid-template-columns: 1fr;
  }

  .knowledge-grid {
    grid-template-columns: 1fr;
  }

  .hero-content {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .hero-visual {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-inner {
    padding: 0 20px;
  }

  .main-nav {
    display: none;
  }

  .products-section,
  .financing-section,
  .experts-section,
  .knowledge-section {
    padding: 48px 20px;
  }

  .section-title {
    font-size: 28px;
  }

  .hero-title {
    font-size: 36px;
  }

  .products-grid {
    grid-template-columns: 1fr;
  }

  .financing-grid {
    grid-template-columns: 1fr;
  }

  .financing-cta {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .cta-actions {
    flex-direction: column;
  }

  .trust-bar {
    flex-wrap: wrap;
    gap: 24px;
  }

  .trust-divider {
    display: none;
  }

  .footer-inner {
    flex-direction: column;
    gap: 32px;
  }
}

/* Element Plus Dropdown Override */
:deep(.el-dropdown-menu) {
  font-family: var(--font-body);
}

:deep(.el-dropdown-menu__item) {
  font-size: 14px;
}
</style>
