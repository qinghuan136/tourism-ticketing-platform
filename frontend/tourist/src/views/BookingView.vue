<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getVenue, listVenueSessions } from '../api/catalog'
import { listMyCoupons } from '../api/coupon'
import { createOrder } from '../api/order'
import { listVisitors } from '../api/visitor'
import type { CatalogVenue, SellableSession, SellableTicketType } from '../types/catalog'
import type { UserCoupon } from '../types/coupon'
import type { Visitor } from '../types/visitor'
import { getRequestErrorMessage } from '../utils/request'

const route = useRoute()
const router = useRouter()
const venue = ref<CatalogVenue | null>(null)
const session = ref<SellableSession | null>(null)
const visitors = ref<Visitor[]>([])
const coupons = ref<UserCoupon[]>([])
const selectedCouponId = ref<number | undefined>()
const selectedVisitorIds = ref<number[]>([])
const ticketTypeByVisitor = reactive<Record<number, number | undefined>>({})
const loading = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const venueId = computed(() => Number(route.query.venueId))
const sessionId = computed(() => Number(route.query.sessionId))
const visitDate = computed(() => typeof route.query.visitDate === 'string' ? route.query.visitDate : '')

const selectedItems = computed(() => selectedVisitorIds.value.map((visitorId) => {
  const visitor = visitors.value.find((item) => item.id === visitorId)
  const ticketType = session.value?.ticketTypes.find(
    (item) => item.sessionTicketTypeId === ticketTypeByVisitor[visitorId],
  )
  return visitor && ticketType ? { visitor, ticketType } : null
}).filter((item): item is { visitor: Visitor; ticketType: SellableTicketType } => item !== null))

const totalAmount = computed(() => selectedItems.value.reduce((total, item) => total + Number(item.ticketType.salePrice), 0))
const selectedCoupon = computed(() => coupons.value.find((coupon) => coupon.id === selectedCouponId.value))
const estimatedAmount = computed(() => Math.max(0, totalAmount.value - Number(selectedCoupon.value?.discountAmount ?? 0)))

async function loadBooking(): Promise<void> {
  if (!Number.isSafeInteger(venueId.value) || venueId.value <= 0
    || !Number.isSafeInteger(sessionId.value) || sessionId.value <= 0
    || !visitDate.value) {
    errorMessage.value = '预约参数不完整，请重新选择场次'
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const [venueResult, sessionResult, visitorResult, couponResult] = await Promise.all([
      getVenue(venueId.value),
      listVenueSessions(venueId.value, visitDate.value),
      listVisitors('ACTIVE'),
      listMyCoupons({ venueId: venueId.value, status: 'AVAILABLE' }),
    ])
    const selectedSession = sessionResult.find((item) => item.id === sessionId.value)
    if (!selectedSession || selectedSession.saleState !== 'ON_SALE') {
      throw new Error('该场次当前不可预约，请重新选择')
    }
    venue.value = venueResult
    session.value = selectedSession
    visitors.value = visitorResult
    coupons.value = couponResult
  } catch (error) {
    errorMessage.value = getRequestErrorMessage(error, '预约信息加载失败')
  } finally {
    loading.value = false
  }
}

function toggleVisitor(visitorId: number, checked: boolean): void {
  if (checked) {
    selectedVisitorIds.value.push(visitorId)
    ticketTypeByVisitor[visitorId] = availableTicketTypes.value[0]?.sessionTicketTypeId
    return
  }
  selectedVisitorIds.value = selectedVisitorIds.value.filter((id) => id !== visitorId)
  delete ticketTypeByVisitor[visitorId]
}

const availableTicketTypes = computed(() => session.value?.ticketTypes.filter(
  (item) => item.remainingQuantity === null || item.remainingQuantity > 0,
) ?? [])

async function submitOrder(): Promise<void> {
  if (!session.value || selectedVisitorIds.value.length === 0) {
    ElMessage.warning('请至少选择一位参观人')
    return
  }
  if (selectedItems.value.length !== selectedVisitorIds.value.length) {
    ElMessage.warning('请为每位参观人选择票种')
    return
  }

  submitting.value = true
  try {
    const result = await createOrder({
      sessionId: session.value.id,
      items: selectedItems.value.map((item) => ({
        visitorId: item.visitor.id,
        sessionTicketTypeId: item.ticketType.sessionTicketTypeId,
      })),
      userCouponId: selectedCouponId.value,
    })
    if (result.status === 'PAID') {
      ElMessage.success('预约成功，电子票已生成')
      await router.replace('/tickets')
      return
    }
    await router.replace({ name: 'payment', params: { orderId: result.id } })
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '订单提交失败'))
  } finally {
    submitting.value = false
  }
}

