<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listVisitors, updateVisitorStatus } from '../api/visitor'
import VisitorFormDialog from '../components/VisitorFormDialog.vue'
import type { Visitor } from '../types/visitor'
import { getRequestErrorMessage } from '../utils/request'

const visitors = ref<Visitor[]>([])
const loading = ref(false)
const errorMessage = ref('')
const dialogVisible = ref(false)
const editingVisitor = ref<Visitor | null>(null)
const statusChangingId = ref<number | null>(null)

const idTypeText: Record<string, string> = { ID_CARD: '身份证', PASSPORT: '护照' }

async function loadData(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    visitors.value = await listVisitors()
  } catch (error) {
    visitors.value = []
    errorMessage.value = getRequestErrorMessage(error, '参观人加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate(): void {
  editingVisitor.value = null
  dialogVisible.value = true
}

function openEdit(visitor: Visitor): void {
  editingVisitor.value = visitor
  dialogVisible.value = true
}

async function toggleStatus(visitor: Visitor): Promise<void> {
  if (statusChangingId.value !== null) return
  statusChangingId.value = visitor.id
  try {
    const status = visitor.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
    await updateVisitorStatus(visitor.id, status)
    ElMessage.success(status === 'ACTIVE' ? '参观人已启用' : '参观人已停用')
    await loadData()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '状态修改失败'))
  } finally {
    statusChangingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <div class="visitor-page">
    <header class="page-heading">
      <div><h1>常用参观人</h1><p>维护预约时可快速选择的参观人信息</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增参观人</el-button>
    </header>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
    <section v-loading="loading" class="visitor-list">
      <article v-for="visitor in visitors" :key="visitor.id" :class="{ disabled: visitor.status === 'DISABLED' }">
        <div class="visitor-title">
          <h2>{{ visitor.name }}</h2>
          <span>{{ visitor.status === 'ACTIVE' ? '正常' : '已停用' }}</span>
        </div>
        <dl>
          <div><dt>证件类型</dt><dd>{{ idTypeText[visitor.idType] || visitor.idType }}</dd></div>
          <div><dt>证件号码</dt><dd>{{ visitor.maskedIdNumber }}</dd></div>
          <div><dt>手机号</dt><dd>{{ visitor.phone || '未填写' }}</dd></div>
        </dl>
        <div class="visitor-actions">
          <button type="button" @click="openEdit(visitor)">编辑</button>
          <button type="button" :disabled="statusChangingId === visitor.id" @click="toggleStatus(visitor)">
            {{ visitor.status === 'ACTIVE' ? '停用' : '启用' }}
          </button>
        </div>
      </article>
    </section>
    <el-empty v-if="!loading && !errorMessage && visitors.length === 0" description="还没有常用参观人">
      <el-button type="primary" @click="openCreate">新增参观人</el-button>
    </el-empty>

    <VisitorFormDialog v-model:visible="dialogVisible" :visitor="editingVisitor" @saved="loadData" />
  </div>
</template>

<style scoped>
.visitor-page { color: #1c2822; }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.page-heading h1 { margin: 0; font-size: 34px; }
.page-heading p { margin: 9px 0 0; color: #77817c; }
.page-heading .el-button { min-height: 42px; --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
.visitor-list { display: grid; min-height: 140px; grid-template-columns: repeat(2,1fr); gap: 14px; }
.visitor-list article { position: relative; padding: 22px; background: #fff; border: 1px solid #dfe4e1; border-radius: 11px; }
.visitor-list article.disabled { background: #f5f6f5; }
.visitor-title { display: flex; align-items: center; justify-content: space-between; padding-right: 118px; }
.visitor-title h2 { margin: 0; font-size: 21px; }
.visitor-title span { color: #28815f; font-size: 13px; }
.disabled .visitor-title span { color: #858d89; }
.visitor-list dl { display: grid; gap: 10px; margin: 20px 0 0; }
.visitor-list dl div { display: grid; grid-template-columns: 82px 1fr; gap: 12px; }
.visitor-list dt { color: #7a837f; }
.visitor-list dd { margin: 0; overflow-wrap: anywhere; }
.visitor-actions { position: absolute; top: 22px; right: 20px; display: flex; gap: 0; }
.visitor-actions button { padding: 1px 11px; color: #626c67; cursor: pointer; background: none; border: 0; border-right: 1px solid #d9dddb; }
.visitor-actions button:last-child { color: #e46f00; border-right: 0; }
.visitor-actions button:disabled { cursor: wait; opacity: .5; }
@media (max-width: 760px) { .visitor-list { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .page-heading { align-items: flex-start; } .page-heading h1 { font-size: 29px; } .page-heading p { max-width: 220px; font-size: 14px; line-height: 1.6; } .page-heading .el-button { flex: none; padding: 0 13px; } .visitor-list article { padding: 19px; } .visitor-title { padding-right: 102px; } .visitor-actions { top: 20px; right: 12px; } }
</style>
