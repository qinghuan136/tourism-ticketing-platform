<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { getTicketTypePage } from '../api/ticketType'
import { createSession, deleteSession, getSession, getSessionPage, sendSessionEvent, updateSession } from '../api/session'
import type { TicketType } from '../types/ticketType'
import type { AdmissionSession, SessionEvent, SessionStatus, SessionWriteRequest } from '../types/session'
import { getRequestErrorMessage } from '../utils/request'
import '../assets/management.css'

interface TicketConfigDraft {
  ticketTypeId: number | null
  salePrice: number
  allocatedQuantity: number
}

interface SessionForm {
  visitDate: string
  startTime: string
  endTime: string
  bookingStartAt: string
  bookingEndAt: string
  totalCapacity: number
  ticketTypes: TicketConfigDraft[]
}

const statusLabels: Record<SessionStatus, string> = {
  DRAFT: '草稿',
  OPEN: '开放预约',
  CLOSED: '停止预约',
  ENDED: '已结束',
  CANCELLED: '已取消',
}

const statusTagTypes: Record<SessionStatus, 'info' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'info',
  OPEN: 'success',
  CLOSED: 'warning',
  ENDED: 'info',
  CANCELLED: 'danger',
}

const eventLabels: Record<SessionEvent, string> = {
  PUBLISH: '发布场次',
  CLOSE_BOOKING: '关闭预约',
  REOPEN_BOOKING: '重新开放',
  CANCEL: '取消场次',
  SESSION_ENDED: '标记结束',
}

const sessions = ref<AdmissionSession[]>([])
const ticketOptions = ref<TicketType[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogLoading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const visitDate = ref('')
const status = ref<SessionStatus | ''>('')
const appliedVisitDate = ref('')
const appliedStatus = ref<SessionStatus | ''>('')

const form = reactive<SessionForm>({
  visitDate: '',
  startTime: '',
  endTime: '',
  bookingStartAt: '',
  bookingEndAt: '',
  totalCapacity: 1,
  ticketTypes: [],
})

function newTicketConfig(): TicketConfigDraft {
  return { ticketTypeId: null, salePrice: 0, allocatedQuantity: 1 }
}

function formatDateTime(value: string): string {
  return value.replace('T', ' ')
}

async function loadSessions(): Promise<void> {
  loading.value = true
  try {
    const result = await getSessionPage({
      visitDate: appliedVisitDate.value || undefined,
      status: appliedStatus.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
    sessions.value = result.items
    total.value = result.total
    currentPage.value = result.page
    pageSize.value = result.size
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '场次列表加载失败'))
  } finally {
    loading.value = false
  }
}

async function loadTicketOptions(): Promise<void> {
  if (ticketOptions.value.length) return
  try {
    const result = await getTicketTypePage({ page: 1, size: 100 })
    ticketOptions.value = result.items
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '票种加载失败'))
  }
}

function resetForm(): void {
  form.visitDate = ''
  form.startTime = ''
  form.endTime = ''
  form.bookingStartAt = ''
  form.bookingEndAt = ''
  form.totalCapacity = 1
  form.ticketTypes = [newTicketConfig()]
}

async function openCreateDialog(): Promise<void> {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  await loadTicketOptions()
}

async function openEditDialog(row: AdmissionSession): Promise<void> {
  editingId.value = row.id
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    const detail = await getSession(row.id)
    form.visitDate = detail.visitDate
    form.startTime = detail.startTime
    form.endTime = detail.endTime
    form.bookingStartAt = detail.bookingStartAt
    form.bookingEndAt = detail.bookingEndAt
    form.totalCapacity = detail.totalCapacity
    form.ticketTypes = detail.ticketTypes.map((item) => ({
      ticketTypeId: item.ticketTypeId,
      salePrice: item.salePrice,
      allocatedQuantity: item.allocatedQuantity,
    }))
    await loadTicketOptions()
  } catch (error) {
    dialogVisible.value = false
    ElMessage.error(getRequestErrorMessage(error, '场次详情加载失败'))
  } finally {
    dialogLoading.value = false
  }
}

function selectTicket(index: number): void {
  const item = form.ticketTypes[index]
  const ticketType = ticketOptions.value.find((option) => option.id === item.ticketTypeId)
  if (ticketType) item.salePrice = Number(ticketType.basePrice)
}

