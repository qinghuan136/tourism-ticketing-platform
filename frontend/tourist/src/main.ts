import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/main.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { installAuthFailureHandler } from './utils/request'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

const authStore = useAuthStore(pinia)
installAuthFailureHandler(() => {
  authStore.logout()
  ElMessage.warning('登录已失效，请重新登录')
  router.push({ name: 'home', query: { login: '1', redirect: router.currentRoute.value.fullPath } })
})

app.mount('#app')
