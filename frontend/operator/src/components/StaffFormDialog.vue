<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormItemRule, type FormRules } from 'element-plus'
import { createStaff, updateStaff } from '../api/staff'
import type { StaffAccount, StaffCreateRequest } from '../types/staff'
import { getRequestErrorMessage } from '../utils/request'

const props = defineProps<{
  modelValue: boolean
  staff: StaffAccount | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [mode: 'create' | 'edit']
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const submitError = ref('')
const isEdit = computed(() => props.staff !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑工作人员' : '新建工作人员'))

const form = reactive<StaffCreateRequest>({
  loginName: '',
  password: '',
  displayName: '',
  phone: '',
})

function requiredTrimmed(message: string): FormItemRule {
  return {
    validator: (_rule, value: unknown, callback) => {
      if (typeof value !== 'string' || !value.trim()) {
        callback(new Error(message))
        return
      }
      callback()
    },
    trigger: 'blur',
  }
}

const rules: FormRules<StaffCreateRequest> = {
  loginName: [
    requiredTrimmed('请输入登录账号'),
    { max: 50, message: '登录账号不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 50, message: '密码长度必须在 6 到 50 个字符之间', trigger: 'blur' },
  ],
  displayName: [
    requiredTrimmed('请输入工作人员姓名'),
    { max: 50, message: '工作人员姓名不能超过 50 个字符', trigger: 'blur' },
  ],
  phone: [
    requiredTrimmed('请输入手机号'),
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
}

function resetForm(): void {
  form.loginName = props.staff?.loginName ?? ''
  form.password = ''
  form.displayName = props.staff?.displayName ?? ''
  form.phone = props.staff?.phone ?? ''
  submitError.value = ''
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    await nextTick()
    resetForm()
  },
)

function closeDialog(): void {
  if (submitting.value) return
  emit('update:modelValue', false)
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value || submitting.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitError.value = ''
  submitting.value = true
  try {
    if (props.staff) {
      await updateStaff(props.staff.id, {
        displayName: form.displayName.trim(),
        phone: form.phone.trim(),
      })
      ElMessage.success('工作人员资料已更新')
      emit('saved', 'edit')
    } else {
      await createStaff({
        loginName: form.loginName.trim(),
        password: form.password,
        displayName: form.displayName.trim(),
        phone: form.phone.trim(),
      })
      ElMessage.success('工作人员账号已创建')
      emit('saved', 'create')
    }
    emit('update:modelValue', false)
  } catch (error) {
    submitError.value = getRequestErrorMessage(error, `${dialogTitle.value}失败，请稍后重试`)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="520px"
    class="staff-form-dialog"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    @close="closeDialog"
  >
    <p class="dialog-intro">
      {{ isEdit ? '登录账号不可修改，请维护工作人员的姓名和手机号。' : '创建后账号默认启用，可用于登录核销工作台。' }}
    </p>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      hide-required-asterisk
      class="staff-form"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="登录账号" prop="loginName">
        <el-input
          v-model="form.loginName"
          :disabled="isEdit"
          maxlength="50"
          autocomplete="off"
          placeholder="例如：staff_zhang"
        />
      </el-form-item>

      <el-form-item v-if="!isEdit" label="初始密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          maxlength="50"
          autocomplete="new-password"
          placeholder="请输入 6–50 位初始密码"
        />
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="工作人员姓名" prop="displayName">
          <el-input
            v-model="form.displayName"
            maxlength="50"
            autocomplete="off"
            placeholder="请输入姓名"
          />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            maxlength="11"
            inputmode="numeric"
            autocomplete="tel"
            placeholder="请输入 11 位手机号"
          />
        </el-form-item>
      </div>

      <el-alert
        v-if="submitError"
        class="submit-error"
        :title="submitError"
        type="error"
        show-icon
        :closable="false"
      />
    </el-form>

    <template #footer>
      <div class="dialog-actions">
        <el-button :disabled="submitting" @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建账号' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-intro {
  padding: 12px 14px;
  margin: -2px 0 22px;
  font-size: 13px;
  line-height: 1.7;
  color: #647080;
  background: #f6f8fa;
  border-left: 3px solid #e99000;
}

.staff-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.staff-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-size: 13px;
  font-weight: 650;
  color: #3f4855;
}

.staff-form :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.submit-error {
  margin-top: -2px;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.dialog-actions :deep(.el-button) {
  min-width: 92px;
}

:global(.staff-form-dialog.el-dialog) {
  max-width: calc(100vw - 32px);
  border-radius: 10px;
}

:global(.staff-form-dialog .el-dialog__header) {
  padding: 22px 24px 16px;
  margin: 0;
  border-bottom: 1px solid #edf0f3;
}

:global(.staff-form-dialog .el-dialog__title) {
  font-size: 18px;
  font-weight: 700;
  color: #252d38;
}

:global(.staff-form-dialog .el-dialog__body) {
  padding: 22px 24px 6px;
}

:global(.staff-form-dialog .el-dialog__footer) {
  padding: 16px 24px 20px;
  border-top: 1px solid #edf0f3;
}

:global(.staff-form-dialog .el-button--primary) {
  --el-button-bg-color: #e99000;
  --el-button-border-color: #e99000;
  --el-button-hover-bg-color: #f0a11a;
  --el-button-hover-border-color: #f0a11a;
  --el-button-active-bg-color: #d67f00;
  --el-button-active-border-color: #d67f00;
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  :global(.staff-form-dialog .el-dialog__body) {
    padding-right: 18px;
    padding-left: 18px;
  }
}
</style>
