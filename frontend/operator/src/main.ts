import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
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
  const redirect = router.currentRoute.value.fullPath
  authStore.logout()

  if (router.currentRoute.value.name !== 'login') {
    void router.replace({
      name: 'login',
      query: { redirect },
    })
  }
})

app.mount('#app')
