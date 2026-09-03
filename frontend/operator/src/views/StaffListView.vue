<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { changeStaffStatus, getStaffPage } from '../api/staff'
import StaffFormDialog from '../components/StaffFormDialog.vue'
import StaffPasswordDialog from '../components/StaffPasswordDialog.vue'
import type { AccountStatus, StaffAccount, StaffStatusFilter } from '../types/staff'
import { getRequestErrorMessage } from '../utils/request'

interface StaffQuery {
  keyword: string
  status: StaffStatusFilter
}

const staffAccounts = ref<StaffAccount[]>([])
const total = ref(0)
const loading = ref(false)
const statusChangingId = ref<number | null>(null)
const staffDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const editingStaff = ref<StaffAccount | null>(null)
const passwordStaff = ref<StaffAccount | null>(null)

const draftQuery = reactive<StaffQuery>({ keyword: '', status: '' })
const appliedQuery = reactive<StaffQuery>({ keyword: '', status: '' })
const currentPage = ref(1)
const pageSize = ref(10)
let requestSequence = 0

async function loadStaff(): Promise<void> {
  const currentRequest = ++requestSequence
  loading.value = true

  try {
    const keyword = appliedQuery.keyword.trim()
    const result = await getStaffPage({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword || undefined,
      status: appliedQuery.status || undefined,
    })

    if (currentRequest !== requestSequence) return
    staffAccounts.value = result.items
    total.value = result.total
    currentPage.value = result.page
    pageSize.value = result.size
  } catch (error) {
    if (currentRequest !== requestSequence) return
    staffAccounts.value = []
    total.value = 0
    ElMessage.error(getRequestErrorMessage(error, '工作人员列表加载失败'))
  } finally {
    if (currentRequest === requestSequence) loading.value = false
  }
}

async function handleSearch(): Promise<void> {
  appliedQuery.keyword = draftQuery.keyword.trim()
  appliedQuery.status = draftQuery.status
  currentPage.value = 1
  await loadStaff()
}

async function handleReset(): Promise<void> {
  draftQuery.keyword = ''
  draftQuery.status = ''
  await handleSearch()
}

async function handlePageSizeChange(size: number): Promise<void> {
  pageSize.value = size
  currentPage.value = 1
  await loadStaff()
}

async function handlePageChange(page: number): Promise<void> {
  currentPage.value = page
  await loadStaff()
}

function openCreateDialog(): void {
  editingStaff.value = null
  staffDialogVisible.value = true
}

function openEditDialog(staff: StaffAccount): void {
  editingStaff.value = staff
  staffDialogVisible.value = true
}

function openPasswordDialog(staff: StaffAccount): void {
  passwordStaff.value = staff
  passwordDialogVisible.value = true
}

async function handleStaffSaved(mode: 'create' | 'edit'): Promise<void> {
  if (mode === 'create') currentPage.value = 1
  await loadStaff()
}

async function toggleStatus(staff: StaffAccount): Promise<void> {
  const nextStatus: AccountStatus = staff.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const actionLabel = nextStatus === 'ACTIVE' ? '启用' : '停用'

  try {
    await ElMessageBox.confirm(
      `确定${actionLabel}工作人员“${staff.displayName}”吗？`,
      `${actionLabel}账号`,
      {
        confirmButtonText: `确认${actionLabel}`,
        cancelButtonText: '取消',
        type: nextStatus === 'ACTIVE' ? 'success' : 'warning',
      },
    )
  } catch {
    return
  }

  statusChangingId.value = staff.id
  try {
    await changeStaffStatus(staff.id, nextStatus)
    ElMessage.success(`已${actionLabel}${staff.displayName}的账号`)
    await loadStaff()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, `${actionLabel}工作人员失败`))
  } finally {
    statusChangingId.value = null
  }
}

onMounted(() => {
  void loadStaff()
})
</script>

