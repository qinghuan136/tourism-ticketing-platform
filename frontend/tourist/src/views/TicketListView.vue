<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Calendar, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMyTicket, pageMyTickets } from '../api/ticket'
import type { Ticket, TicketStatus } from '../types/ticket'
import { getRequestErrorMessage } from '../utils/request'

interface StatusOption { label: string; value?: TicketStatus }

const tickets = ref<Ticket[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const selectedStatus = ref<TicketStatus | undefined>()
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<Ticket | null>(null)

const statusOptions: StatusOption[] = [
  { label: '全部' },
  { label: '有效', value: 'VALID' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已核销', value: 'USED' },
  { label: '已作废', value: 'VOID' },
  { label: '已过期', value: 'EXPIRED' },
]
const statusText: Record<TicketStatus, string> = { VALID: '有效', REFUNDING: '退款中', USED: '已核销', VOID: '已作废', EXPIRED: '已过期' }

async function loadTickets(): Promise<void> {
  loading.value = true
  try {
    const result = await pageMyTickets({ page: page.value, size, status: selectedStatus.value })
    tickets.value = result.items
    total.value = result.total
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '票券加载失败'))
  } finally {
    loading.value = false
  }
}

async function selectStatus(status?: TicketStatus): Promise<void> {
  selectedStatus.value = status
  page.value = 1
  await loadTickets()
}

async function openDetail(ticketId: number): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getMyTicket(ticketId)
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '票券详情加载失败'))
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function copyTicketCode(code: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('票码已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制票码')
  }
}

function formatTime(value: string): string { return value.slice(0, 5) }
function formatDateTime(value: string): string { return value.replace('T', ' ').slice(0, 16) }

onMounted(loadTickets)
</script>

