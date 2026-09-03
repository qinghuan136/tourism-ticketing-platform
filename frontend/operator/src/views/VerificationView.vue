<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { verifyTicket } from '../api/verification'
import type { VerificationResponse } from '../types/verification'
import { getRequestErrorMessage } from '../utils/request'
import '../assets/management.css'

const ticketCode = ref('')
const deviceNo = ref('')
const submitting = ref(false)
const result = ref<VerificationResponse | null>(null)
let pendingRequestNo = ''

const verificationSucceeded = computed(() => result.value?.result === 'SUCCESS')

function createRequestNo(): string {
  return `VERIFY-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`
}

function formatDateTime(value: string): string {
  return value.replace('T', ' ')
}

async function submitVerification(): Promise<void> {
  const code = ticketCode.value.trim()
  if (!code) {
    ElMessage.warning('请输入电子票码')
    return
  }

  pendingRequestNo ||= createRequestNo()
  submitting.value = true
  result.value = null
  try {
    result.value = await verifyTicket({
      requestNo: pendingRequestNo,
      ticketCode: code,
      deviceNo: deviceNo.value.trim() || undefined,
    })
    pendingRequestNo = ''
    if (result.value.result === 'SUCCESS') ticketCode.value = ''
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '核销请求提交失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="management-page verification-page">
    <header class="management-heading">
      <div>
        <h1>核销工作台</h1>
        <p>输入游客电子票码，确认票券状态并完成入园核销。</p>
      </div>
    </header>

    <div class="verification-layout">
      <section class="verification-panel" aria-labelledby="verification-form-title">
        <h2 id="verification-form-title">票码核销</h2>
        <p>请使用扫码设备录入，或手动输入完整票码。</p>

        <el-form label-position="top" @submit.prevent="submitVerification">
          <el-form-item label="电子票码">
            <el-input
              v-model="ticketCode"
              size="large"
              maxlength="64"
              autofocus
              clearable
              placeholder="请输入电子票码"
              @input="pendingRequestNo = ''"
              @keyup.enter="submitVerification"
            />
          </el-form-item>
          <el-form-item label="设备编号（选填）">
            <el-input v-model="deviceNo" maxlength="64" clearable placeholder="例如：GATE-01" />
          </el-form-item>
          <el-button class="verify-button" type="primary" native-type="submit" :loading="submitting">
            确认核销
          </el-button>
        </el-form>
      </section>

      <section v-if="result" class="verification-result" :class="verificationSucceeded ? 'success' : 'failed'" aria-live="polite">
        <div class="result-heading">
          <el-icon><CircleCheck v-if="verificationSucceeded" /><CircleClose v-else /></el-icon>
          <div>
            <h2>{{ verificationSucceeded ? '核销成功' : '核销失败' }}</h2>
            <p>{{ verificationSucceeded ? '该票券已完成入园核销' : result.failureReason }}</p>
          </div>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="游客姓名">{{ result.ticket.visitorName }}</el-descriptions-item>
          <el-descriptions-item label="票种">{{ result.ticket.ticketTypeName }}</el-descriptions-item>
          <el-descriptions-item label="参观场次">
            {{ result.ticket.visitDate }} {{ result.ticket.startTime }}–{{ result.ticket.endTime }}
          </el-descriptions-item>
          <el-descriptions-item label="票码">{{ result.ticket.ticketCode }}</el-descriptions-item>
          <el-descriptions-item label="核销时间">{{ formatDateTime(result.verifiedAt) }}</el-descriptions-item>
        </el-descriptions>
      </section>

      <section v-else class="verification-placeholder" aria-live="polite">
        <el-icon><CircleCheck /></el-icon>
        <strong>等待核销</strong>
        <span>核销结果会显示在这里</span>
      </section>
    </div>
  </section>
</template>

<style scoped>
.verification-layout {
  display: grid;
  grid-template-columns: minmax(360px, 520px) minmax(360px, 1fr);
  gap: 22px;
  align-items: start;
}

.verification-panel,
.verification-result,
.verification-placeholder {
  padding: 28px;
  background: #fff;
  border: 1px solid #dfe3e8;
  border-radius: 9px;
}

.verification-panel h2,
.verification-result h2 {
  margin: 0;
  font-size: 20px;
}

.verification-panel > p {
  margin: 8px 0 24px;
  font-size: 14px;
  color: #75808e;
}

.verification-panel :deep(.el-form-item__label) {
  font-weight: 600;
  color: #3f4855;
}

.verify-button {
  width: 100%;
  min-height: 46px;
  margin-top: 4px;
}

.result-heading {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 24px;
}

.result-heading .el-icon {
  flex: 0 0 auto;
  font-size: 38px;
}

.result-heading p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #697481;
}

.verification-result.success {
  border-top: 4px solid #27a567;
}

.verification-result.success .result-heading .el-icon {
  color: #27a567;
}

.verification-result.failed {
  border-top: 4px solid #dc4c4c;
}

.verification-result.failed .result-heading .el-icon {
  color: #dc4c4c;
}

.verification-placeholder {
  display: grid;
  min-height: 310px;
  color: #98a1ab;
  place-content: center;
  justify-items: center;
}

.verification-placeholder .el-icon {
  margin-bottom: 14px;
  font-size: 46px;
  color: #c8ced5;
}

.verification-placeholder strong {
  margin-bottom: 6px;
  font-size: 17px;
  color: #68727e;
}

.verification-placeholder span {
  font-size: 13px;
}

@media (max-width: 900px) {
  .verification-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .verification-panel,
  .verification-result,
  .verification-placeholder {
    padding: 22px 18px;
  }
}
</style>