<template>
  <section class="staff-page">
    <header class="page-heading" aria-labelledby="staff-page-title">
      <div>
        <h1 id="staff-page-title">工作人员管理</h1>
        <p>管理当前景点的核销工作人员账号。</p>
      </div>
      <el-button class="create-button" type="primary" :icon="Plus" @click="openCreateDialog">
        新建工作人员
      </el-button>
    </header>

    <section class="filter-panel" aria-label="工作人员筛选条件">
      <div class="filter-field keyword-field">
        <label for="staff-keyword">关键字</label>
        <el-input
          id="staff-keyword"
          v-model="draftQuery.keyword"
          clearable
          placeholder="登录名或姓名"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
        />
      </div>

      <div class="filter-field status-field">
        <span class="field-label">账号状态</span>
        <el-radio-group v-model="draftQuery.status" class="status-selector">
          <el-radio-button value="">全部状态</el-radio-button>
          <el-radio-button value="ACTIVE">启用</el-radio-button>
          <el-radio-button value="DISABLED">停用</el-radio-button>
        </el-radio-group>
      </div>

      <div class="filter-actions">
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
      </div>
    </section>

    <section class="table-panel" aria-label="工作人员列表">
      <el-table
        v-loading="loading"
        :data="staffAccounts"
        row-key="id"
        class="staff-table"
        empty-text="暂无符合条件的工作人员"
      >
        <el-table-column prop="loginName" label="登录账号" min-width="145" />
        <el-table-column prop="displayName" label="工作人员" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="150" />
        <el-table-column label="账号状态" min-width="125">
          <template #default="scope">
            <el-tag
              :type="scope.row.status === 'ACTIVE' ? 'success' : 'danger'"
              effect="light"
              size="large"
            >
              {{ scope.row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" min-width="250" fixed="right">
          <template #default="scope">
            <div class="row-actions">
              <el-button link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
              <el-button link type="primary" @click="openPasswordDialog(scope.row)">重置密码</el-button>
              <el-button
                link
                :type="scope.row.status === 'ACTIVE' ? 'danger' : 'primary'"
                :loading="statusChangingId === scope.row.id"
                :disabled="statusChangingId !== null && statusChangingId !== scope.row.id"
                @click="toggleStatus(scope.row)"
              >
                {{ scope.row.status === 'ACTIVE' ? '停用' : '启用' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <footer class="table-footer">
        <div class="result-summary">
          <span>共 {{ total }} 条</span>
          <el-select
            :model-value="pageSize"
            class="page-size-select"
            aria-label="每页显示数量"
            @change="handlePageSizeChange"
          >
            <el-option :value="10" label="10 条/页" />
            <el-option :value="20" label="20 条/页" />
          </el-select>
        </div>

        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
        />
      </footer>
    </section>

    <StaffFormDialog v-model="staffDialogVisible" :staff="editingStaff" @saved="handleStaffSaved" />
    <StaffPasswordDialog v-model="passwordDialogVisible" :staff="passwordStaff" />
  </section>
</template>

<style scoped>
.staff-page {
  --brand: #e99000;
  --brand-dark: #d67f00;
  --surface: #fff;
  --text: #202733;
  --muted: #75808e;
  --border: #dfe3e8;

  min-width: 0;
  color: var(--text);
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-heading h1 {
  margin: 0 0 8px;
  font-size: 27px;
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.page-heading p {
  margin: 0;
  font-size: 14px;
  color: var(--muted);
}

.create-button {
  min-height: 42px;
  padding: 0 19px;
  font-size: 14px;
  font-weight: 600;
}

.filter-panel {
  display: flex;
  gap: 30px;
  align-items: flex-end;
  padding: 22px 24px;
  margin-bottom: 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 9px;
  box-shadow: 0 2px 7px rgb(32 39 51 / 3%);
}

.filter-field {
  display: grid;
  gap: 9px;
}

.filter-field label,
.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #4a5360;
}

.keyword-field {
  width: 260px;
}

.status-field {
  min-width: 290px;
}

.filter-actions {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.table-panel {
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 9px;
  box-shadow: 0 3px 10px rgb(32 39 51 / 4%);
}

.staff-table {
  width: 100%;
}

.staff-table :deep(.el-table__header-wrapper th.el-table__cell) {
  height: 56px;
  padding: 0;
  font-size: 13px;
  font-weight: 650;
  color: #313944;
  background: #fafbfc;
}

.staff-table :deep(.el-table__body td.el-table__cell) {
  height: 54px;
  padding: 0;
  font-size: 14px;
  color: #444d59;
}

.staff-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.staff-table :deep(.el-tag) {
  min-width: 54px;
  height: 30px;
  justify-content: center;
  font-size: 13px;
  border-radius: 5px;
}

.row-actions {
  display: flex;
  align-items: center;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  margin: 0 16px 0 0;
  font-size: 13px;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72px;
  padding: 0 22px;
  border-top: 1px solid #ebeef2;
}

.result-summary {
  display: flex;
  gap: 16px;
  align-items: center;
  font-size: 13px;
  color: #5f6975;
}

.page-size-select {
  width: 108px;
}

.staff-page :deep(.el-button--primary) {
  --el-button-bg-color: var(--brand);
  --el-button-border-color: var(--brand);
  --el-button-hover-bg-color: #f0a11a;
  --el-button-hover-border-color: #f0a11a;
  --el-button-active-bg-color: var(--brand-dark);
  --el-button-active-border-color: var(--brand-dark);
}

.staff-page :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  color: var(--brand-dark);
  background: #fff8eb;
  border-color: var(--brand);
  box-shadow: -1px 0 0 0 var(--brand);
}

.staff-page :deep(.el-radio-button__inner) {
  min-width: 84px;
  height: 40px;
  padding: 0 16px;
  font-size: 13px;
  line-height: 38px;
}

.staff-page :deep(.el-input__wrapper),
.staff-page :deep(.el-select__wrapper) {
  min-height: 40px;
  border-radius: 6px;
  box-shadow: 0 0 0 1px #d9dee5 inset;
}

.staff-page :deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--brand);
}

@media (max-width: 1180px) {
  .filter-panel {
    flex-wrap: wrap;
    gap: 18px 24px;
  }
}

@media (max-width: 680px) {
  .page-heading {
    gap: 16px;
    align-items: center;
  }

  .page-heading h1 {
    font-size: 24px;
  }

  .create-button {
    flex: 0 0 auto;
    min-height: 40px;
    padding: 0 14px;
  }

  .filter-panel {
    gap: 18px;
    padding: 19px 18px;
  }

  .keyword-field,
  .status-field {
    width: 100%;
    min-width: 0;
  }

  .status-selector {
    display: flex;
  }

  .status-selector :deep(.el-radio-button) {
    flex: 1;
  }

  .status-selector :deep(.el-radio-button__inner) {
    width: 100%;
    min-width: 0;
    padding: 0 8px;
  }

  .filter-actions {
    width: 100%;
    margin-left: 0;
    justify-content: flex-end;
  }

  .table-panel {
    overflow-x: auto;
  }

  .staff-table {
    min-width: 820px;
  }

  .table-footer {
    gap: 14px;
    min-width: 620px;
  }
}
</style>