<template>
  <div class="ticket-page">
    <h1>我的票券</h1>
    <p class="subtitle">每位参观人对应一张电子票</p>

    <div class="status-tabs">
      <button
        v-for="option in statusOptions"
        :key="option.label"
        type="button"
        :class="{ active: selectedStatus === option.value }"
        @click="selectStatus(option.value)"
      >{{ option.label }}</button>
    </div>

    <div v-loading="loading" class="ticket-grid">
      <article v-for="ticket in tickets" :key="ticket.id" class="ticket-card">
        <div class="ticket-main">
          <span :class="['ticket-status', `status-${ticket.status.toLowerCase()}`]">{{ statusText[ticket.status] }}</span>
          <h2>{{ ticket.venueName }}</h2>
          <p><el-icon><Calendar /></el-icon>{{ ticket.visitDate }}　{{ formatTime(ticket.startTime) }} - {{ formatTime(ticket.endTime) }}</p>
          <dl>
            <div><dt>参观人</dt><dd>{{ ticket.visitorName }}</dd></div>
            <div><dt>票种</dt><dd>{{ ticket.ticketTypeName }}</dd></div>
            <div><dt>票码</dt><dd>{{ ticket.ticketCode }}</dd></div>
          </dl>
        </div>
        <aside>
          <el-icon><Tickets /></el-icon>
          <el-button v-if="ticket.status === 'VALID'" type="primary" @click="openDetail(ticket.id)">查看票码</el-button>
          <el-button @click="openDetail(ticket.id)">查看详情</el-button>
        </aside>
      </article>
      <el-empty v-if="!loading && tickets.length === 0" description="暂无电子票" />
    </div>

    <el-pagination
      v-if="total > size"
      v-model:current-page="page"
      class="pagination"
      background
      layout="prev, pager, next"
      :page-size="size"
      :total="total"
      @current-change="loadTickets"
    />

    <el-dialog v-model="detailVisible" title="电子票详情" width="min(500px, 92vw)">
      <div v-loading="detailLoading" class="ticket-detail">
        <template v-if="detail">
          <span :class="['ticket-status', `status-${detail.status.toLowerCase()}`]">{{ statusText[detail.status] }}</span>
          <h2>{{ detail.venueName }}</h2>
          <p>{{ detail.visitDate }}　{{ formatTime(detail.startTime) }} - {{ formatTime(detail.endTime) }}</p>
          <div class="code-box">
            <span>核销票码</span><strong>{{ detail.ticketCode }}</strong>
            <el-button size="small" @click="copyTicketCode(detail.ticketCode)">复制</el-button>
          </div>
          <dl>
            <div><dt>参观人</dt><dd>{{ detail.visitorName }}</dd></div>
            <div><dt>票种</dt><dd>{{ detail.ticketTypeName }}</dd></div>
            <div><dt>景点地址</dt><dd>{{ detail.venueAddress || '-' }}</dd></div>
            <div><dt>订单号</dt><dd>{{ detail.orderNo || '-' }}</dd></div>
            <div><dt>有效时间</dt><dd>{{ formatDateTime(detail.validFrom) }} 至 {{ formatDateTime(detail.validUntil) }}</dd></div>
            <div v-if="detail.verifiedAt"><dt>核销时间</dt><dd>{{ formatDateTime(detail.verifiedAt) }}</dd></div>
          </dl>
          <p class="ticket-note">入馆时向工作人员出示上方票码</p>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.ticket-page { color: #1d2923; }
h1 { margin: 0 0 8px; font-size: 30px; }
.subtitle { margin: 0 0 24px; color: #79827e; }
.status-tabs { display: flex; gap: 30px; overflow-x: auto; margin-bottom: 18px; border-bottom: 1px solid #dde2df; }
.status-tabs { scrollbar-width: none; }
.status-tabs::-webkit-scrollbar { display: none; }
.status-tabs button { flex: none; padding: 10px 2px 12px; color: #4f5b55; cursor: pointer; background: transparent; border: 0; border-bottom: 3px solid transparent; }
.status-tabs button.active { color: #e87800; font-weight: 700; border-bottom-color: #ef8610; }
.ticket-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; min-height: 260px; }
.ticket-grid > .el-empty { grid-column: 1 / -1; }
.ticket-card { display: grid; grid-template-columns: 1fr 170px; overflow: hidden; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.ticket-main { padding: 22px; }
.ticket-status { display: inline-flex; padding: 5px 9px; font-size: 12px; border-radius: 5px; }
.status-valid { color: #167a54; background: #e9f7f0; }.status-used { color: #3f6e96; background: #edf4fa; }.status-void { color: #bd4843; background: #fff0ef; }.status-expired { color: #707975; background: #eef0ef; }
.ticket-card h2 { margin: 14px 0 10px; font-size: 20px; }
.ticket-card p { display: flex; gap: 7px; align-items: center; margin: 0; color: #6d7772; font-size: 14px; }
.ticket-card dl { display: grid; gap: 8px; margin: 18px 0 0; padding-top: 15px; border-top: 1px dashed #d8ddda; }
.ticket-card dl div { display: grid; grid-template-columns: 58px 1fr; gap: 10px; font-size: 14px; }
.ticket-card dt { color: #7d8682; }.ticket-card dd { margin: 0; overflow-wrap: anywhere; }
.ticket-card aside { display: flex; flex-direction: column; justify-content: center; gap: 10px; padding: 20px; border-left: 1px dashed #d8ddda; }
.ticket-card aside > .el-icon { align-self: center; margin-bottom: 8px; color: #ef8610; font-size: 30px; }
.ticket-card .el-button { margin: 0; }.ticket-card .el-button--primary { --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
.pagination { grid-column: 1 / -1; justify-content: center; margin: 24px 0; }
.ticket-detail { min-height: 180px; text-align: center; }
.ticket-detail h2 { margin: 15px 0 7px; }.ticket-detail > p { margin: 0; color: #707a75; }
.code-box { display: grid; grid-template-columns: 1fr auto; gap: 8px; align-items: center; margin: 22px 0; padding: 17px; text-align: left; background: #fff8ee; border: 1px solid #f0d7b8; border-radius: 8px; }
.code-box span { grid-column: 1 / -1; color: #8a6b44; font-size: 12px; }.code-box strong { overflow-wrap: anywhere; color: #bd6300; font-family: Consolas, monospace; font-size: 20px; }
.ticket-detail dl { display: grid; gap: 12px; text-align: left; }
.ticket-detail dl div { display: grid; grid-template-columns: 78px 1fr; gap: 12px; }.ticket-detail dt { color: #7a847f; }.ticket-detail dd { margin: 0; overflow-wrap: anywhere; }
.ticket-note { margin-top: 20px !important; padding-top: 15px; border-top: 1px solid #e5e8e6; font-size: 13px; }
@media (max-width: 850px) { .ticket-grid { grid-template-columns: 1fr; } }
@media (max-width: 520px) { .status-tabs { gap: 22px; } .ticket-card { grid-template-columns: 1fr; } .ticket-card aside { flex-direction: row; border-top: 1px dashed #d8ddda; border-left: 0; } .ticket-card aside > .el-icon { display: none; } }
</style>