function formatMoney(value: number): string {
  return `¥${Number(value).toFixed(2)}`
}

function formatTime(value: string): string {
  return value.slice(0, 5)
}

watch(totalAmount, () => {
  if (selectedCoupon.value && Number(selectedCoupon.value.thresholdAmount) > totalAmount.value) {
    selectedCouponId.value = undefined
  }
})

onMounted(loadBooking)
</script>

<template>
  <div class="booking-page" v-loading="loading">
    <RouterLink
      class="back-link"
      :to="venue ? { name: 'venue-detail', params: { venueId: venue.id }, query: { visitDate, sessionId } } : '/'"
    >
      <el-icon><ArrowLeft /></el-icon>返回场次
    </RouterLink>

    <div class="steps" aria-label="预约步骤">
      <span class="active"><b>1</b>填写订单</span><i></i>
      <span><b>2</b>支付</span><i></i>
      <span><b>3</b>票券</span>
    </div>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />

    <template v-if="venue && session">
      <h1>确认预约信息</h1>
      <div class="booking-layout">
        <div class="booking-main">
          <section class="session-card">
            <el-icon><Calendar /></el-icon>
            <div>
              <span>已选场次</span>
              <strong>{{ venue.name }}</strong>
              <p>{{ visitDate }}　{{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</p>
            </div>
          </section>

          <section class="visitor-section">
            <div class="section-title">
              <div><h2>参观人</h2><span>为每位参观人选择对应票种</span></div>
              <RouterLink to="/visitors">管理参观人</RouterLink>
            </div>

            <div v-if="visitors.length" class="visitor-list">
              <article v-for="visitor in visitors" :key="visitor.id" class="visitor-row">
                <input
                  :id="`visitor-${visitor.id}`"
                  type="checkbox"
                  :checked="selectedVisitorIds.includes(visitor.id)"
                  @change="toggleVisitor(visitor.id, ($event.target as HTMLInputElement).checked)"
                >
                <label :for="`visitor-${visitor.id}`">
                  <strong>{{ visitor.name }}</strong>
                  <span>{{ visitor.idType }}　{{ visitor.maskedIdNumber }}</span>
                </label>
                <el-select
                  v-model="ticketTypeByVisitor[visitor.id]"
                  :disabled="!selectedVisitorIds.includes(visitor.id)"
                  placeholder="选择票种"
                >
                  <el-option
                    v-for="ticketType in availableTicketTypes"
                    :key="ticketType.sessionTicketTypeId"
                    :label="`${ticketType.ticketTypeName}　${formatMoney(ticketType.salePrice)}`"
                    :value="ticketType.sessionTicketTypeId"
                  />
                </el-select>
              </article>
            </div>
            <el-empty v-else description="暂无可用参观人，请先添加">
              <el-button type="primary" @click="router.push('/visitors')">添加参观人</el-button>
            </el-empty>
          </section>
        </div>

        <aside class="order-summary">
          <h2>订单明细</h2>
          <div class="summary-session"><strong>{{ venue.name }}</strong><span>{{ visitDate }}　{{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</span></div>
          <div class="selected-list">
            <p v-if="selectedItems.length === 0">尚未选择参观人</p>
            <div v-for="item in selectedItems" :key="item.visitor.id">
              <span>{{ item.visitor.name }} · {{ item.ticketType.ticketTypeName }}</span>
              <b>{{ formatMoney(item.ticketType.salePrice) }}</b>
            </div>
          </div>
          <div class="coupon-selector">
            <span>优惠券</span>
            <el-select v-model="selectedCouponId" clearable placeholder="不使用优惠券">
              <el-option
                v-for="coupon in coupons"
                :key="coupon.id"
                :label="`${coupon.couponName} · 减${formatMoney(coupon.discountAmount)}`"
                :value="coupon.id"
                :disabled="Number(coupon.thresholdAmount) > totalAmount"
              />
            </el-select>
          </div>
          <div class="summary-row"><span>参观人数</span><b>{{ selectedItems.length }} 人</b></div>
          <div class="summary-row"><span>票券金额</span><b>{{ formatMoney(totalAmount) }}</b></div>
          <div v-if="selectedCoupon" class="summary-row discount"><span>优惠金额</span><b>-{{ formatMoney(selectedCoupon.discountAmount) }}</b></div>
          <div class="summary-row total"><span>预计应付</span><b>{{ formatMoney(estimatedAmount) }}</b></div>
          <el-button type="primary" :loading="submitting" @click="submitOrder">提交订单</el-button>
          <small>订单金额以后端最终计算结果为准</small>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.booking-page { color: #1d2923; }
.back-link { display: inline-flex; gap: 7px; align-items: center; margin: 2px 0 18px; color: #435049; text-decoration: none; }
.steps { display: grid; grid-template-columns: auto 1fr auto 1fr auto; gap: 18px; align-items: center; max-width: 900px; margin: 8px auto 34px; color: #858d89; }
.steps span { display: flex; gap: 9px; align-items: center; white-space: nowrap; }
.steps b { display: grid; width: 32px; height: 32px; font-weight: 600; background: #eef0ef; border-radius: 50%; place-items: center; }
.steps i { height: 1px; background: #dfe3e0; }
.steps .active { color: #dc7200; font-weight: 700; }
.steps .active b { color: #fff; background: #ef8610; }
h1 { margin: 0 0 20px; font-size: 29px; }
.booking-layout { display: grid; grid-template-columns: minmax(0, 1fr) 330px; gap: 26px; align-items: start; }
.booking-main { display: grid; gap: 22px; }
.session-card { display: flex; gap: 16px; align-items: center; padding: 21px 24px; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.session-card > .el-icon { color: #ef8610; font-size: 29px; }
.session-card div { display: grid; gap: 5px; }
.session-card span { color: #7b8580; font-size: 13px; }
.session-card strong { font-size: 18px; }
.session-card p { margin: 0; color: #606b65; }
.visitor-section { overflow: hidden; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.section-title { display: flex; align-items: end; justify-content: space-between; padding: 20px 24px 14px; }
.section-title h2 { display: inline; margin: 0 10px 0 0; font-size: 20px; }
.section-title span { color: #8a928e; font-size: 13px; }
.section-title a { color: #e47800; text-decoration: none; }
.visitor-row { display: grid; grid-template-columns: auto minmax(180px, 1fr) minmax(220px, 330px); gap: 15px; align-items: center; min-height: 82px; padding: 14px 24px; border-top: 1px solid #e7ebe8; }
.visitor-row input { width: 18px; height: 18px; accent-color: #ef8610; }
.visitor-row label { display: grid; gap: 6px; cursor: pointer; }
.visitor-row label span { color: #737d78; font-size: 13px; }
.order-summary { position: sticky; top: 92px; padding: 22px; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; box-shadow: 0 10px 24px rgb(27 43 34 / 8%); }
.order-summary h2 { margin: 0 0 18px; font-size: 20px; }
.summary-session { display: grid; gap: 7px; padding-bottom: 17px; border-bottom: 1px solid #e8ebe9; }
.summary-session span { color: #747e79; font-size: 13px; }
.selected-list { display: grid; gap: 12px; padding: 17px 0; border-bottom: 1px solid #e8ebe9; }
.selected-list p { margin: 0; color: #8b938f; font-size: 13px; }
.selected-list div, .summary-row { display: flex; justify-content: space-between; gap: 14px; font-size: 14px; }
.selected-list b { color: #d96f00; }
.coupon-selector { display: grid; gap: 9px; padding: 17px 0 0; }
.coupon-selector > span { color: #59645e; font-size: 14px; }
.coupon-selector .el-select { width: 100%; }
.summary-row { padding-top: 17px; }
.summary-row.discount b { color: #18815a; }
.summary-row.total { align-items: center; margin-top: 17px; border-top: 1px solid #e8ebe9; font-size: 17px; font-weight: 700; }
.summary-row.total b { color: #e76f00; font-size: 27px; }
.order-summary .el-button { width: 100%; min-height: 44px; margin-top: 20px; --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
.order-summary small { display: block; margin-top: 12px; color: #89918d; text-align: center; }
@media (max-width: 900px) { .booking-layout { grid-template-columns: 1fr; } .order-summary { position: static; } }
@media (max-width: 640px) { .steps { gap: 8px; margin-bottom: 24px; font-size: 13px; } .steps b { width: 27px; height: 27px; } .steps i { min-width: 12px; } h1 { font-size: 25px; } .visitor-row { grid-template-columns: auto 1fr; padding: 15px; } .visitor-row .el-select { grid-column: 2; width: 100%; } .section-title { padding: 17px 15px 13px; } .section-title span { display: none; } }
</style>
