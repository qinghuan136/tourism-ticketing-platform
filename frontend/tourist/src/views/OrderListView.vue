<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getMyOrder, pageMyOrders, refundOrder } from '../api/order'
import type { OrderDetail, OrderStatus, OrderSummary } from '../types/order'
import { getRequestErrorMessage } from '../utils/request'

interface StatusOption { label: string; value?: OrderStatus }

const router = useRouter()
const orders = ref<OrderSummary[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const selectedStatus = ref<OrderStatus | undefined>()
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<OrderDetail | null>(null)

const statusOptions: StatusOption[] = [
  { label: '全部' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '已支付', value: 'PAID' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已退款', value: 'REFUNDED' },
]

const statusText: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付', PAID: '已支付', REFUNDING: '退款中', CANCELLED: '已取消', CLOSED: '已关闭',
  COMPLETED: '已完成', REFUNDED: '已退款',
}

async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    const result = await pageMyOrders({ page: page.value, size, status: selectedStatus.value })
    orders.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '订单加载失败'))
  } finally {
    loading.value = false
  }
}

async function selectStatus(status?: OrderStatus): Promise<void> {
  selectedStatus.value = status
  page.value = 1
  await loadOrders()
}

async function openDetail(orderId: number): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getMyOrder(orderId)
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '订单详情加载失败'))
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function handleCancel(order: OrderSummary): Promise<void> {
  try {
    await ElMessageBox.confirm('取消后将释放场次和票种库存，是否继续？', '取消订单', { type: 'warning' })
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(getRequestErrorMessage(error, '取消订单失败'))
  }
}

async function handleRefund(order: OrderSummary): Promise<void> {
  try {
    await ElMessageBox.confirm('退款后电子票将作废，是否继续？', '整单退款', { type: 'warning' })
    await refundOrder(order.id)
    ElMessage.success('退款申请已提交')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(getRequestErrorMessage(error, '退款失败'))
  }
}

function formatMoney(value: number): string { return `¥${Number(value).toFixed(2)}` }
function formatTime(value: string): string { return value.slice(0, 5) }
function formatDateTime(value: string): string { return value.replace('T', ' ').slice(0, 16) }

onMounted(loadOrders)
</script>

