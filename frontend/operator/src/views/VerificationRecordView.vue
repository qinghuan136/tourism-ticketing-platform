<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshRight, Search } from '@element-plus/icons-vue'
import { getVerificationRecordPage } from '../api/verification'
import type { VerificationRecord, VerificationResult } from '../types/verification'
import { getRequestErrorMessage } from '../utils/request'
import '../assets/management.css'

const resultLabels: Record<VerificationResult, string> = { SUCCESS: '成功', FAILED: '失败' }
const records = ref<VerificationRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const ticketCode = ref('')
const result = ref<VerificationResult | ''>('')
const verificationDate = ref('')
const appliedTicketCode = ref('')
const appliedResult = ref<VerificationResult | ''>('')
const appliedDate = ref('')

function formatDateTime(value: string): string {
  return value.replace('T', ' ')
}

async function loadRecords(): Promise<void> {
  loading.value = true
  try {
    const page = await getVerificationRecordPage({
      ticketCode: appliedTicketCode.value || undefined,
      result: appliedResult.value || undefined,
      verificationDate: appliedDate.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
    records.value = page.items
    total.value = page.total
    currentPage.value = page.page
    pageSize.value = page.size
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '核销记录加载失败'))
  } finally {
    loading.value = false
  }
}

async function search(): Promise<void> {
  appliedTicketCode.value = ticketCode.value.trim()
  appliedResult.value = result.value
  appliedDate.value = verificationDate.value
  currentPage.value = 1
  await loadRecords()
}

async function reset(): Promise<void> {
  ticketCode.value = ''
  result.value = ''
  verificationDate.value = ''
  await search()
}

onMounted(loadRecords)
</script>

<template>
  <section class="management-page verification-record-page">
    <header class="management-heading">
      <div><h1>核销记录</h1><p>查询当前景点的成功与失败核销记录。</p></div>
    </header>

    <section class="management-filter" aria-label="核销记录筛选条件">
      <div class="management-filter__field">
        <label for="record-ticket-code">电子票码</label>
        <el-input id="record-ticket-code" v-model="ticketCode" maxlength="64" clearable placeholder="输入票码" @keyup.enter="search" />
      </div>
      <div class="management-filter__field compact-field">
        <label for="verification-result">核销结果</label>
        <el-select id="verification-result" v-model="result" clearable placeholder="全部结果">
          <el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILED" />
        </el-select>
      </div>
      <div class="management-filter__field compact-field">
        <label for="verification-date">核销日期</label>
        <el-date-picker id="verification-date" v-model="verificationDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      </div>
      <div class="management-filter__actions">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshRight" @click="reset">重置</el-button>
      </div>
    </section>

    <section class="management-table" aria-label="核销记录列表">
      <el-table v-loading="loading" :data="records" row-key="id" empty-text="暂无核销记录">
        <el-table-column label="核销时间" min-width="170"><template #default="scope">{{ formatDateTime(scope.row.verifiedAt) }}</template></el-table-column>
        <el-table-column prop="ticketCode" label="票码" min-width="190" />
        <el-table-column prop="visitorName" label="参观人" min-width="100" />
        <el-table-column prop="ticketTypeName" label="票种" min-width="105" />
        <el-table-column prop="verifierName" label="核销人员" min-width="110" />
        <el-table-column label="结果" width="80"><template #default="scope"><el-tag :type="scope.row.result === 'SUCCESS' ? 'success' : 'danger'">{{ resultLabels[scope.row.result as VerificationResult] }}</el-tag></template></el-table-column>
        <el-table-column label="失败原因" min-width="130"><template #default="scope">{{ scope.row.failureReason || '—' }}</template></el-table-column>
        <el-table-column label="设备" min-width="100"><template #default="scope">{{ scope.row.deviceNo || '—' }}</template></el-table-column>
      </el-table>
      <footer class="management-table__footer">
        <span>共 {{ total }} 条</span>
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20]" layout="sizes, prev, pager, next" background @current-change="loadRecords" @size-change="currentPage = 1; loadRecords()" />
      </footer>
    </section>
  </section>
</template>

<style scoped>
.verification-record-page .compact-field { width: 190px; min-width: 190px; }
.verification-record-page .management-filter__field :deep(.el-date-editor),
.verification-record-page .management-filter__field :deep(.el-select) { width: 100%; }
@media (max-width: 680px) {
  .verification-record-page .compact-field { width: 100%; }
  .management-table :deep(.el-table) { min-width: 1100px; }
}
</style>
