import { createRouter, createWebHistory, type RouteLocationRaw } from 'vue-router'
import OperatorLayout from '../layouts/OperatorLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import { useAuthStore } from '../stores/auth'
import type { OperatorAppRole } from '../types/auth'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    roles?: OperatorAppRole[]
    title?: string
  }
}

export function getRoleHomeRoute(role: OperatorAppRole): RouteLocationRaw {
  return role === 'OPERATOR' ? { name: 'dashboard' } : { name: 'verification' }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: OperatorLayout,
      meta: { roles: ['OPERATOR', 'STAFF'] },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: DashboardView,
          meta: { roles: ['OPERATOR'], title: '运营数据看板' },
        },
        {
          path: 'staff',
          name: 'staff',
          component: () => import('../views/StaffListView.vue'),
          meta: { roles: ['OPERATOR'], title: '工作人员管理' },
        },
        {
          path: 'venue',
          name: 'venue',
          component: () => import('../views/VenueView.vue'),
          meta: { roles: ['OPERATOR'], title: '景点信息' },
        },
        {
          path: 'ticket-types',
          name: 'ticket-types',
          component: () => import('../views/TicketTypeListView.vue'),
          meta: { roles: ['OPERATOR'], title: '票种管理' },
        },
        {
          path: 'sessions',
          name: 'sessions',
          component: () => import('../views/SessionListView.vue'),
          meta: { roles: ['OPERATOR'], title: '场次管理' },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('../views/OrderListView.vue'),
          meta: { roles: ['OPERATOR', 'STAFF'], title: '订单查询' },
        },
        {
          path: 'verification',
          name: 'verification',
          component: () => import('../views/VerificationView.vue'),
          meta: { roles: ['OPERATOR', 'STAFF'], title: '核销工作台' },
        },
        {
          path: 'verification-records',
          name: 'verification-records',
          component: () => import('../views/VerificationRecordView.vue'),
          meta: { roles: ['OPERATOR'], title: '核销记录' },
        },
        {
          path: 'coupons',
          name: 'coupons',
          component: () => import('../views/CouponActivityView.vue'),
          meta: { roles: ['OPERATOR'], title: '优惠券活动' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (to.name === 'login' && authStore.hasValidSession() && authStore.identity) {
      return getRoleHomeRoute(authStore.identity.roleCode)
    }
    return true
  }

  if (!authStore.hasValidSession() || !authStore.identity) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  const allowedRoles = to.meta.roles
  if (allowedRoles && !allowedRoles.includes(authStore.identity.roleCode)) {
    return getRoleHomeRoute(authStore.identity.roleCode)
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 文旅票务运营端`
    : '文旅票务运营端'
})

export default router
