<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import {
  cancelCouponActivity,
  createCouponActivity,
  getCouponActivity,
  getCouponActivityPage,
  publishCouponActivity,
  updateCouponActivity,
} from '../api/coupon'
import type { CouponActivity, CouponActivityStatus, CouponActivityWriteRequest } from '../types/coupon'
import { getRequestErrorMessage } from '../utils/request'
import '../assets/management.css'

const statusLabels: Record<CouponActivityStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  ENDED: '已结束',
  CANCELLED: '已取消',
}

const statusTagTypes: Record<CouponActivityStatus, 'info' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'info',
  PUBLISHED: 'success',
  ENDED: 'warning',
  CANCELLED: 'danger',
}

const activities = ref<CouponActivity[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const detailLoading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const keyword = ref('')
const status = ref<CouponActivityStatus | ''>('')
const appliedKeyword = ref('')
const appliedStatus = ref<CouponActivityStatus | ''>('')

const form = reactive<CouponActivityWriteRequest>({
  name: '',
  thresholdAmount: 0,
  discountAmount: 0,
  totalStock: 1,
  claimStartAt: '',
  claimEndAt: '',
  validFrom: '',
  validUntil: '',
})

function formatDateTime(value: string): string {
  return value.replace('T', ' ')
}

function formatMoney(value: number): string {
  return `¥${Number(value).toFixed(2)}`
}

async function loadActivities(): Promise<void> {
  loading.value = true
  try {
    const page = await getCouponActivityPage({
      keyword: appliedKeyword.value || undefined,
      status: appliedStatus.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
    activities.value = page.items
    total.value = page.total
    currentPage.value = page.page
    pageSize.value = page.size
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '优惠券活动加载失败'))
  } finally {
    loading.value = false
  }
}

async function search(): Promise<void> {
  appliedKeyword.value = keyword.value.trim()
  appliedStatus.value = status.value
  currentPage.value = 1
  await loadActivities()
}

async function reset(): Promise<void> {
  keyword.value = ''
  status.value = ''
  await search()
}

function resetForm(): void {
  form.name = ''
  form.thresholdAmount = 0
  form.discountAmount = 0
  form.totalStock = 1
  form.claimStartAt = ''
  form.claimEndAt = ''
  form.validFrom = ''
  form.validUntil = ''
}

function openCreateDialog(): void {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEditDialog(row: CouponActivity): Promise<void> {
  editingId.value = row.id
  dialogVisible.value = true
  detailLoading.value = true
  try {
    const detail = await getCouponActivity(row.id)
    form.name = detail.name
    form.thresholdAmount = Number(detail.thresholdAmount)
    form.discountAmount = Number(detail.discountAmount)
    form.totalStock = detail.totalStock
    form.claimStartAt = detail.claimStartAt
    form.claimEndAt = detail.claimEndAt
    form.validFrom = detail.validFrom
    form.validUntil = detail.validUntil
  } catch (error) {
    dialogVisible.value = false
    ElMessage.error(getRequestErrorMessage(error, '活动详情加载失败'))
  } finally {
    detailLoading.value = false
  }
}

function validateForm(): string {
  if (!form.name.trim()) return '请输入活动名称'
  if (form.discountAmount <= 0) return '优惠金额必须大于 0'
  if (form.thresholdAmount < form.discountAmount) return '使用门槛不能小于优惠金额'
  if (form.totalStock <= 0) return '发行量必须大于 0'
  if (!form.claimStartAt || !form.claimEndAt || !form.validFrom || !form.validUntil) return '请填写完整的活动时间'
  if (form.claimStartAt >= form.claimEndAt) return '领取开始时间必须早于结束时间'
  if (form.validFrom >= form.validUntil) return '生效时间必须早于失效时间'
  if (form.validUntil <= form.claimStartAt) return '失效时间必须晚于领取开始时间'
  return ''
}

async function saveActivity(): Promise<void> {
  const message = validateForm()
  if (message) {
    ElMessage.warning(message)
    return
  }

  saving.value = true
  try {
    const payload = { ...form, name: form.name.trim() }
    if (editingId.value) await updateCouponActivity(editingId.value, payload)
    else await createCouponActivity(payload)
    ElMessage.success(editingId.value ? '活动已更新' : '活动草稿已创建')
    dialogVisible.value = false
    if (!editingId.value) currentPage.value = 1
    await loadActivities()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '活动保存失败'))
  } finally {
    saving.value = false
  }
}

async function changeStatus(row: CouponActivity, action: 'publish' | 'cancel'): Promise<void> {
  const actionLabel = action === 'publish' ? '发布' : '取消'
  try {
    await ElMessageBox.confirm(`确定${actionLabel}活动“${row.name}”吗？`, `${actionLabel}活动`, {
      confirmButtonText: `确认${actionLabel}`,
      cancelButtonText: '返回',
      type: action === 'cancel' ? 'warning' : 'info',
    })
  } catch {
    return
  }

  try {
    if (action === 'publish') await publishCouponActivity(row.id)
    else await cancelCouponActivity(row.id)
    ElMessage.success(`活动已${actionLabel}`)
    await loadActivities()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, `${actionLabel}活动失败`))
  }
}

