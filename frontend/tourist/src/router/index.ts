import { createRouter, createWebHistory } from 'vue-router'
import TouristLayout from '../layouts/TouristLayout.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: TouristLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('../views/HomeView.vue'),
        },
        {
          path: 'venues/:venueId',
          name: 'venue-detail',
          component: () => import('../views/VenueDetailView.vue'),
        },
        {
          path: 'visitors',
          name: 'visitors',
          component: () => import('../views/VisitorListView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'booking',
          name: 'booking',
          component: () => import('../views/BookingView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'payment/:orderId',
          name: 'payment',
          component: () => import('../views/PaymentView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('../views/OrderListView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'tickets',
          name: 'tickets',
          component: () => import('../views/TicketListView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'coupons',
          name: 'coupons',
          component: () => import('../views/CouponCenterView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/ProfileView.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (!to.meta.requiresAuth) return true
  const authStore = useAuthStore()
  if (authStore.hasValidSession()) return true
  return {
    name: 'home',
    query: { login: '1', redirect: to.fullPath },
  }
})

export default router
