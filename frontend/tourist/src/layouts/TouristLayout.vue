<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, Search, Tickets, UserFilled } from '@element-plus/icons-vue'
import AuthDialog from '../components/AuthDialog.vue'
import { useAuthStore } from '../stores/auth'

type AuthMode = 'login' | 'register'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const searchKeyword = ref('')
const authVisible = ref(false)
const authMode = ref<AuthMode>('login')

const navItems = [
  { key: 'home', label: '首页', icon: HomeFilled },
  { key: 'orders', label: '订单', icon: Tickets },
  { key: 'tickets', label: '票券', icon: Tickets },
  { key: 'profile', label: '我的', icon: UserFilled },
] as const

function openAuth(mode: AuthMode): void {
  authMode.value = mode
  authVisible.value = true
}

function submitSearch(): void {
  if (!searchKeyword.value.trim()) return
  router.push({ name: 'home', query: { keyword: searchKeyword.value.trim() } })
}

function showMobileSearchNotice(): void {
  router.push('/')
  ElMessage.info('请在页面顶部搜索景点')
}

async function handleNav(key: typeof navItems[number]['key']): Promise<void> {
  if (key === 'home') {
    await router.push('/')
    return
  }
  if (!authStore.hasValidSession()) {
    openAuth('login')
    return
  }
  if (key === 'orders') {
    await router.push('/orders')
    return
  }
  if (key === 'tickets') {
    await router.push('/tickets')
    return
  }
  if (key === 'profile') {
    await router.push('/profile')
    return
  }
}

async function handleAuthenticated(): Promise<void> {
  const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
    ? route.query.redirect
    : undefined
  await router.replace(redirect || '/')
}

function logout(): void {
  authStore.logout()
  ElMessage.success('已退出登录')
}

watch(
  () => route.query.login,
  (login) => {
    if (login === '1') openAuth('login')
  },
  { immediate: true },
)

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
  { immediate: true },
)
</script>

<template>
  <div class="tourist-shell">
    <header class="site-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/" aria-label="返回首页">
          <span class="brand-mark" aria-hidden="true">旅</span>
          <span class="brand-name">文旅预约</span>
        </RouterLink>

        <form class="site-search" role="search" @submit.prevent="submitSearch">
          <el-icon aria-hidden="true"><Search /></el-icon>
          <input v-model="searchKeyword" type="search" placeholder="搜索景点名称或城市" aria-label="搜索景点名称或城市">
          <button type="submit">搜索</button>
        </form>

        <button class="mobile-search-trigger" type="button" aria-label="搜索景点" @click="showMobileSearchNotice">
          <el-icon><Search /></el-icon>
        </button>

        <el-dropdown v-if="authStore.isAuthenticated" trigger="click">
          <button class="account-button" type="button">
            <span class="account-avatar">{{ authStore.identity?.loginName.slice(0, 1).toUpperCase() }}</span>
            <span class="account-name">{{ authStore.identity?.loginName }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleNav('profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <button v-else class="auth-entry" type="button" @click="openAuth('login')">
          登录 / 注册
        </button>
      </div>
    </header>

    <main class="site-content">
      <RouterView />
    </main>

    <nav class="bottom-nav" aria-label="游客端主导航">
      <button
        v-for="item in navItems"
        :key="item.key"
        type="button"
        :class="{
          active: item.key === 'home'
            ? route.name === 'home' || route.name === 'venue-detail' || route.name === 'booking' || route.name === 'payment'
            : item.key === 'orders'
              ? route.name === 'orders'
              : item.key === 'tickets'
                ? route.name === 'tickets'
                : route.name === 'profile' || route.name === 'visitors' || route.name === 'coupons'
        }"
        @click="handleNav(item.key)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <AuthDialog v-model:visible="authVisible" :mode="authMode" @authenticated="handleAuthenticated" />
  </div>
</template>

<style scoped>
.tourist-shell {
  min-height: 100vh;
  padding-bottom: 92px;
  background: #f5f6f4;
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 68px;
  background: #fff;
  border-bottom: 1px solid #e7e9e7;
}

.header-inner {
  display: flex;
  align-items: center;
  width: min(1160px, calc(100% - 40px));
  height: 100%;
  margin: 0 auto;
  gap: 24px;
}

.brand {
  display: flex;
  flex: none;
  align-items: center;
  gap: 10px;
  color: #15251e;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  width: 38px;
  height: 38px;
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  background: #ef8610;
  border-radius: 9px;
  place-items: center;
}

.brand-name {
  font-size: 20px;
  font-weight: 750;
  white-space: nowrap;
}

.site-search {
  display: flex;
  flex: 1;
  align-items: center;
  max-width: 580px;
  height: 42px;
  margin: 0 auto;
  overflow: hidden;
  color: #8a918d;
  background: #f5f6f5;
  border: 1px solid #e0e3e1;
  border-radius: 9px;
}

.site-search .el-icon {
  flex: none;
  margin-left: 14px;
  font-size: 18px;
}

.site-search input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 12px;
  color: #26332d;
  outline: 0;
  background: transparent;
  border: 0;
}

