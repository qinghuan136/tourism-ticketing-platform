<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import { createTicketType, deleteTicketType, getTicketTypePage, updateTicketType } from '../api/ticketType'
import type { TicketType, TicketTypeWriteRequest } from '../types/ticketType'
import { getRequestErrorMessage } from '../utils/request'
import '../assets/management.css'

const ticketTypes = ref<TicketType[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const appliedKeyword = ref('')
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive<TicketTypeWriteRequest>({
  name: '',
  description: '',
  audienceRule: '',
  basePrice: 0,
  status: 'ENABLED',
})

const rules: FormRules<TicketTypeWriteRequest> = {
  name: [
    { required: true, whitespace: true, message: '请输入票种名称', trigger: 'blur' },
    { max: 100, message: '票种名称不能超过 100 个字符', trigger: 'blur' },
  ],
  basePrice: [{ required: true, message: '请输入基础价格', trigger: 'change' }],
}

async function loadTicketTypes(): Promise<void> {
  loading.value = true
  try {
    const result = await getTicketTypePage({
      keyword: appliedKeyword.value || undefined,
      page: currentPage.value,
      size: pageSize.value,
    })
    ticketTypes.value = result.items
    total.value = result.total
    currentPage.value = result.page
    pageSize.value = result.size
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '票种列表加载失败'))
  } finally {
    loading.value = false
  }
}

function openDialog(ticketType?: TicketType): void {
  editingId.value = ticketType?.id ?? null
  form.name = ticketType?.name ?? ''
  form.description = ticketType?.description ?? ''
  form.audienceRule = ticketType?.audienceRule ?? ''
  form.basePrice = ticketType?.basePrice ?? 0
  form.status = ticketType?.status ?? 'ENABLED'
  dialogVisible.value = true
}

async function saveTicketType(): Promise<void> {
  if (!formRef.value || saving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  const payload = {
    ...form,
    name: form.name.trim(),
    description: form.description.trim(),
    audienceRule: form.audienceRule.trim(),
  }
  try {
    if (editingId.value) await updateTicketType(editingId.value, payload)
    else await createTicketType(payload)
    ElMessage.success(editingId.value ? '票种已更新' : '票种已创建')
    dialogVisible.value = false
    if (!editingId.value) currentPage.value = 1
    await loadTicketTypes()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '票种保存失败'))
  } finally {
    saving.value = false
  }
}

async function removeTicketType(ticketType: TicketType): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除票种“${ticketType.name}”吗？`, '删除票种', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await deleteTicketType(ticketType.id)
    ElMessage.success('票种已删除')
    await loadTicketTypes()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '票种删除失败'))
  }
}

async function search(): Promise<void> {
  appliedKeyword.value = keyword.value.trim()
  currentPage.value = 1
  await loadTicketTypes()
}

async function reset(): Promise<void> {
  keyword.value = ''
  await search()
}

onMounted(loadTicketTypes)
</script>

<template>
  <section class="management-page ticket-type-page">
    <header class="management-heading">
      <div>
        <h1>票种管理</h1>
        <p>维护当前景点可复用的基础票种。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openDialog()">新建票种</el-button>
    </header>

    <section class="management-filter" aria-label="票种筛选条件">
      <div class="management-filter__field">
        <label for="ticket-keyword">票种名称</label>
        <el-input id="ticket-keyword" v-model="keyword" clearable placeholder="请输入票种名称" @keyup.enter="search" />
      </div>
      <div class="management-filter__actions">
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshRight" @click="reset">重置</el-button>
      </div>
    </section>

    <section class="management-table" aria-label="票种列表">
      <el-table v-loading="loading" :data="ticketTypes" row-key="id" empty-text="暂无票种">
        <el-table-column prop="name" label="票种名称" min-width="150" />
        <el-table-column label="基础价格" min-width="120">
          <template #default="scope">¥ {{ Number(scope.row.basePrice).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="audienceRule" label="适用规则" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" min-width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">
              {{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="removeTicketType(scope.row)">删除</el-button>
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
          @current-change="loadTicketTypes"
          @size-change="currentPage = 1; loadTicketTypes()"
        />
      </footer>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑票种' : '新建票种'"
      width="560px"
      class="management-dialog"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="票种名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="基础价格" prop="basePrice">
          <el-input-number v-model="form.basePrice" :min="0" :precision="2" :max="99999999.99" />
        </el-form-item>
        <el-form-item label="适用规则">
          <el-input v-model="form.audienceRule" maxlength="500" placeholder="例如：18 周岁及以上游客" />
        </el-form-item>
        <el-form-item label="票种描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
        <el-form-item label="票种状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTicketType">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.ticket-type-page :deep(.el-input-number) {
  width: 100%;
}

@media (max-width: 680px) {
  .management-table :deep(.el-table) {
    min-width: 820px;
  }
}
</style>
