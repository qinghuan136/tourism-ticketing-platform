<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormItemRule, type FormRules } from 'element-plus'
import { resetStaffPassword } from '../api/staff'
import type { StaffAccount } from '../types/staff'
import { getRequestErrorMessage } from '../utils/request'

interface PasswordForm {
  password: string
  confirmPassword: string
}

const props = defineProps<{
  modelValue: boolean
  staff: StaffAccount | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const submitError = ref('')
const form = reactive<PasswordForm>({
  password: '',
  confirmPassword: '',
})

const confirmPasswordRule: FormItemRule = {
  validator: (_rule, value: unknown, callback) => {
    if (!value) {
      callback(new Error('请再次输入新密码'))
      return
    }
    if (value !== form.password) {
      callback(new Error('两次输入的密码不一致'))
      return
    }
    callback()
  },
  trigger: ['blur', 'change'],
}

const rules: FormRules<PasswordForm> = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度必须在 6 到 50 个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [confirmPasswordRule],
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    form.password = ''
    form.confirmPassword = ''
    submitError.value = ''
    await nextTick()
    formRef.value?.clearValidate()
  },
)

function closeDialog(): void {
  if (submitting.value) return
  emit('update:modelValue', false)
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value || !props.staff || submitting.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitError.value = ''
  submitting.value = true
  try {
    await resetStaffPassword(props.staff.id, { password: form.password })
    ElMessage.success(`已重置${props.staff.displayName}的登录密码`)
    emit('saved')
    emit('update:modelValue', false)
  } catch (error) {
    submitError.value = getRequestErrorMessage(error, '重置密码失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="重置工作人员密码"
    width="460px"
    class="staff-password-dialog"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @close="closeDialog"
  >
    <div v-if="staff" class="account-summary">
      <span class="account-avatar" aria-hidden="true">{{ staff.displayName.slice(0, 1) }}</span>
      <div>
        <strong>{{ staff.displayName }}</strong>
        <p>登录账号：{{ staff.loginName }}</p>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      hide-required-asterisk
      class="password-form"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="新密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          maxlength="50"
          autocomplete="new-password"
          placeholder="请输入 6–50 位新密码"
        />
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          maxlength="50"
          autocomplete="new-password"
          placeholder="请再次输入新密码"
        />
      </el-form-item>

      <el-alert
        v-if="submitError"
        :title="submitError"
        type="error"
        show-icon
        :closable="false"
      />
    </el-form>

    <template #footer>
      <div class="dialog-actions">
        <el-button :disabled="submitting" @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认重置</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.account-summary {
  display: flex;
  gap: 13px;
  align-items: center;
  padding: 14px 16px;
  margin-bottom: 22px;
  background: #f6f8fa;
  border: 1px solid #e7ebef;
  border-radius: 8px;
}

.account-avatar {
  display: grid;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  place-items: center;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: #e99000;
  border-radius: 50%;
}

.account-summary strong {
  font-size: 14px;
  color: #303845;
}

.account-summary p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #75808e;
}

.password-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.password-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-size: 13px;
  font-weight: 650;
  color: #3f4855;
}

.password-form :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 6px;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.dialog-actions :deep(.el-button) {
  min-width: 92px;
}

:global(.staff-password-dialog.el-dialog) {
  max-width: calc(100vw - 32px);
  border-radius: 10px;
}

:global(.staff-password-dialog .el-dialog__header) {
  padding: 22px 24px 16px;
  margin: 0;
  border-bottom: 1px solid #edf0f3;
}

:global(.staff-password-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #252d38;
}

:global(.staff-password-dialog .el-dialog__body) {
  padding: 22px 24px 6px;
}

:global(.staff-password-dialog .el-dialog__footer) {
  padding: 16px 24px 20px;
  border-top: 1px solid #edf0f3;
}

:global(.staff-password-dialog .el-button--primary) {
  --el-button-bg-color: #e99000;
  --el-button-border-color: #e99000;
  --el-button-hover-bg-color: #f0a11a;
  --el-button-hover-border-color: #f0a11a;
  --el-button-active-bg-color: #d67f00;
  --el-button-active-border-color: #d67f00;
}
</style>