<template>
  <div class="order-page">
    <h1>我的订单</h1>
    <p class="subtitle">查看订单状态、继续支付或查看票券</p>

    <div class="status-tabs">
      <button
        v-for="option in statusOptions"
        :key="option.label"
        type="button"
        :class="{ active: selectedStatus === option.value }"
        @click="selectStatus(option.value)"
      >{{ option.label }}</button>
    </div>

    <div v-loading="loading" class="order-list">
      <article v-for="order in orders" :key="order.id" class="order-card">
        <header>
          <span>订单号：{{ order.orderNo }}</span>
          <b :class="`status-${order.status.toLowerCase()}`">{{ statusText[order.status] }}</b>
          <time>下单时间：{{ formatDateTime(order.createdAt) }}</time>
        </header>
        <div class="order-body">
          <el-icon><Calendar /></el-icon>
          <div class="order-info">
            <h2>{{ order.venueName }}</h2>
            <p>{{ order.visitDate }}　{{ formatTime(order.startTime) }} - {{ formatTime(order.endTime) }}</p>
            <p><el-icon><User /></el-icon>参观人数：{{ order.quantity }} 人</p>
          </div>
          <strong class="amount">{{ formatMoney(order.totalAmount) }}</strong>
        </div>
        <footer>
          <el-button v-if="order.status === 'PENDING_PAYMENT'" @click="handleCancel(order)">取消订单</el-button>
          <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="router.push(`/payment/${order.id}`)">继续支付</el-button>
          <el-button v-if="order.status === 'PAID'" @click="handleRefund(order)">申请退款</el-button>
          <el-button v-if="order.status === 'PAID' || order.status === 'COMPLETED'" type="primary" plain @click="router.push('/tickets')">查看票券</el-button>
          <el-button @click="openDetail(order.id)">查看详情</el-button>
        </footer>
      </article>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>

    <el-pagination
      v-if="total > size"
      v-model:current-page="page"
      class="pagination"
      background
      layout="prev, pager, next"
      :page-size="size"
      :total="total"
      @current-change="loadOrders"
    />

    <el-dialog v-model="detailVisible" title="订单详情" width="min(680px, 92vw)">
      <div v-loading="detailLoading" class="detail-content">
        <template v-if="detail">
          <dl class="detail-grid">
            <div><dt>订单号</dt><dd>{{ detail.orderNo }}</dd></div>
            <div><dt>订单状态</dt><dd>{{ statusText[detail.status] }}</dd></div>
            <div><dt>景点</dt><dd>{{ detail.venueName }}</dd></div>
            <div><dt>参观场次</dt><dd>{{ detail.visitDate }} {{ formatTime(detail.startTime) }} - {{ formatTime(detail.endTime) }}</dd></div>
            <div><dt>订单金额</dt><dd>{{ formatMoney(detail.totalAmount) }}</dd></div>
            <div v-if="detail.paymentNo"><dt>支付流水</dt><dd>{{ detail.paymentNo }}</dd></div>
          </dl>
          <h3>参观人和票种</h3>
          <div class="detail-items">
            <div v-for="item in detail.items" :key="item.id">
              <span><b>{{ item.visitorName }}</b><small>{{ item.visitorIdType }}　{{ item.maskedVisitorIdNumber }}</small></span>
              <span>{{ item.ticketTypeName }}　{{ formatMoney(item.unitPrice) }}</span>
              <span v-if="item.ticket" class="ticket-code">{{ item.ticket.ticketCode }}</span>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-page { color: #1d2923; }
h1 { margin: 0 0 8px; font-size: 30px; }
.subtitle { margin: 0 0 24px; color: #79827e; }
.status-tabs { display: flex; gap: 28px; overflow-x: auto; margin-bottom: 14px; border-bottom: 1px solid #dde2df; }
.status-tabs { scrollbar-width: none; }
.status-tabs::-webkit-scrollbar { display: none; }
.status-tabs button { flex: none; padding: 10px 2px 12px; color: #4f5b55; cursor: pointer; background: transparent; border: 0; border-bottom: 3px solid transparent; }
.status-tabs button.active { color: #e87800; font-weight: 700; border-bottom-color: #ef8610; }
.order-list { min-height: 260px; }
.order-card { margin-bottom: 14px; overflow: hidden; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.order-card header { display: grid; grid-template-columns: 1fr auto 1fr; gap: 15px; align-items: center; padding: 12px 20px; color: #67716c; font-size: 13px; background: #fafbfa; border-bottom: 1px solid #e7eae8; }
.order-card header b { font-size: 14px; }
.order-card header time { text-align: right; }
.status-pending_payment { color: #d87500; }.status-paid { color: #16835a; }.status-refunding { color: #d87500; }.status-cancelled,.status-refunded { color: #bd4c46; }.status-closed,.status-completed { color: #68726d; }
.order-body { display: grid; grid-template-columns: 42px 1fr auto; gap: 18px; align-items: center; padding: 20px; }
.order-body > .el-icon { display: grid; width: 42px; height: 42px; color: #ef8610; font-size: 23px; background: #fff5e9; border-radius: 8px; place-items: center; }
.order-info h2 { margin: 0 0 8px; font-size: 19px; }
.order-info p { display: flex; align-items: center; margin: 5px 0; color: #6f7974; font-size: 14px; }
.order-info p .el-icon { margin-right: 5px; }
.amount { color: #e87500; font-size: 23px; }
.order-card footer { display: flex; justify-content: flex-end; padding: 12px 20px; border-top: 1px solid #e7eae8; }
.order-card .el-button--primary:not(.is-plain) { --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
.pagination { justify-content: center; margin: 24px 0; }
.detail-content { min-height: 160px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 24px; margin: 0; }
.detail-grid div { min-width: 0; }
.detail-grid dt { margin-bottom: 5px; color: #7c8580; font-size: 13px; }
.detail-grid dd { margin: 0; overflow-wrap: anywhere; }
.detail-content h3 { margin: 24px 0 10px; }
.detail-items { border-top: 1px solid #e4e8e5; }
.detail-items > div { display: grid; grid-template-columns: 1fr auto; gap: 8px 16px; padding: 13px 0; border-bottom: 1px solid #e4e8e5; }
.detail-items span:first-child { display: grid; gap: 4px; }
.detail-items small { color: #7a847f; }
.ticket-code { grid-column: 1 / -1; color: #707a75; font-family: Consolas, monospace; }
@media (max-width: 640px) { .status-tabs { gap: 20px; } .order-card header { grid-template-columns: 1fr auto; padding: 11px 14px; } .order-card header time { grid-column: 1 / -1; text-align: left; } .order-body { grid-template-columns: 36px 1fr; gap: 12px; padding: 16px 14px; } .order-body > .el-icon { width: 36px; height: 36px; } .amount { grid-column: 2; font-size: 21px; } .order-card footer { flex-wrap: wrap; padding: 12px 14px; } .detail-grid { grid-template-columns: 1fr; } }
</style>
