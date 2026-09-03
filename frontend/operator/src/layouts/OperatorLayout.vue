<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  CircleCheck,
  Document,
  Discount,
  Fold,
  HomeFilled,
  Location,
  Menu as MenuIcon,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import type { OperatorAppRole } from '../types/auth'

interface NavigationItem {
  label: string
  icon: typeof HomeFilled
  routeName?: string
  roles: OperatorAppRole[]
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const mobileNavigationVisible = ref(false)

const navigationItems: NavigationItem[] = [
  { label: '数据看板', icon: HomeFilled, routeName: 'dashboard', roles: ['OPERATOR'] },
  { label: '景点信息', icon: Location, routeName: 'venue', roles: ['OPERATOR'] },
  { label: '场次管理', icon: Calendar, routeName: 'sessions', roles: ['OPERATOR'] },
  { label: '票种管理', icon: Tickets, routeName: 'ticket-types', roles: ['OPERATOR'] },
  { label: '优惠券活动', icon: Discount, routeName: 'coupons', roles: ['OPERATOR'] },
  { label: '订单查询', icon: Document, routeName: 'orders', roles: ['OPERATOR', 'STAFF'] },
  { label: '核销记录', icon: CircleCheck, routeName: 'verification-records', roles: ['OPERATOR'] },
  { label: '工作人员', icon: User, routeName: 'staff', roles: ['OPERATOR'] },
  { label: '核销工作台', icon: CircleCheck, routeName: 'verification', roles: ['STAFF'] },
]

const currentRole = computed<OperatorAppRole>(() => authStore.identity?.roleCode ?? 'OPERATOR')
const visibleNavigationItems = computed(() => (
  navigationItems.filter((item) => item.roles.includes(currentRole.value))
))
const roleLabel = computed(() => (currentRole.value === 'OPERATOR' ? '运营者' : '工作人员'))
const sidebarCaption = computed(() => (
  currentRole.value === 'OPERATOR' ? '景点运营工作台' : '入园核销工作台'
))
const venueLabel = computed(() => (
  authStore.identity?.venueId ? `景点 #${authStore.identity.venueId}` : '未绑定景点'
))
const avatarText = computed(() => (
  authStore.identity?.loginName.slice(0, 1).toUpperCase() ?? '运'
))

function isActive(item: NavigationItem): boolean {
  return item.routeName !== undefined && route.name === item.routeName
}

async function handleNavigation(item: NavigationItem): Promise<void> {
  mobileNavigationVisible.value = false
  if (!item.routeName) {
    ElMessage.info(`${item.label}模块尚未开发`)
    return
  }
  if (route.name !== item.routeName) {
    await router.push({ name: item.routeName })
  }
}

async function handleAccountCommand(command: string): Promise<void> {
  if (command !== 'logout') return
  authStore.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="operator-shell">
    <header class="topbar">
      <div class="brand-area">
        <button
          class="mobile-menu-button"
          type="button"
          aria-label="打开主导航"
          @click="mobileNavigationVisible = true"
        >
          <el-icon><MenuIcon /></el-icon>
        </button>

        <button class="brand" type="button" @click="router.push({ name: 'dashboard' })">
          <span class="brand-mark" aria-hidden="true">
            <el-icon><Tickets /></el-icon>
          </span>
          <span class="brand-name">文旅票务运营端</span>
        </button>
      </div>

      <div class="venue-context">
        <span class="context-label">当前景点：</span>
        <strong>{{ venueLabel }}</strong>
      </div>

      <el-dropdown class="account-dropdown" trigger="click" @command="handleAccountCommand">
        <button class="account-menu" type="button" aria-label="打开账号菜单">
          <el-avatar :size="34">{{ avatarText }}</el-avatar>
          <span class="account-identity">{{ roleLabel }} · {{ authStore.identity?.loginName }}</span>
          <span class="menu-chevron" aria-hidden="true">⌄</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>{{ roleLabel }}账号</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <aside class="sidebar">
      <div class="sidebar-caption">{{ sidebarCaption }}</div>
      <nav class="sidebar-nav" aria-label="运营端主导航">
        <button
          v-for="item in visibleNavigationItems"
          :key="item.label"
          type="button"
          class="nav-item"
          :class="{ active: isActive(item), pending: !item.routeName }"
          :aria-current="isActive(item) ? 'page' : undefined"
          @click="handleNavigation(item)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </aside>

    <main class="workspace">
      <RouterView />
    </main>

    <el-drawer
      v-model="mobileNavigationVisible"
      direction="ltr"
      size="280px"
      class="mobile-navigation"
      :show-close="false"
    >
      <template #header>
        <div class="drawer-heading">
          <span>{{ sidebarCaption }}</span>
          <button type="button" aria-label="关闭主导航" @click="mobileNavigationVisible = false">
            <el-icon><Fold /></el-icon>
          </button>
        </div>
      </template>

      <nav class="drawer-nav" aria-label="移动端主导航">
        <button
          v-for="item in visibleNavigationItems"
          :key="item.label"
          type="button"
          class="drawer-nav-item"
          :class="{ active: isActive(item), pending: !item.routeName }"
          :aria-current="isActive(item) ? 'page' : undefined"
          @click="handleNavigation(item)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </el-drawer>
  </div>
</template>

<style scoped>
.operator-shell {
  --brand: #e99000;
  --sidebar: #253341;
  --workspace: #f4f6f9;
  --text: #202733;

  display: grid;
  grid-template-columns: 244px minmax(0, 1fr);
  grid-template-rows: 68px minmax(calc(100vh - 68px), auto);
  min-height: 100vh;
  color: var(--text);
  background: var(--workspace);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: 244px 1fr auto;
  align-items: center;
  min-width: 0;
  color: #fff;
  background: var(--brand);
  box-shadow: 0 2px 10px rgb(68 48 15 / 12%);
}

.brand-area,
.brand {
  display: flex;
  align-items: center;
  height: 100%;
}

.brand-area {
  border-right: 1px solid rgb(255 255 255 / 14%);
}

.brand {
  gap: 12px;
  width: 100%;
  padding: 0 28px;
  font: inherit;
  color: #fff;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.brand-mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: var(--brand);
  background: #fff;
  border-radius: 8px;
}

.brand-mark .el-icon {
  font-size: 20px;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}

.mobile-menu-button {
  display: none;
}

.venue-context {
  display: flex;
  gap: 5px;
  align-items: center;
  min-width: 0;
  padding-left: 30px;
  font-size: 15px;
}

.context-label {
  color: rgb(255 255 255 / 82%);
}

.account-dropdown {
  align-self: stretch;
  height: 100%;
}

.account-menu {
  display: flex;
  gap: 10px;
  align-items: center;
  height: 100%;
  padding: 0 28px;
  font: inherit;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.account-menu:hover {
  background: rgb(255 255 255 / 10%);
}

.account-menu :deep(.el-avatar) {
  color: #fff;
  background: rgb(255 255 255 / 15%);
  border: 1px solid rgb(255 255 255 / 75%);
}

.menu-chevron {
  margin-left: 2px;
  font-size: 16px;
}

.sidebar {
  min-height: calc(100vh - 68px);
  color: #d9e0e7;
  background: var(--sidebar);
  box-shadow: inset -1px 0 rgb(255 255 255 / 5%);
}

.sidebar-caption {
  padding: 24px 28px 18px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: #8f9dab;
}

.sidebar-nav {
  display: grid;
  gap: 5px;
  padding: 0 12px;
}

.nav-item {
  position: relative;
  display: flex;
  gap: 14px;
  align-items: center;
  width: 100%;
  min-height: 52px;
  padding: 0 16px;
  font: inherit;
  font-size: 15px;
  color: #d9e0e7;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 7px;
  transition: background-color 160ms ease, color 160ms ease;
}

.nav-item .el-icon {
  flex: 0 0 auto;
  font-size: 19px;
}

.nav-item:hover {
  color: #fff;
  background: rgb(255 255 255 / 7%);
}

.nav-item.pending {
  color: #b2beca;
}

.nav-item.active {
  color: #ffad1f;
  background: rgb(255 255 255 / 9%);
}

.nav-item.active::before {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -12px;
  width: 3px;
  content: '';
  background: #f2a000;
}

.workspace {
  min-width: 0;
  padding: 30px 32px 44px;
  overflow: auto;
}

:global(.mobile-navigation.el-drawer) {
  color: #e4e9ee;
  background: #253341;
}

:global(.mobile-navigation .el-drawer__header) {
  padding: 22px 20px;
  margin: 0;
  color: #fff;
  border-bottom: 1px solid rgb(255 255 255 / 9%);
}

:global(.mobile-navigation .el-drawer__body) {
  padding: 14px 12px;
}

.drawer-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  font-size: 15px;
  font-weight: 700;
}

.drawer-heading button {
  display: grid;
  padding: 8px;
  font-size: 20px;
  color: #d5dce3;
  cursor: pointer;
  background: transparent;
  border: 0;
  place-items: center;
}

.drawer-nav {
  display: grid;
  gap: 5px;
}

.drawer-nav-item {
  display: flex;
  gap: 14px;
  align-items: center;
  min-height: 50px;
  padding: 0 15px;
  font: inherit;
  font-size: 15px;
  color: #d9e0e7;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 7px;
}

.drawer-nav-item .el-icon {
  font-size: 19px;
}

.drawer-nav-item.pending {
  color: #aeb9c4;
}

.drawer-nav-item.active {
  color: #ffad1f;
  background: rgb(255 255 255 / 9%);
}

@media (max-width: 1180px) {
  .operator-shell {
    grid-template-columns: 82px minmax(0, 1fr);
  }

  .topbar {
    grid-template-columns: 82px 1fr auto;
  }

  .brand {
    justify-content: center;
    padding: 0;
  }

  .brand-name,
  .sidebar-caption,
  .nav-item span {
    display: none;
  }

  .sidebar-nav {
    padding: 14px 8px;
  }

  .nav-item {
    justify-content: center;
    padding: 0;
  }

  .nav-item.active::before {
    left: -8px;
  }
}

@media (max-width: 680px) {
  .operator-shell {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: 60px minmax(calc(100vh - 60px), auto);
  }

  .topbar {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .brand-area {
    min-width: 0;
    border-right: 0;
  }

  .mobile-menu-button {
    display: grid;
    width: 52px;
    height: 60px;
    place-items: center;
    font-size: 21px;
    color: #fff;
    cursor: pointer;
    background: transparent;
    border: 0;
  }

  .brand {
    width: auto;
    padding: 0 6px;
  }

  .brand-name,
  .venue-context,
  .sidebar {
    display: none;
  }

  .account-dropdown {
    min-width: 0;
    justify-self: end;
  }

  .account-menu {
    max-width: 230px;
    padding: 0 12px;
    font-size: 13px;
  }

  .account-identity {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .workspace {
    padding: 22px 16px 34px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .nav-item {
    transition: none;
  }
}
</style>
