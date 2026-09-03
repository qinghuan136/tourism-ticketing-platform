<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, Phone, User } from '@element-plus/icons-vue'
import { registerTourist } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import type { LoginCredentials, RegisterForm } from '../types/auth'
import { getRequestErrorMessage } from '../utils/request'

type AuthMode = 'login' | 'register'

const props = defineProps<{
  visible: boolean
  mode: AuthMode
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  authenticated: []
}>()

const authStore = useAuthStore()
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const currentMode = ref<AuthMode>(props.mode)
const submitting = ref(false)
const formError = ref('')

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value),
})

const loginForm = reactive<LoginCredentials>({ loginName: '', password: '' })
const registerForm = reactive<RegisterForm>({
  loginName: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  phone: '',
})

const loginRules: FormRules<LoginCredentials> = {
  loginName: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 50, message: '登录账号不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { max: 50, message: '登录密码不能超过 50 个字符', trigger: 'blur' },
  ],
}

const registerRules: FormRules<RegisterForm> = {
  loginName: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 4, max: 50, message: '登录账号长度应为 4 到 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度应为 6 到 32 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入登录密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        value === registerForm.password ? callback() : callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur',
    },
  ],
  displayName: [
    { required: true, message: '请输入游客昵称', trigger: 'blur' },
    { max: 50, message: '昵称不能超过 50 个字符', trigger: 'blur' },
  ],
  phone: [
    { pattern: /^$|^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
}

watch(
  () => props.mode,
  (mode) => {
    if (props.visible) switchMode(mode)
  },
)

watch(
  () => props.visible,
  (visible) => {
    if (visible) switchMode(props.mode)
  },
)

function switchMode(mode: AuthMode): void {
  currentMode.value = mode
  formError.value = ''
  nextTick(() => {
    loginFormRef.value?.clearValidate()
    registerFormRef.value?.clearValidate()
  })
}

async function handleLogin(): Promise<void> {
  if (!loginFormRef.value || submitting.value) return
  if (!await loginFormRef.value.validate().catch(() => false)) return

  formError.value = ''
  submitting.value = true
  try {
    await authStore.login({
      loginName: loginForm.loginName.trim(),
      password: loginForm.password,
    })
    ElMessage.success('登录成功')
    emit('authenticated')
    dialogVisible.value = false
  } catch (error) {
    formError.value = getRequestErrorMessage(error, '登录失败，请检查账号和密码')
  } finally {
    submitting.value = false
  }
}

async function handleRegister(): Promise<void> {
  if (!registerFormRef.value || submitting.value) return
  if (!await registerFormRef.value.validate().catch(() => false)) return

  formError.value = ''
  submitting.value = true
  try {
    await registerTourist({
      loginName: registerForm.loginName.trim(),
      password: registerForm.password,
      displayName: registerForm.displayName.trim(),
      phone: registerForm.phone.trim(),
    })
    loginForm.loginName = registerForm.loginName.trim()
    loginForm.password = ''
    switchMode('login')
    ElMessage.success('注册成功，请登录')
  } catch (error) {
    formError.value = getRequestErrorMessage(error, '注册失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    class="auth-dialog"
    :width="currentMode === 'login' ? 430 : 500"
    :title="currentMode === 'login' ? '游客登录' : '注册游客账号'"
    append-to-body
    destroy-on-close
  >
    <el-form
      v-if="currentMode === 'login'"
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginRules"
      label-position="top"
      hide-required-asterisk
      @submit.prevent="handleLogin"
    >
      <el-form-item label="登录账号" prop="loginName">
        <el-input
          v-model="loginForm.loginName"
          :prefix-icon="User"
          autocomplete="username"
          maxlength="50"
          placeholder="请输入登录账号"
          size="large"
        />
      </el-form-item>
      <el-form-item label="登录密码" prop="password">
        <el-input
          v-model="loginForm.password"
          :prefix-icon="Lock"
          autocomplete="current-password"
          maxlength="50"
          placeholder="请输入登录密码"
          show-password
          size="large"
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <el-alert v-if="formError" :title="formError" type="error" show-icon :closable="false" />
      <el-button class="submit-button" type="primary" native-type="submit" :loading="submitting">
        登录
      </el-button>
      <p class="mode-switch">还没有账号？<button type="button" @click="switchMode('register')">立即注册</button></p>
    </el-form>

    <el-form
      v-else
      ref="registerFormRef"
      :model="registerForm"
      :rules="registerRules"
      label-position="top"
      hide-required-asterisk
      @submit.prevent="handleRegister"
    >
      <div class="register-grid">
        <el-form-item label="登录账号" prop="loginName">
          <el-input v-model="registerForm.loginName" :prefix-icon="User" maxlength="50" placeholder="4 到 50 个字符" size="large" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input v-model="registerForm.password" :prefix-icon="Lock" maxlength="32" placeholder="6 到 32 个字符" show-password size="large" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" :prefix-icon="Lock" maxlength="32" placeholder="请再次输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item label="游客昵称" prop="displayName">
          <el-input v-model="registerForm.displayName" :prefix-icon="User" maxlength="50" placeholder="请输入游客昵称" size="large" />
        </el-form-item>
        <el-form-item class="phone-field" label="手机号（选填）" prop="phone">
          <el-input v-model="registerForm.phone" :prefix-icon="Phone" maxlength="11" placeholder="请输入手机号" size="large" />
        </el-form-item>
      </div>

      <el-alert v-if="formError" :title="formError" type="error" show-icon :closable="false" />
      <el-button class="submit-button" type="primary" native-type="submit" :loading="submitting">
        注册
      </el-button>
      <p class="mode-switch">已有账号？<button type="button" @click="switchMode('login')">返回登录</button></p>
    </el-form>
  </el-dialog>
</template>

<style>
.auth-dialog {
  --el-color-primary: #ed8c16;
  max-width: calc(100vw - 28px);
  border-radius: 14px;
}

.auth-dialog .el-dialog__header {
  padding: 24px 28px 14px;
}

.auth-dialog .el-dialog__title {
  font-size: 22px;
  font-weight: 700;
  color: #17221d;
}

.auth-dialog .el-dialog__body {
  padding: 14px 28px 24px;
}

.auth-dialog .el-form-item {
  margin-bottom: 20px;
}

.auth-dialog .el-form-item__label {
  padding-bottom: 8px;
  font-weight: 600;
  color: #303a35;
}

.auth-dialog .el-input__wrapper {
  min-height: 46px;
  border-radius: 8px;
}

.auth-dialog .register-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.auth-dialog .phone-field {
  grid-column: 1 / -1;
}

.auth-dialog .el-alert {
  margin: -2px 0 18px;
}

.auth-dialog .submit-button {
  width: 100%;
  min-height: 46px;
  margin-top: 4px;
  font-size: 16px;
  font-weight: 650;
  border-radius: 8px;
}

.auth-dialog .mode-switch {
  margin: 18px 0 0;
  color: #68736d;
  text-align: center;
}

.auth-dialog .mode-switch button {
  padding: 0;
  color: #df7900;
  cursor: pointer;
  background: none;
  border: 0;
}

@media (max-width: 600px) {
  .auth-dialog {
    width: calc(100vw - 20px) !important;
    max-height: calc(100vh - 20px);
    margin: 10px auto;
    overflow: auto;
  }

  .auth-dialog .el-dialog__header {
    padding: 22px 20px 12px;
  }

  .auth-dialog .el-dialog__body {
    padding: 12px 20px 22px;
  }

  .auth-dialog .register-grid {
    grid-template-columns: 1fr;
  }

  .auth-dialog .phone-field {
    grid-column: auto;
  }
}
</style>