onMounted(loadActivities)
</script>

<template>
  <section class="management-page coupon-page">
    <header class="management-heading">
      <div><h1>优惠券活动</h1><p>创建满减券活动，并管理活动发布与取消状态。</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建活动</el-button>
    </header>

    <section class="management-filter" aria-label="优惠券活动筛选条件">
      <div class="management-filter__field">
        <label for="coupon-keyword">活动名称</label>
        <el-input id="coupon-keyword" v-model="keyword" maxlength="100" clearable placeholder="输入活动名称" @keyup.enter="search" />
      </div>
      <div class="management-filter__field compact-field">
        <label for="coupon-status">活动状态</label>
        <el-select id="coupon-status" v-model="status" clearable placeholder="全部状态">
          <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
        </el-select>
      </div>
      <div class="management-filter__actions">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshRight" @click="reset">重置</el-button>
      </div>
    </section>

    <section class="management-table" aria-label="优惠券活动列表">
      <el-table v-loading="loading" :data="activities" row-key="id" empty-text="暂无优惠券活动">
        <el-table-column prop="name" label="活动名称" min-width="170" />
        <el-table-column label="优惠规则" min-width="140"><template #default="scope">满 {{ formatMoney(scope.row.thresholdAmount) }} 减 {{ formatMoney(scope.row.discountAmount) }}</template></el-table-column>
        <el-table-column label="剩余/总量" min-width="100"><template #default="scope">{{ scope.row.remainingStock }}/{{ scope.row.totalStock }}</template></el-table-column>
        <el-table-column label="领取时间" min-width="220"><template #default="scope"><div>{{ formatDateTime(scope.row.claimStartAt) }}</div><small>至 {{ formatDateTime(scope.row.claimEndAt) }}</small></template></el-table-column>
        <el-table-column label="有效期" min-width="220"><template #default="scope"><div>{{ formatDateTime(scope.row.validFrom) }}</div><small>至 {{ formatDateTime(scope.row.validUntil) }}</small></template></el-table-column>
        <el-table-column label="状态" min-width="90"><template #default="scope"><el-tag :type="statusTagTypes[scope.row.status as CouponActivityStatus]">{{ statusLabels[scope.row.status as CouponActivityStatus] }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.status === 'DRAFT'" link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-if="scope.row.status === 'DRAFT'" link type="primary" @click="changeStatus(scope.row, 'publish')">发布</el-button>
            <el-button v-if="scope.row.status === 'DRAFT' || scope.row.status === 'PUBLISHED'" link type="danger" @click="changeStatus(scope.row, 'cancel')">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <footer class="management-table__footer">
        <span>共 {{ total }} 条</span>
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20]" layout="sizes, prev, pager, next" background @current-change="loadActivities" @size-change="currentPage = 1; loadActivities()" />
      </footer>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑活动草稿' : '新建优惠券活动'" width="720px" class="management-dialog coupon-dialog" :close-on-click-modal="false">
      <div v-loading="detailLoading">
        <el-form label-position="top">
          <el-form-item label="活动名称"><el-input v-model="form.name" maxlength="100" placeholder="例如：暑期满100减20券" /></el-form-item>
          <div class="coupon-form-grid">
            <el-form-item label="使用门槛"><el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" /></el-form-item>
            <el-form-item label="优惠金额"><el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" /></el-form-item>
            <el-form-item label="发行总量"><el-input-number v-model="form.totalStock" :min="1" :max="1000000" /></el-form-item>
            <span />
            <el-form-item label="领取开始时间"><el-date-picker v-model="form.claimStartAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" /></el-form-item>
            <el-form-item label="领取结束时间"><el-date-picker v-model="form.claimEndAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" /></el-form-item>
            <el-form-item label="生效时间"><el-date-picker v-model="form.validFrom" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" /></el-form-item>
            <el-form-item label="失效时间"><el-date-picker v-model="form.validUntil" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="选择时间" /></el-form-item>
          </div>
        </el-form>
      </div>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveActivity">保存草稿</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.coupon-page .compact-field { width: 190px; min-width: 190px; }
.coupon-page .management-filter__field :deep(.el-select) { width: 100%; }
.management-table small { color: #7a8490; }
.coupon-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 18px; }
.coupon-form-grid :deep(.el-date-editor), .coupon-form-grid :deep(.el-input-number) { width: 100%; }
@media (max-width: 680px) {
  .coupon-page .compact-field { width: 100%; }
  .management-table :deep(.el-table) { min-width: 1150px; }
  .coupon-form-grid { grid-template-columns: 1fr; }
  .coupon-form-grid > span { display: none; }
}
</style>
