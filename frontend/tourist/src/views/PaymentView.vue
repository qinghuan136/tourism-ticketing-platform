<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Clock, Document, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMyOrder, payOrder } from '../api/order'
import type { OrderDetail } from '../types/order'
import { getRequestErrorMessage } from '../utils/request'

const route = useRoute()
const router = useRouter()
const order = ref<OrderDetail | null>(null)
const loading = ref(false)
const paying = ref(false)
const errorMessage = ref('')
const now = ref(Date.now())
let timer: number | undefined

const orderId = computed(() => Number(route.params.orderId))
const remainingSeconds = computed(() => {
  if (!order.value?.expireAt) return 0
  return Math.max(0, Math.floor((new Date(order.value.expireAt).getTime() - now.value) / 1000))
})
const countdownText = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
const canPay = computed(() => order.value?.status === 'PENDING_PAYMENT' && remainingSeconds.value > 0)

async function loadOrder(): Promise<void> {
  if (!Number.isSafeInteger(orderId.value) || orderId.value <= 0) {
    errorMessage.value = '订单参数无效'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    order.value = await getMyOrder(orderId.value)
    now.value = Date.now()
  } catch (error) {
    errorMessage.value = getRequestErrorMessage(error, '订单加载失败')
  } finally {
    loading.value = false
  }
}

async function confirmPayment(): Promise<void> {
  if (!canPay.value) return
  paying.value = true
  try {
    await payOrder(orderId.value)
    ElMessage.success('支付成功，电子票已生成')
    await router.replace('/tickets')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '支付失败'))
    await loadOrder()
  } finally {
    paying.value = false
  }
}

function formatMoney(value: number): string {
  return `¥${Number(value).toFixed(2)}`
}

function formatTime(value: string): string {
  return value.slice(0, 5)
}

onMounted(async () => {
  await loadOrder()
  timer = window.setInterval(() => { now.value = Date.now() }, 1000)
})
onBeforeUnmount(() => window.clearInterval(timer))
</script>

<template>
  <div class="payment-page" v-loading="loading">
    <div class="steps" aria-label="预约步骤">
      <span><b>1</b>填写订单</span><i></i>
      <span class="active"><b>2</b>支付</span><i></i>
      <span><b>3</b>票券</span>
    </div>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />

    <section v-if="order" class="payment-box">
      <h1>支付订单</h1>
      <p>模拟支付，不接入真实支付平台</p>

      <div v-if="order.status === 'PENDING_PAYMENT'" :class="['countdown', { expired: !canPay }]">
        <el-icon><Clock /></el-icon>
        <span v-if="canPay">请在 <b>{{ countdownText }}</b> 内完成支付</span>
        <span v-else>订单已超过支付截止时间</span>
      </div>
      <el-alert v-else title="当前订单已不处于待支付状态" type="info" :closable="false" />

      <div class="payment-detail">
        <div class="detail-row"><el-icon><Document /></el-icon><span>订单号</span><b>{{ order.orderNo }}</b></div>
        <div class="detail-row venue-row">
          <el-icon><Clock /></el-icon><span>景点与场次</span>
          <b>{{ order.venueName }}<small>{{ order.visitDate }}　{{ formatTime(order.startTime) }} - {{ formatTime(order.endTime) }}</small></b>
        </div>
        <div class="detail-row"><el-icon><User /></el-icon><span>参观人数</span><b>{{ order.quantity }} 人</b></div>
        <div class="detail-row amount"><span>支付金额</span><b>{{ formatMoney(order.totalAmount) }}</b></div>
      </div>

      <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" :disabled="!canPay" :loading="paying" @click="confirmPayment">
        确认模拟支付
      </el-button>
      <el-button v-else-if="order.status === 'PAID'" type="primary" @click="router.push('/tickets')">查看电子票</el-button>
      <el-button plain @click="router.push('/orders')">稍后处理</el-button>
    </section>
  </div>
</template>

<style scoped>
.payment-page { min-height: 690px; color: #1d2923; }
.steps { display: grid; grid-template-columns: auto 1fr auto 1fr auto; gap: 18px; align-items: center; max-width: 900px; margin: 10px auto 44px; color: #858d89; }
.steps span { display: flex; gap: 9px; align-items: center; white-space: nowrap; }
.steps b { display: grid; width: 32px; height: 32px; font-weight: 600; background: #eef0ef; border-radius: 50%; place-items: center; }
.steps i { height: 1px; background: #dfe3e0; }
.steps .active { color: #dc7200; font-weight: 700; }
.steps .active b { color: #fff; background: #ef8610; }
.payment-box { width: min(610px, 100%); margin: 0 auto; text-align: center; }
.payment-box h1 { margin: 0 0 8px; font-size: 31px; }
.payment-box > p { margin: 0 0 24px; color: #808984; }
.countdown { display: inline-flex; gap: 10px; align-items: center; margin-bottom: 20px; padding: 12px 20px; color: #805b0a; background: #fff7e8; border: 1px solid #f1d7a4; border-radius: 8px; }
.countdown .el-icon { color: #ef8610; font-size: 20px; }
.countdown b { color: #e86f00; font-size: 21px; }
.countdown.expired { color: #a44742; background: #fff1f0; border-color: #f0c5c2; }
.payment-detail { margin: 20px 0; overflow: hidden; text-align: left; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.detail-row { display: grid; grid-template-columns: 24px 120px 1fr; gap: 12px; align-items: center; min-height: 76px; padding: 16px 22px; border-bottom: 1px solid #e8ebe9; }
.detail-row .el-icon { color: #7d8782; font-size: 21px; }
.detail-row > span { color: #59645e; }
.detail-row > b { text-align: right; }
.detail-row small { display: block; margin-top: 7px; color: #77817c; font-weight: 400; }
.detail-row.amount { grid-template-columns: 1fr auto; border-bottom: 0; font-size: 17px; font-weight: 700; }
.detail-row.amount b { color: #e86f00; font-size: 28px; }
.payment-box > .el-button { width: 100%; min-height: 46px; margin: 0 0 10px; }
.payment-box > .el-button--primary { --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
@media (max-width: 600px) { .steps { gap: 8px; margin-bottom: 30px; font-size: 13px; } .steps b { width: 27px; height: 27px; } .detail-row { grid-template-columns: 22px 1fr; gap: 8px; padding: 15px; } .detail-row > b { grid-column: 2; text-align: left; } .detail-row.amount { grid-template-columns: 1fr auto; } .detail-row.amount b { grid-column: auto; } }
</style>
