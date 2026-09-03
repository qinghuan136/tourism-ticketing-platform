<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getStatisticsOverview, getStatisticsTrend, getTicketTypeStatistics } from '../api/statistics'
import type { StatisticsOverview, StatisticsTrend, TicketTypeStatistics } from '../types/statistics'
import { getRequestErrorMessage } from '../utils/request'

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = new Date()
const weekAgo = new Date(today)
weekAgo.setDate(today.getDate() - 6)

const dateRange = ref<[string, string]>([formatDate(weekAgo), formatDate(today)])
const loading = ref(false)
const overview = ref<StatisticsOverview>({
  paidOrderCount: 0,
  grossRevenue: 0,
  refundAmount: 0,
  netRevenue: 0,
  soldTicketCount: 0,
  verifiedTicketCount: 0,
})
const trend = ref<StatisticsTrend[]>([])
const ticketTypes = ref<TicketTypeStatistics[]>([])
const maxTrendRevenue = computed(() => Math.max(...trend.value.map((item) => Number(item.grossRevenue)), 1))
const maxTicketSales = computed(() => Math.max(...ticketTypes.value.map((item) => Number(item.soldQuantity)), 1))

function formatMoney(value: number): string {
  return `¥${Number(value).toFixed(2)}`
}

async function loadStatistics(): Promise<void> {
  if (!dateRange.value?.[0] || !dateRange.value?.[1]) {
    ElMessage.warning('请选择统计日期范围')
    return
  }
  loading.value = true
  const query = { startDate: dateRange.value[0], endDate: dateRange.value[1] }
  try {
    const [overviewResult, trendResult, ticketTypeResult] = await Promise.all([
      getStatisticsOverview(query),
      getStatisticsTrend(query),
      getTicketTypeStatistics(query),
    ])
    overview.value = overviewResult
    trend.value = trendResult
    ticketTypes.value = ticketTypeResult
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '运营数据加载失败'))
  } finally {
    loading.value = false
  }
}

onMounted(loadStatistics)
</script>

<template>
  <section v-loading="loading" class="statistics-page">
    <header class="statistics-heading">
      <div><h1>运营数据看板</h1><p>查看当前景点的收入、售票与核销情况。</p></div>
      <div class="date-actions">
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-button type="primary" :icon="Refresh" @click="loadStatistics">刷新</el-button>
      </div>
    </header>

    <section class="metric-grid" aria-label="经营概览">
      <article><span>净收入</span><strong>{{ formatMoney(overview.netRevenue) }}</strong><small>实收减退款</small></article>
      <article><span>实收金额</span><strong>{{ formatMoney(overview.grossRevenue) }}</strong><small>{{ overview.paidOrderCount }} 笔已支付订单</small></article>
      <article><span>退款金额</span><strong>{{ formatMoney(overview.refundAmount) }}</strong><small>统计期内退款</small></article>
      <article><span>售出票券</span><strong>{{ overview.soldTicketCount }}</strong><small>已支付订单票数</small></article>
      <article><span>成功核销</span><strong>{{ overview.verifiedTicketCount }}</strong><small>已入园票券</small></article>
    </section>

    <div class="dashboard-grid">
      <section class="dashboard-panel">
        <div class="panel-heading"><h2>每日销售趋势</h2><p>按支付日期统计实收金额</p></div>
        <div v-if="trend.length" class="trend-chart">
          <div v-for="item in trend" :key="item.statisticDate" class="trend-item">
            <span class="trend-value">{{ formatMoney(item.grossRevenue) }}</span>
            <div class="trend-track"><span :style="{ height: `${Math.max(Number(item.grossRevenue) / maxTrendRevenue * 100, 5)}%` }" /></div>
            <span class="trend-date">{{ item.statisticDate.slice(5) }}</span>
          </div>
        </div>
        <el-empty v-else description="当前日期范围暂无销售数据" :image-size="80" />
      </section>

      <section class="dashboard-panel">
        <div class="panel-heading"><h2>票种销售排行</h2><p>按已支付票券销量排序</p></div>
        <div v-if="ticketTypes.length" class="ticket-ranking">
          <div v-for="(item, index) in ticketTypes" :key="item.ticketTypeName" class="ranking-row">
            <span class="ranking-index">{{ index + 1 }}</span>
            <div class="ranking-main">
              <div><strong>{{ item.ticketTypeName }}</strong><span>{{ item.soldQuantity }} 张 · {{ formatMoney(item.salesAmount) }}</span></div>
              <span class="ranking-track"><i :style="{ width: `${Number(item.soldQuantity) / maxTicketSales * 100}%` }" /></span>
            </div>
          </div>
        </div>
        <el-empty v-else description="当前日期范围暂无票种数据" :image-size="80" />
      </section>
    </div>
  </section>
