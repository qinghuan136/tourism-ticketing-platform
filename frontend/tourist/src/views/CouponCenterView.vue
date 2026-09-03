<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Calendar, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { pageVenues } from '../api/catalog'
import { claimCoupon, getCouponClaimResult, listCouponActivities, listMyCoupons } from '../api/coupon'
import type { CatalogVenue } from '../types/catalog'
import type { CouponActivity, CouponClaimDisplayState, UserCoupon, UserCouponStatus } from '../types/coupon'
import { getRequestErrorMessage } from '../utils/request'

type CouponTab = 'claim' | 'mine'

const route = useRoute()
const router = useRouter()
const activeTab = ref<CouponTab>(route.query.tab === 'mine' ? 'mine' : 'claim')
const venues = ref<CatalogVenue[]>([])
const selectedVenueId = ref<number | undefined>()
const activities = ref<CouponActivity[]>([])
const myCoupons = ref<UserCoupon[]>([])
const selectedCouponStatus = ref<UserCouponStatus | undefined>()
const activityLoading = ref(false)
const couponLoading = ref(false)
const claimingActivityIds = ref<number[]>([])
let disposed = false

const activityStateText: Record<CouponClaimDisplayState, string> = {
  NOT_STARTED: '未开始', IN_PROGRESS: '进行中', SOLD_OUT: '已抢完',
}
const couponStatusText: Record<UserCouponStatus, string> = {
  AVAILABLE: '可使用', LOCKED: '已锁定', USED: '已使用', EXPIRED: '已过期',
}
const couponStatusOptions: Array<{ label: string; value?: UserCouponStatus }> = [
  { label: '全部' }, { label: '可使用', value: 'AVAILABLE' }, { label: '已锁定', value: 'LOCKED' },
  { label: '已使用', value: 'USED' }, { label: '已过期', value: 'EXPIRED' },
]

async function loadVenues(): Promise<void> {
  try {
    const result = await pageVenues({ page: 1, size: 100 })
    venues.value = result.items
    const queryVenueId = Number(route.query.venueId)
    selectedVenueId.value = venues.value.some((venue) => venue.id === queryVenueId)
      ? queryVenueId
      : venues.value[0]?.id
    if (selectedVenueId.value) await loadActivities()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '景点列表加载失败'))
  }
}

async function loadActivities(): Promise<void> {
  if (!selectedVenueId.value) {
    activities.value = []
    return
  }
  activityLoading.value = true
  try {
    activities.value = await listCouponActivities(selectedVenueId.value)
  } catch (error) {
    activities.value = []
    ElMessage.error(getRequestErrorMessage(error, '优惠券活动加载失败'))
  } finally {
    activityLoading.value = false
  }
}

async function loadCoupons(): Promise<void> {
  couponLoading.value = true
  try {
    myCoupons.value = await listMyCoupons({ status: selectedCouponStatus.value })
  } catch (error) {
    myCoupons.value = []
    ElMessage.error(getRequestErrorMessage(error, '我的优惠券加载失败'))
  } finally {
    couponLoading.value = false
  }
}

async function switchTab(tab: CouponTab): Promise<void> {
  activeTab.value = tab
  await router.replace({ query: { ...route.query, tab: tab === 'mine' ? 'mine' : undefined } })
  if (tab === 'mine') await loadCoupons()
}

async function handleVenueChange(venueId: number): Promise<void> {
  await router.replace({ query: { ...route.query, venueId: String(venueId) } })
  await loadActivities()
}

async function selectCouponStatus(status?: UserCouponStatus): Promise<void> {
  selectedCouponStatus.value = status
  await loadCoupons()
}

async function handleClaim(activity: CouponActivity): Promise<void> {
  if (activity.claimState !== 'IN_PROGRESS' || claimingActivityIds.value.includes(activity.id)) return
  claimingActivityIds.value.push(activity.id)
  try {
    const accepted = await claimCoupon(activity.id)
    ElMessage.info('抢券请求已受理，正在确认结果')
    for (let attempt = 0; attempt < 10 && !disposed; attempt += 1) {
      if (attempt > 0) await new Promise((resolve) => window.setTimeout(resolve, 1000))
      const result = await getCouponClaimResult(accepted.requestId)
      if (result.status === 'PENDING') continue
      if (result.status === 'SUCCESS') {
        ElMessage.success('领取成功，优惠券已放入账户')
        await switchTab('mine')
      } else {
        ElMessage.error(failureText(result.failureReason))
      }
      return
    }
    if (!disposed) ElMessage.info('请求仍在处理中，可稍后在“我的优惠券”查看')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '抢券请求提交失败'))
  } finally {
    claimingActivityIds.value = claimingActivityIds.value.filter((id) => id !== activity.id)
  }
}