function validateForm(): string {
  if (!form.visitDate || !form.startTime || !form.endTime || !form.bookingStartAt || !form.bookingEndAt) {
    return '请填写完整的日期和时间'
  }
  if (form.startTime >= form.endTime) return '场次结束时间必须晚于开始时间'
  if (form.bookingStartAt >= form.bookingEndAt) return '预约结束时间必须晚于开始时间'
  if (form.totalCapacity <= 0) return '场次容量必须大于 0'
  if (!form.ticketTypes.length || form.ticketTypes.some((item) => !item.ticketTypeId)) return '请至少配置一个票种'
  if (new Set(form.ticketTypes.map((item) => item.ticketTypeId)).size !== form.ticketTypes.length) return '同一票种不能重复配置'
  if (form.ticketTypes.some((item) => item.salePrice < 0 || item.allocatedQuantity <= 0)) return '票价不能小于 0，票种配额必须大于 0'
  if (form.ticketTypes.reduce((sum, item) => sum + item.allocatedQuantity, 0) > form.totalCapacity) return '票种配额总和不能超过场次容量'
  return ''
}

async function saveSession(): Promise<void> {
  const validationMessage = validateForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  const payload: SessionWriteRequest = {
    visitDate: form.visitDate,
    startTime: form.startTime,
    endTime: form.endTime,
    bookingStartAt: form.bookingStartAt,
    bookingEndAt: form.bookingEndAt,
    totalCapacity: form.totalCapacity,
    ticketTypes: form.ticketTypes.map((item) => ({
      ticketTypeId: item.ticketTypeId!,
      salePrice: item.salePrice,
      allocatedQuantity: item.allocatedQuantity,
    })),
  }

  saving.value = true
  try {
    if (editingId.value) await updateSession(editingId.value, payload)
    else await createSession(payload)
    ElMessage.success(editingId.value ? '场次已更新' : '场次已创建')
    dialogVisible.value = false
    if (!editingId.value) currentPage.value = 1
    await loadSessions()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '场次保存失败'))
  } finally {
    saving.value = false
  }
}

function availableEvents(sessionStatus: SessionStatus): SessionEvent[] {
  if (sessionStatus === 'DRAFT') return ['PUBLISH', 'CANCEL']
  if (sessionStatus === 'OPEN') return ['CLOSE_BOOKING', 'CANCEL', 'SESSION_ENDED']
  if (sessionStatus === 'CLOSED') return ['REOPEN_BOOKING', 'CANCEL', 'SESSION_ENDED']
  return []
}