</template>

<style scoped>
.statistics-page { --brand: #e99000; --el-color-primary: var(--brand); min-height: 400px; color: #202733; }
.statistics-heading { display: flex; gap: 20px; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; }
.statistics-heading h1 { margin: 0 0 8px; font-size: 27px; }
.statistics-heading p, .panel-heading p { margin: 0; font-size: 14px; color: #75808e; }
.date-actions { display: flex; gap: 10px; }
.metric-grid { display: grid; grid-template-columns: repeat(5, minmax(150px, 1fr)); gap: 14px; margin-bottom: 20px; }
.metric-grid article, .dashboard-panel { background: #fff; border: 1px solid #dfe3e8; border-radius: 9px; }
.metric-grid article { display: grid; gap: 8px; padding: 21px; }
.metric-grid span, .metric-grid small { font-size: 13px; color: #75808e; }
.metric-grid strong { font-size: 25px; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.8fr); gap: 20px; }
.dashboard-panel { min-height: 340px; padding: 24px; }
.panel-heading h2 { margin: 0 0 6px; font-size: 18px; }
.trend-chart { display: flex; gap: 14px; align-items: end; height: 245px; padding-top: 30px; overflow-x: auto; }
.trend-item { display: grid; flex: 1 0 54px; grid-template-rows: 24px 1fr 22px; gap: 5px; min-width: 54px; height: 100%; text-align: center; }
.trend-value, .trend-date { font-size: 11px; color: #737e89; }
.trend-track { position: relative; overflow: hidden; background: #f3f5f7; border-radius: 5px 5px 2px 2px; }
.trend-track span { position: absolute; right: 0; bottom: 0; left: 0; background: #e99000; border-radius: 5px 5px 2px 2px; }
.ticket-ranking { display: grid; gap: 22px; margin-top: 28px; }
.ranking-row { display: flex; gap: 12px; align-items: flex-start; }
.ranking-index { display: grid; flex: 0 0 auto; width: 25px; height: 25px; place-items: center; font-size: 12px; color: #9a5a00; background: #fff2dc; border-radius: 6px; }
.ranking-main { flex: 1; min-width: 0; }
.ranking-main > div { display: flex; gap: 10px; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }
.ranking-main > div span { color: #75808e; white-space: nowrap; }
.ranking-track { display: block; height: 7px; overflow: hidden; background: #eef1f4; border-radius: 4px; }
.ranking-track i { display: block; height: 100%; background: #e99000; border-radius: inherit; }
@media (max-width: 1100px) {
  .metric-grid { grid-template-columns: repeat(3, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
}
@media (max-width: 680px) {
  .statistics-heading { display: grid; }
  .statistics-heading h1 { font-size: 24px; }
  .date-actions { display: grid; }
  .date-actions :deep(.el-date-editor) { width: 100%; }
  .metric-grid { grid-template-columns: 1fr 1fr; }
  .metric-grid article { padding: 17px; }
  .metric-grid strong { font-size: 21px; }
  .dashboard-panel { padding: 20px 16px; }
}
</style>
