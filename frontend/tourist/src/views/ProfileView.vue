<script setup lang="ts">
import { ArrowRight, Discount, Document, SwitchButton, Tickets, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const quickEntries = [
  { title: '我的订单', description: '查看预约和支付状态', path: '/orders', icon: Document },
  { title: '我的票券', description: '查看入馆票码', path: '/tickets', icon: Tickets },
  { title: '优惠券中心', description: '领取和管理优惠券', path: '/coupons', icon: Discount },
  { title: '常用参观人', description: '维护同行人员信息', path: '/visitors', icon: User },
]

async function logout(): Promise<void> {
  authStore.logout()
  ElMessage.success('已退出登录')
  await router.replace('/')
}
</script>

<template>
  <div class="profile-page">
    <h1>个人中心</h1>
    <div class="profile-layout">
      <aside class="account-panel">
        <div class="account-heading">
          <span class="avatar">{{ authStore.identity?.loginName.slice(0, 1).toUpperCase() }}</span>
          <div><strong>{{ authStore.identity?.loginName }}</strong><span>游客账号</span></div>
        </div>
        <dl>
          <div><dt>登录账号</dt><dd>{{ authStore.identity?.loginName }}</dd></div>
          <div><dt>用户编号</dt><dd>{{ authStore.identity?.userId }}</dd></div>
          <div><dt>账号角色</dt><dd>游客</dd></div>
        </dl>
        <el-button :icon="SwitchButton" @click="logout">退出登录</el-button>
      </aside>

      <section class="quick-panel">
        <button v-for="entry in quickEntries" :key="entry.path" type="button" @click="router.push(entry.path)">
          <el-icon class="entry-icon"><component :is="entry.icon" /></el-icon>
          <span><strong>{{ entry.title }}</strong><small>{{ entry.description }}</small></span>
          <el-icon class="arrow"><ArrowRight /></el-icon>
        </button>
        <p>当前版本暂不支持修改账号资料</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.profile-page { color: #1d2923; }
h1 { margin: 0 0 20px; font-size: 30px; }
.profile-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 26px; align-items: start; }
.account-panel { padding: 30px; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.account-heading { display: flex; gap: 20px; align-items: center; padding-bottom: 26px; border-bottom: 1px solid #e4e8e5; }
.avatar { display: grid; flex: none; width: 86px; height: 86px; color: #fff; font-size: 38px; font-weight: 700; background: #ef7810; border-radius: 50%; place-items: center; }
.account-heading div { display: grid; gap: 8px; }
.account-heading strong { font-size: 23px; overflow-wrap: anywhere; }
.account-heading span { color: #737d78; }
.account-panel dl { margin: 0 0 26px; }
.account-panel dl div { display: flex; justify-content: space-between; gap: 14px; padding: 19px 0; border-bottom: 1px solid #e4e8e5; }
.account-panel dt { color: #69736e; }.account-panel dd { margin: 0; text-align: right; overflow-wrap: anywhere; }
.account-panel .el-button { width: 100%; min-height: 42px; }
.quick-panel { overflow: hidden; background: #fff; border: 1px solid #dfe4e1; border-radius: 10px; }
.quick-panel button { display: grid; grid-template-columns: 58px 1fr 24px; gap: 18px; align-items: center; width: 100%; min-height: 128px; padding: 24px 38px; color: #27332d; text-align: left; cursor: pointer; background: #fff; border: 0; border-bottom: 1px solid #e4e8e5; }
.quick-panel button:hover { background: #fffaf4; }
.entry-icon { color: #515a55; font-size: 43px; }
.quick-panel button > span { display: grid; gap: 8px; }
.quick-panel strong { font-size: 21px; }.quick-panel small { color: #737d78; font-size: 14px; }
.arrow { justify-self: end; color: #66716b; font-size: 23px; }
.quick-panel > p { margin: 0; padding: 18px; color: #858d89; text-align: center; }
@media (max-width: 800px) { .profile-layout { grid-template-columns: 1fr; } .account-panel { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; } .account-panel dl { margin: 0; } .account-panel .el-button { grid-column: 1 / -1; } }
@media (max-width: 560px) { h1 { font-size: 26px; } .account-panel { display: block; padding: 22px; } .account-panel dl { margin-bottom: 22px; } .avatar { width: 72px; height: 72px; font-size: 31px; } .quick-panel button { grid-template-columns: 44px 1fr 20px; gap: 13px; min-height: 100px; padding: 18px 20px; } .entry-icon { font-size: 34px; } .quick-panel strong { font-size: 18px; } }
</style>