.site-search button {
  align-self: stretch;
  padding: 0 17px;
  color: #bd6500;
  font-weight: 650;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.auth-entry,
.account-button {
  min-height: 40px;
  padding: 0 15px;
  color: #cb6b00;
  font-weight: 650;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e88a1e;
  border-radius: 8px;
  white-space: nowrap;
}

.mobile-search-trigger {
  display: none;
}

.account-button {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #26332d;
  border-color: #dfe4e1;
}

.account-avatar {
  display: grid;
  width: 28px;
  height: 28px;
  color: #fff;
  font-size: 13px;
  background: #ef8610;
  border-radius: 50%;
  place-items: center;
}

.site-content {
  width: min(1160px, calc(100% - 40px));
  margin: 0 auto;
  padding: 24px 0;
}

.bottom-nav {
  position: fixed;
  bottom: 16px;
  left: 50%;
  z-index: 15;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  width: min(620px, calc(100vw - 28px));
  height: 64px;
  overflow: hidden;
  background: rgb(255 255 255 / 96%);
  border: 1px solid #e2e5e3;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgb(24 38 30 / 14%);
  transform: translateX(-50%);
}

.bottom-nav button {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: #6f7974;
  font-size: 13px;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.bottom-nav button + button {
  border-left: 1px solid #edf0ee;
}

.bottom-nav button.active {
  color: #e07800;
  font-weight: 650;
}

.bottom-nav .el-icon {
  font-size: 21px;
}

@media (max-width: 680px) {
  .tourist-shell {
    padding-bottom: 72px;
  }

  .site-header {
    height: 58px;
  }

  .header-inner,
  .site-content {
    width: calc(100% - 24px);
  }

  .header-inner {
    gap: 12px;
  }

  .brand-mark {
    width: 34px;
    height: 34px;
    font-size: 19px;
  }

  .brand-name {
    font-size: 17px;
  }

  .site-search {
    display: none;
  }

  .mobile-search-trigger {
    display: grid;
    width: 38px;
    height: 38px;
    margin-left: auto;
    padding: 0;
    color: #20352b;
    cursor: pointer;
    background: transparent;
    border: 0;
    place-items: center;
  }

  .mobile-search-trigger .el-icon {
    font-size: 24px;
  }

  .auth-entry {
    min-height: auto;
    padding: 7px 0;
    color: #21352b;
    background: transparent;
    border: 0;
  }

  .account-button {
    min-height: 36px;
    padding: 0;
    background: transparent;
    border: 0;
  }

  .account-name {
    display: none;
  }

  .site-content {
    padding: 14px 0 20px;
  }

  .bottom-nav {
    bottom: 0;
    width: 100%;
    height: 62px;
    border-right: 0;
    border-bottom: 0;
    border-left: 0;
    border-radius: 0;
    box-shadow: 0 -4px 18px rgb(24 38 30 / 8%);
  }
}
</style>