async function handleEvent(row: AdmissionSession, event: SessionEvent): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定${eventLabels[event]}吗？`, '变更场次状态', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: event === 'CANCEL' ? 'warning' : 'info',
    })
  } catch {
    return
  }

  try {
    await sendSessionEvent(row.id, event)
    ElMessage.success('场次状态已更新')
    await loadSessions()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '场次状态更新失败'))
  }
}

async function removeSession(row: AdmissionSession): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除这个草稿场次吗？', '删除场次', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await deleteSession(row.id)
    ElMessage.success('场次已删除')
    await loadSessions()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '场次删除失败'))
  }
}

async function search(): Promise<void> {
  appliedVisitDate.value = visitDate.value
  appliedStatus.value = status.value
  currentPage.value = 1
  await loadSessions()
}

async function reset(): Promise<void> {
  visitDate.value = ''
  status.value = ''
  await search()
}

onMounted(loadSessions)
</script>

<template>
  <section class="management-page session-page">
    <header class="management-heading">
      <div>
        <h1>场次管理</h1>
        <p>配置开放日期、预约时间、票种售价与配额。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建场次</el-button>
    </header>

    <section class="management-filter" aria-label="场次筛选条件">
      <div class="management-filter__field">
        <label for="session-date">参观日期</label>
        <el-date-picker id="session-date" v-model="visitDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
      </div>
      <div class="management-filter__field">
        <label for="session-status">场次状态</label>
        <el-select id="session-status" v-model="status" placeholder="全部状态" clearable>
          <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
        </el-select>
      </div>
      <div class="management-filter__actions">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshRight" @click="reset">重置</el-button>
      </div>
    </section>

    <section class="management-table" aria-label="场次列表">
      <el-table v-loading="loading" :data="sessions" row-key="id" empty-text="暂无场次">
        <el-table-column prop="visitDate" label="参观日期" min-width="105" />
        <el-table-column label="入园时间" min-width="130">
          <template #default="scope">{{ scope.row.startTime }}–{{ scope.row.endTime }}</template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="220">
          <template #default="scope">{{ formatDateTime(scope.row.bookingStartAt) }} 至 {{ formatDateTime(scope.row.bookingEndAt) }}</template>
        </el-table-column>
        <el-table-column label="余量/容量" min-width="90">
          <template #default="scope">{{ scope.row.remainingCapacity }}/{{ scope.row.totalCapacity }}</template>
        </el-table-column>
        <el-table-column label="票种" min-width="70">
          <template #default="scope">{{ scope.row.ticketTypes.length }} 种</template>
        </el-table-column>
        <el-table-column label="状态" min-width="95">
          <template #default="scope">
            <el-tag :type="statusTagTypes[scope.row.status as SessionStatus]">{{ statusLabels[scope.row.status as SessionStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.status === 'DRAFT'" link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-dropdown v-if="availableEvents(scope.row.status).length" @command="(event: SessionEvent) => handleEvent(scope.row, event)">
              <el-button link type="primary">状态操作</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="event in availableEvents(scope.row.status)" :key="event" :command="event">
                    {{ eventLabels[event] }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-if="scope.row.status === 'DRAFT'" link type="danger" @click="removeSession(scope.row)">删除</el-button>
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
          @current-change="loadSessions"
          @size-change="currentPage = 1; loadSessions()"
        />
      </footer>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑草稿场次' : '新建场次'"
      width="760px"
      class="management-dialog session-dialog"
      :close-on-click-modal="false"
    >
      <div v-loading="dialogLoading">
        <el-form label-position="top">
          <div class="session-form-grid">
            <el-form-item label="参观日期">
              <el-date-picker v-model="form.visitDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
            <el-form-item label="场次容量">
              <el-input-number v-model="form.totalCapacity" :min="1" />
            </el-form-item>
            <el-form-item label="入园开始时间">
              <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" format="HH:mm" placeholder="选择时间" />
            </el-form-item>
            <el-form-item label="入园结束时间">
              <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" format="HH:mm" placeholder="选择时间" />
            </el-form-item>
            <el-form-item label="预约开始时间">
              <el-date-picker v-model="form.bookingStartAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" />
            </el-form-item>
            <el-form-item label="预约结束时间">
              <el-date-picker v-model="form.bookingEndAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" />
            </el-form-item>
          </div>
        </el-form>

        <div class="ticket-config-heading">
          <strong>场次票种</strong>
          <el-button link type="primary" @click="form.ticketTypes.push(newTicketConfig())">添加票种</el-button>
        </div>
        <div class="ticket-config-list">
          <div v-for="(item, index) in form.ticketTypes" :key="index" class="ticket-config-row">
            <el-select v-model="item.ticketTypeId" placeholder="选择票种" @change="selectTicket(index)">
              <el-option v-for="option in ticketOptions" :key="option.id" :label="option.name" :value="option.id" />
            </el-select>
            <el-input-number v-model="item.salePrice" :min="0" :precision="2" placeholder="售价" />
            <el-input-number v-model="item.allocatedQuantity" :min="1" placeholder="配额" />
            <el-button link type="danger" @click="form.ticketTypes.splice(index, 1)">移除</el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSession">保存草稿</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.session-page .management-filter__field :deep(.el-date-editor),
.session-page .management-filter__field :deep(.el-select) {
  width: 220px;
}

.session-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.session-form-grid :deep(.el-date-editor),
.session-form-grid :deep(.el-input-number) {
  width: 100%;
}

.ticket-config-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  margin-bottom: 10px;
  border-top: 1px solid #e7eaee;
}

.ticket-config-list {
  display: grid;
  gap: 10px;
}

.ticket-config-row {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 160px 150px 48px;
  gap: 10px;
  align-items: center;
}

.ticket-config-row :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 680px) {
  .management-table :deep(.el-table) {
    min-width: 1050px;
  }

  .session-form-grid {
    grid-template-columns: 1fr;
  }

  .ticket-config-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
