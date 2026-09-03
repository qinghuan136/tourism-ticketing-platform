<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { getOrderDetail, getOrderPage } from '../api/order'
import type { OrderDetail, OrderStatus, OrderSummary, TicketStatus } from '../types/order'
import { getRequestErrorMessage } from '../utils/request'
import '../assets/management.css'

const statusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  REFUNDING: '退款中',
  CANCELLED: '已取消',
  CLOSED: '已关闭',
  COMPLETED: '已完成',
  REFUNDED: '已退款',
}

const statusTagTypes: Record<OrderStatus, 'info' | 'success' | 'warning' | 'danger'> = {
  PENDING_PAYMENT: 'warning',
  PAID: 'success',
  REFUNDING: 'warning',
  CANCELLED: 'info',
  CLOSED: 'info',
  COMPLETED: 'success',
  REFUNDED: 'danger',
}

const ticketStatusLabels: Record<TicketStatus, string> = {
  VALID: '待使用',
  REFUNDING: '退款中',
  USED: '已核销',
  VOID: '已作废',
  EXPIRED: '已过期',
}

const orders = ref<OrderSummary[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detail = ref<OrderDetail | null>(null)

const orderNo = ref('')
const sessionId = ref<number | undefined>()
const visitDate = ref('')
const status = ref<OrderStatus | ''>('')
const appliedOrderNo = ref('')
const appliedSessionId = ref<number | undefined>()
const appliedVisitDate = ref('')
const appliedStatus = ref<OrderStatus | ''>('')

function formatDateTime(value: string | null): string {
  return value ? value.replace('T', ' ') : '—'
}

function formatMoney(value: number): string {
  return `¥${Number(value).toFixed(2)}`
}

async function loadOrders(): Promise<void> {
  loading.value = true
  try {
    const result = await getOrderPage({
      orderNo: appliedOrderNo.value || undefined,
      sessionId: appliedSessionId.value,
      visitDate: appliedVisitDate.value || undefined,
      status: appliedStatus.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
    orders.value = result.items
    total.value = result.total
    currentPage.value = result.page
    pageSize.value = result.size
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '订单列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function search(): Promise<void> {
  appliedOrderNo.value = orderNo.value.trim()
  appliedSessionId.value = sessionId.value
  appliedVisitDate.value = visitDate.value
  appliedStatus.value = status.value
  currentPage.value = 1
  await loadOrders()
}

async function reset(): Promise<void> {
  orderNo.value = ''
  sessionId.value = undefined
  visitDate.value = ''
  status.value = ''
  await search()
}

async function openDetail(row: OrderSummary): Promise<void> {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getOrderDetail(row.id)
  } catch (error) {
    detailVisible.value = false
    ElMessage.error(getRequestErrorMessage(error, '订单详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <section class="management-page order-page">
    <header class="management-heading">
      <div>
        <h1>订单查询</h1>
        <p>查询当前景点订单，并查看购买人、参观人与电子票信息。</p>
      </div>
    </header>

    <section class="management-filter" aria-label="订单筛选条件">
      <div class="management-filter__field">
        <label for="order-number">订单号</label>
        <el-input id="order-number" v-model="orderNo" maxlength="32" clearable placeholder="输入订单号" @keyup.enter="search" />
      </div>
      <div class="management-filter__field compact-field">
        <label for="session-id">场次 ID</label>
        <el-input-number id="session-id" v-model="sessionId" :min="1" :controls="false" placeholder="输入场次 ID" />
      </div>
      <div class="management-filter__field compact-field">
        <label for="visit-date">参观日期</label>
        <el-date-picker id="visit-date" v-model="visitDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      </div>
      <div class="management-filter__field compact-field">
        <label for="order-status">订单状态</label>
        <el-select id="order-status" v-model="status" clearable placeholder="全部状态">
          <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
        </el-select>
      </div>
      <div class="management-filter__actions">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshRight" @click="reset">重置</el-button>
      </div>
    </section>

    <section class="management-table" aria-label="订单列表">
      <el-table v-loading="loading" :data="orders" row-key="id" empty-text="暂无订单">
        <el-table-column prop="orderNo" label="订单号" min-width="185" />
        <el-table-column label="参观场次" min-width="190">
          <template #default="scope">
            <div>{{ scope.row.visitDate }}</div>
            <small>{{ scope.row.startTime }}–{{ scope.row.endTime }} · 场次 {{ scope.row.sessionId }}</small>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="票数" width="70" />
        <el-table-column label="实付金额" min-width="100">
          <template #default="scope">{{ formatMoney(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="95">
          <template #default="scope">
            <el-tag :type="statusTagTypes[scope.row.status as OrderStatus]">
              {{ statusLabels[scope.row.status as OrderStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="170">
          <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <footer class="management-table__footer">
        <span>共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20]"
          layout="sizes, prev, pager, next"
          background
          @current-change="loadOrders"
          @size-change="currentPage = 1; loadOrders()"
        />
      </footer>
    </section>

    <el-dialog v-model="detailVisible" title="订单详情" width="860px" class="management-dialog" destroy-on-close>
      <div v-loading="detailLoading" class="order-detail">
        <template v-if="detail">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="statusTagTypes[detail.status]">{{ statusLabels[detail.status] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="下单时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="购买人">{{ detail.purchaserName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.purchaserPhone }}</el-descriptions-item>
            <el-descriptions-item label="支付单号">{{ detail.paymentNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="参观场次">{{ detail.visitDate }} {{ detail.startTime }}–{{ detail.endTime }}</el-descriptions-item>
            <el-descriptions-item label="票数">{{ detail.quantity }} 张</el-descriptions-item>
            <el-descriptions-item label="实付金额">{{ formatMoney(detail.totalAmount) }}</el-descriptions-item>
          </el-descriptions>

          <h3>订单票券</h3>
          <el-table :data="detail.items" border empty-text="暂无票券">
            <el-table-column prop="visitorName" label="参观人" min-width="100" />
            <el-table-column prop="maskedVisitorIdNumber" label="证件号码" min-width="170" />
            <el-table-column prop="ticketTypeName" label="票种" min-width="110" />
            <el-table-column label="成交价" width="100">
              <template #default="scope">{{ formatMoney(scope.row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="票码" min-width="220">
              <template #default="scope">{{ scope.row.ticket?.ticketCode || '未出票' }}</template>
            </el-table-column>
            <el-table-column label="票券状态" width="95">
              <template #default="scope">{{ scope.row.ticket ? ticketStatusLabels[scope.row.ticket.status as TicketStatus] : '—' }}</template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.order-page .management-filter {
  flex-wrap: wrap;
}

.order-page .management-filter__field {
  min-width: 180px;
}

.order-page .compact-field {
  width: 180px;
}

.order-page .management-filter__field :deep(.el-date-editor),
.order-page .management-filter__field :deep(.el-input-number),
.order-page .management-filter__field :deep(.el-select) {
  width: 100%;
}

.management-table small {
  color: #7a8490;
}

.order-detail {
  min-height: 120px;
}

.order-detail h3 {
  margin: 24px 0 12px;
  font-size: 16px;
}

@media (max-width: 680px) {
  .order-page .compact-field {
    width: 100%;
  }

  .management-table :deep(.el-table) {
    min-width: 980px;
  }

  .order-detail :deep(.el-descriptions__body) {
    overflow-x: auto;
  }
}
</style>