function failureText(reason: string | null): string {
  const messages: Record<string, string> = {
    SOLD_OUT: '优惠券已抢完', ACTIVITY_UNAVAILABLE: '活动当前不可领取',
    MESSAGE_SEND_FAILED: '请求发送失败，请稍后重试', MESSAGE_CONSUME_FAILED: '领取处理失败，请稍后重试',
  }
  return reason ? messages[reason] || `领取失败：${reason}` : '领取失败'
}

function formatMoney(value: number): string { return Number(value).toFixed(0) }
function formatDateTime(value: string): string { return value.replace('T', ' ').slice(0, 16) }

onMounted(async () => {
  await Promise.all([loadVenues(), loadCoupons()])
})
onBeforeUnmount(() => { disposed = true })
</script>

<template>
  <div class="coupon-page">
    <h1>优惠券中心</h1>
    <p class="subtitle">领取景点优惠券，预约下单时可直接使用</p>

    <div class="coupon-tabs">
      <button type="button" :class="{ active: activeTab === 'claim' }" @click="switchTab('claim')">领券中心</button>
      <button type="button" :class="{ active: activeTab === 'mine' }" @click="switchTab('mine')">我的优惠券</button>
    </div>

    <section v-if="activeTab === 'claim'" class="claim-section">
      <div class="venue-select">
        <label for="coupon-venue">选择景点</label>
        <el-select id="coupon-venue" v-model="selectedVenueId" placeholder="请选择景点" @change="handleVenueChange">
          <el-option v-for="venue in venues" :key="venue.id" :label="venue.name" :value="venue.id" />
        </el-select>
      </div>

      <div v-loading="activityLoading" class="coupon-list">
        <article v-for="activity in activities" :key="activity.id" class="coupon-row">
          <div class="coupon-value">
            <h2>{{ activity.name }}</h2>
            <p>满 ¥{{ formatMoney(activity.thresholdAmount) }} 可用</p>
            <strong><small>¥</small>{{ formatMoney(activity.discountAmount) }}</strong>
          </div>
          <div class="coupon-time">
            <p><el-icon><Clock /></el-icon><span>领取时间：{{ formatDateTime(activity.claimStartAt) }} 至 {{ formatDateTime(activity.claimEndAt) }}</span></p>
            <p><el-icon><Calendar /></el-icon><span>有效时间：{{ formatDateTime(activity.validFrom) }} 至 {{ formatDateTime(activity.validUntil) }}</span></p>
          </div>
          <div class="claim-action">
            <span :class="`state-${activity.claimState.toLowerCase()}`">{{ activityStateText[activity.claimState] }}</span>
            <el-button
              type="primary"
              :disabled="activity.claimState !== 'IN_PROGRESS'"
              :loading="claimingActivityIds.includes(activity.id)"
              @click="handleClaim(activity)"
            >{{ activity.claimState === 'IN_PROGRESS' ? '立即领取' : activityStateText[activity.claimState] }}</el-button>
            <small v-if="claimingActivityIds.includes(activity.id)">抢券请求处理中…</small>
          </div>
        </article>
        <el-empty v-if="!activityLoading && activities.length === 0" description="该景点暂无可领取优惠券" />
      </div>
    </section>

    <section v-else v-loading="couponLoading" class="my-section">
      <div class="status-tabs">
        <button
          v-for="option in couponStatusOptions"
          :key="option.label"
          type="button"
          :class="{ active: selectedCouponStatus === option.value }"
          @click="selectCouponStatus(option.value)"
        >{{ option.label }}</button>
      </div>
      <div class="coupon-list owned-list">
        <article v-for="coupon in myCoupons" :key="coupon.id" class="coupon-row">
          <div class="coupon-value">
            <h2>{{ coupon.couponName }}</h2>
            <p>满 ¥{{ formatMoney(coupon.thresholdAmount) }} 可用</p>
            <strong><small>¥</small>{{ formatMoney(coupon.discountAmount) }}</strong>
          </div>
          <div class="coupon-time">
            <p><b>{{ coupon.venueName }}</b></p>
            <p><el-icon><Calendar /></el-icon><span>有效时间：{{ formatDateTime(coupon.validFrom) }} 至 {{ formatDateTime(coupon.validUntil) }}</span></p>
            <p>领取时间：{{ formatDateTime(coupon.acquiredAt) }}</p>
          </div>
          <div class="claim-action"><span :class="`coupon-${coupon.status.toLowerCase()}`">{{ couponStatusText[coupon.status] }}</span></div>
        </article>
        <el-empty v-if="!couponLoading && myCoupons.length === 0" description="暂无优惠券" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.coupon-page { color: #1d2923; }
h1 { margin: 0 0 8px; font-size: 30px; }
.subtitle { margin: 0 0 24px; color: #79827e; }
.coupon-tabs { display: flex; gap: 44px; margin-bottom: 28px; border-bottom: 1px solid #dde2df; }
.coupon-tabs button, .status-tabs button { padding: 10px 4px 12px; color: #4f5b55; font-size: 15px; cursor: pointer; background: transparent; border: 0; border-bottom: 3px solid transparent; }
.coupon-tabs button.active, .status-tabs button.active { color: #e87800; font-weight: 700; border-bottom-color: #ef8610; }
.claim-section { padding: 16px; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.venue-select { display: flex; align-items: center; gap: 18px; margin-bottom: 16px; }
.venue-select label { flex: none; color: #59645e; }
.venue-select .el-select { width: min(440px, 100%); }
.coupon-list { display: grid; gap: 14px; min-height: 180px; }
.coupon-row { display: grid; grid-template-columns: 300px 1fr 190px; min-height: 164px; overflow: hidden; background: #fff; border: 1px solid #dfe4e1; border-radius: 9px; }
.coupon-value { padding: 24px 30px; border-right: 1px dashed #d8ddda; }
.coupon-value h2 { margin: 0 0 6px; font-size: 20px; }
.coupon-value p { margin: 0; color: #606b65; }
.coupon-value strong { display: block; margin-top: 9px; color: #ef7200; font-size: 43px; line-height: 1; }
.coupon-value small { margin-right: 4px; font-size: 20px; }
.coupon-time { display: flex; flex-direction: column; justify-content: center; gap: 17px; padding: 24px 34px; color: #69736e; }
.coupon-time p { display: flex; gap: 9px; align-items: center; margin: 0; }
.coupon-time .el-icon { flex: none; font-size: 19px; }
.claim-action { display: flex; flex-direction: column; align-items: stretch; justify-content: center; gap: 12px; padding: 24px; text-align: center; }
.claim-action > span { font-size: 14px; }.state-in_progress,.coupon-available { color: #17805a; }.state-not_started,.state-sold_out,.coupon-expired { color: #737d78; }.coupon-locked { color: #ba7500; }.coupon-used { color: #4c7292; }
.claim-action .el-button { min-height: 42px; margin: 0; --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
.claim-action small { color: #7a847f; }
.my-section { min-height: 280px; }
.status-tabs { display: flex; gap: 26px; overflow-x: auto; margin-bottom: 16px; scrollbar-width: none; }
.status-tabs::-webkit-scrollbar { display: none; }
.owned-list .coupon-time p:first-child { color: #25332c; }
@media (max-width: 850px) { .coupon-row { grid-template-columns: 220px 1fr; } .claim-action { grid-column: 1 / -1; flex-direction: row; align-items: center; justify-content: flex-end; padding: 14px 22px; border-top: 1px dashed #d8ddda; } .claim-action .el-button { width: 150px; } }
@media (max-width: 620px) { .coupon-tabs { gap: 30px; margin-bottom: 18px; } .claim-section { padding: 12px; } .venue-select { align-items: stretch; flex-direction: column; gap: 8px; } .venue-select .el-select { width: 100%; } .coupon-row { grid-template-columns: 1fr; } .coupon-value { padding: 20px; border-right: 0; border-bottom: 1px dashed #d8ddda; } .coupon-value strong { font-size: 36px; } .coupon-time { gap: 12px; padding: 18px 20px; font-size: 13px; } .claim-action { grid-column: auto; justify-content: space-between; padding: 14px 20px; } }
</style>
