<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Hide, Lock, User, View } from '@element-plus/icons-vue'
import loginIllustration from '../assets/operator-login-illustration.png'
import { useAuthStore } from '../stores/auth'
import type { LoginCredentials } from '../types/auth'
import { getRequestErrorMessage } from '../utils/request'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const loginError = ref('')
const passwordVisible = ref(false)

const form = reactive<LoginCredentials>({
  loginName: '',
  password: '',
})

const rules: FormRules<LoginCredentials> = {
  loginName: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 50, message: '登录账号不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { max: 50, message: '登录密码不能超过 50 个字符', trigger: 'blur' },
  ],
}

function getSafeRedirect(): string {
  const redirect = route.query.redirect
  if (
    typeof redirect === 'string'
    && redirect.startsWith('/')
    && !redirect.startsWith('//')
    && !(redirect === '/' && authStore.identity?.roleCode === 'STAFF')
  ) {
    return redirect
  }
  return authStore.identity?.roleCode === 'STAFF' ? '/verification' : '/'
}

async function handleLogin(): Promise<void> {
  if (!formRef.value || submitting.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loginError.value = ''
  submitting.value = true
  try {
    await authStore.login({
      loginName: form.loginName.trim(),
      password: form.password,
    })
    ElMessage.success('登录成功')
    await router.replace(getSafeRedirect())
  } catch (error) {
    loginError.value = getRequestErrorMessage(error, '登录失败，请检查账号和密码')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-screen">
    <section class="brand-panel" aria-label="文旅票务运营端介绍">
      <div class="brand-lockup">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 44 38" role="presentation">
            <path d="M4 10 22 2l18 8H4Z" />
            <path d="M7 13h30v4H7zM9 29h26v4H9zM5 34h34v3H5z" />
            <path d="M11 17h4v12h-4zM20 17h4v12h-4zM29 17h4v12h-4z" />
          </svg>
        </span>
        <span>文旅票务运营端</span>
      </div>

      <div class="brand-message">
        <h1>让每一次入园，都<span>有序</span>发生。</h1>
        <p>统一管理场次、票务、订单与核销工作。</p>
      </div>

      <img
        class="venue-illustration"
        :src="loginIllustration"
        alt="游客有序排队并通过手机票码核销入园的线稿插图"
      >
    </section>

    <section class="form-panel" aria-labelledby="login-title">
      <div class="login-form-wrap">
        <header class="form-heading">
          <h2 id="login-title">登录运营工作台</h2>
          <p>请输入运营者或工作人员账号与密码</p>
        </header>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          hide-required-asterisk
          label-position="top"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="登录账号" prop="loginName">
            <el-input
              v-model="form.loginName"
              :prefix-icon="User"
              autocomplete="username"
              maxlength="50"
              placeholder="请输入登录账号"
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              autocomplete="current-password"
              maxlength="50"
              placeholder="请输入登录密码"
              size="large"
              :type="passwordVisible ? 'text' : 'password'"
              @keyup.enter="handleLogin"
            >
              <template #suffix>
                <button
                  class="password-toggle"
                  type="button"
                  :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                  @click="passwordVisible = !passwordVisible"
                >
                  <el-icon>
                    <Hide v-if="passwordVisible" />
                    <View v-else />
                  </el-icon>
                </button>
              </template>
            </el-input>
          </el-form-item>

          <el-alert
            v-if="loginError"
            class="login-error"
            :title="loginError"
            type="error"
            show-icon
            :closable="false"
          />

          <el-button
            class="login-button"
            type="primary"
            native-type="submit"
            :loading="submitting"
          >
            登录
          </el-button>
        </el-form>

        <p class="access-note">仅限景点运营者与工作人员使用</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-screen {
  --brand: #e99000;
  --brand-hover: #f0a11a;
  --brand-active: #d67f00;
  --slate: #253341;
  --slate-deep: #202d39;
  --text: #202733;
  --muted: #75808e;
  --border: #d7dce2;

  display: grid;
  grid-template-columns: minmax(520px, 1fr) minmax(520px, 1fr);
  min-height: 100vh;
  color: var(--text);
  background: #fff;
}

.brand-panel {
  position: relative;
  min-height: 100vh;
  padding: 44px clamp(44px, 5.7vw, 90px);
  overflow: hidden;
  color: #fff;
  background: var(--slate);
}

.brand-panel::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: '';
  box-shadow: inset -1px 0 rgb(255 255 255 / 8%);
}

.brand-lockup {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 14px;
  align-items: center;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.brand-mark {
  position: relative;
  display: grid;
  width: 43px;
  height: 38px;
  place-items: center;
  color: #fff;
}

.brand-mark::after {
  position: absolute;
  right: 2px;
  bottom: 0;
  left: 2px;
  height: 3px;
  content: '';
  background: var(--brand);
  border-radius: 2px;
}

.brand-mark svg {
  width: 40px;
  height: 36px;
  fill: currentcolor;
}

.brand-message {
  position: relative;
  z-index: 1;
  max-width: 700px;
  margin-top: clamp(150px, 19vh, 215px);
}

.brand-message h1 {
  margin: 0;
  font-size: clamp(36px, 2.75vw, 44px);
  line-height: 1.24;
  letter-spacing: -0.045em;
  white-space: nowrap;
}

.brand-message h1 span {
  color: var(--brand);
}

.brand-message p {
  margin: 24px 0 0;
  font-size: clamp(17px, 1.45vw, 22px);
  line-height: 1.7;
  color: #d5dde4;
}

.venue-illustration {
  position: absolute;
  right: 0;
  bottom: 12%;
  left: 0;
  width: 100%;
  height: 48%;
  object-fit: cover;
  object-position: center bottom;
  opacity: 0.68;
  mask-image: linear-gradient(
    to bottom,
    transparent 0%,
    #000 12%,
    #000 88%,
    transparent 100%
  );
}

.form-panel {
  display: grid;
  min-height: 100vh;
  padding: 48px clamp(56px, 7.5vw, 120px);
  place-items: center;
  background: #fff;
}

.login-form-wrap {
  width: min(100%, 540px);
  margin-top: -14px;
}

.form-heading {
  margin-bottom: 48px;
}

.form-heading h2 {
  margin: 0;
  font-size: clamp(32px, 2.6vw, 42px);
  line-height: 1.25;
  letter-spacing: -0.025em;
}

.form-heading p {
  margin: 18px 0 0;
  font-size: 16px;
  color: var(--muted);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 30px;
}

.login-form :deep(.el-form-item__label) {
  height: auto;
  padding: 0 0 10px;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.4;
  color: var(--text);
}

.login-form :deep(.el-input__wrapper) {
  min-height: 58px;
  padding: 0 18px;
  border-radius: 6px;
  box-shadow: 0 0 0 1px var(--border) inset;
  transition: box-shadow 160ms ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b8c0c9 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--brand) inset;
}

.login-form :deep(.el-input__inner) {
  font-size: 16px;
}

.login-form :deep(.el-input__prefix),
.login-form :deep(.el-input__suffix) {
  font-size: 19px;
  color: #87919d;
}

.password-toggle {
  display: grid;
  padding: 4px;
  color: #7d8792;
  cursor: pointer;
  background: transparent;
  border: 0;
  place-items: center;
}

.password-toggle:hover {
  color: var(--text);
}

.password-toggle .el-icon {
  font-size: 19px;
}

.login-button {
  width: 100%;
  min-height: 60px;
  margin-top: 6px;
  font-size: 18px;
  font-weight: 650;
  border-radius: 6px;
}

.login-error {
  margin: -8px 0 24px;
}

.login-button.el-button--primary {
  --el-button-bg-color: var(--brand);
  --el-button-border-color: var(--brand);
  --el-button-hover-bg-color: var(--brand-hover);
  --el-button-hover-border-color: var(--brand-hover);
  --el-button-active-bg-color: var(--brand-active);
  --el-button-active-border-color: var(--brand-active);
}

.access-note {
  margin: 36px 0 0;
  font-size: 14px;
  color: #697481;
  text-align: center;
}

@media (max-width: 900px) {
  .login-screen {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 218px;
    padding: 28px 30px;
  }

  .brand-lockup {
    font-size: 19px;
  }

  .brand-mark {
    width: 36px;
    height: 33px;
  }

  .brand-mark svg {
    width: 34px;
    height: 31px;
  }

  .brand-message {
    margin-top: 44px;
  }

  .brand-message h1 {
    font-size: clamp(28px, 7.5vw, 36px);
    white-space: normal;
  }

  .brand-message p {
    margin-top: 12px;
    font-size: 15px;
  }

  .venue-illustration {
    right: -30px;
    bottom: -35px;
    left: auto;
    width: 310px;
    height: auto;
    opacity: 0.16;
  }

  .form-panel {
    min-height: calc(100vh - 218px);
    padding: 48px 24px 56px;
    place-items: start center;
  }

  .login-form-wrap {
    width: min(100%, 520px);
    margin-top: 0;
  }

  .form-heading {
    margin-bottom: 34px;
  }

  .form-heading h2 {
    font-size: 30px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-form :deep(.el-input__wrapper) {
    transition: none;
  }
}
</style>
