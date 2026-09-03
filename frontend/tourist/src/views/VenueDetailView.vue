<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, Location, Star } from '@element-plus/icons-vue'
import { createVenueComment, likeVenueComment, pageVenueComments, unlikeVenueComment } from '../api/comment'
import { getVenue, listVenueSessions } from '../api/catalog'
import VenueImage from '../components/VenueImage.vue'
import { useAuthStore } from '../stores/auth'
import type { VenueComment } from '../types/comment'
import type { CatalogVenue, SellableSession, SessionSaleState } from '../types/catalog'
import { getRequestErrorMessage } from '../utils/request'

interface DateOption { value: string; label: string; date: string }

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const venue = ref<CatalogVenue | null>(null)
const sessions = ref<SellableSession[]>([])
const comments = ref<VenueComment[]>([])
const loading = ref(false)
const sessionLoading = ref(false)
const commentLoading = ref(false)
const commentSubmitting = ref(false)
const commentActionId = ref<number | null>(null)
const errorMessage = ref('')
const sessionError = ref('')
const commentError = ref('')
const commentContent = ref('')
const commentTotal = ref(0)
const commentPage = ref(1)
const selectedSessionId = ref<number | null>(Number(route.query.sessionId) || null)

const venueId = computed(() => Number(route.params.venueId))
const commentPageSize = 20
const dateOptions = createDateOptions()
const selectedDate = ref(
  typeof route.query.visitDate === 'string' && dateOptions.some((item) => item.value === route.query.visitDate)
    ? route.query.visitDate
    : dateOptions[0]!.value,
)

function createDateOptions(): DateOption[] {
  const formatter = new Intl.DateTimeFormat('zh-CN', { weekday: 'short' })
  const pad = (value: number) => String(value).padStart(2, '0')
  return Array.from({ length: 7 }, (_, index) => {
    const current = new Date()
    current.setHours(12, 0, 0, 0)
    current.setDate(current.getDate() + index)
    return {
      value: `${current.getFullYear()}-${pad(current.getMonth() + 1)}-${pad(current.getDate())}`,
      label: index === 0 ? '今天' : index === 1 ? '明天' : formatter.format(current),
      date: `${pad(current.getMonth() + 1)}-${pad(current.getDate())}`,
    }
  })
}

const saleStateText: Record<SessionSaleState, string> = {
  NOT_STARTED: '未开售',
  ON_SALE: '预约中',
  SOLD_OUT: '已售罄',
}

async function loadVenue(): Promise<void> {
  if (!Number.isSafeInteger(venueId.value) || venueId.value <= 0) {
    errorMessage.value = '景点参数无效'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    venue.value = await getVenue(venueId.value)
  } catch (error) {
    errorMessage.value = getRequestErrorMessage(error, '景点信息加载失败')
  } finally {
    loading.value = false
  }
}

async function loadSessions(): Promise<void> {
  if (!Number.isSafeInteger(venueId.value) || venueId.value <= 0) return
  sessionLoading.value = true
  sessionError.value = ''
  try {
    sessions.value = await listVenueSessions(venueId.value, selectedDate.value)
  } catch (error) {
    sessions.value = []
    sessionError.value = getRequestErrorMessage(error, '场次加载失败')
  } finally {
    sessionLoading.value = false
  }
}

async function loadComments(): Promise<void> {
  if (!Number.isSafeInteger(venueId.value) || venueId.value <= 0) return
  commentLoading.value = true
  commentError.value = ''
  try {
    const result = await pageVenueComments(venueId.value, commentPage.value, commentPageSize)
    comments.value = result.items
    commentTotal.value = result.total
  } catch (error) {
    comments.value = []
    commentTotal.value = 0
    commentError.value = getRequestErrorMessage(error, '评论加载失败')
  } finally {
    commentLoading.value = false
  }
}

async function requireCommentLogin(): Promise<boolean> {
  if (authStore.hasValidSession()) return true
  ElMessage.warning('请先登录后再操作评论')
  await router.push({ name: 'home', query: { login: '1', redirect: route.fullPath } })
  return false
}

async function submitComment(): Promise<void> {
  const content = commentContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  if (!await requireCommentLogin() || commentSubmitting.value) return

  commentSubmitting.value = true
  try {
    await createVenueComment(venueId.value, { content })
    commentContent.value = ''
    commentPage.value = 1
    await loadComments()
    ElMessage.success('评论发表成功')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '评论发表失败'))
  } finally {
    commentSubmitting.value = false
  }
}

async function changeCommentLike(commentId: number, shouldLike: boolean): Promise<void> {
  if (!await requireCommentLogin() || commentActionId.value !== null) return

  commentActionId.value = commentId
  try {
    if (shouldLike) {
      await likeVenueComment(commentId)
      ElMessage.success('点赞成功')
    } else {
      await unlikeVenueComment(commentId)
      ElMessage.success('已取消点赞')
    }
    await loadComments()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, shouldLike ? '点赞失败' : '取消点赞失败'))
  } finally {
    commentActionId.value = null
  }
}

async function changeCommentPage(nextPage: number): Promise<void> {
  if (nextPage === commentPage.value) return
  commentPage.value = nextPage
  await loadComments()
}

async function selectDate(date: string): Promise<void> {
  selectedDate.value = date
  selectedSessionId.value = null
  await router.replace({ query: { visitDate: date } })
}

async function selectSession(session: SellableSession): Promise<void> {
  if (session.saleState !== 'ON_SALE') return
  selectedSessionId.value = session.id
  await router.push({
    name: 'booking',
    query: {
      venueId: String(venueId.value),
      visitDate: selectedDate.value,
      sessionId: String(session.id),
    },
  })
}

function formatTime(value: string): string {
  return value.slice(0, 5)
}

function formatPrice(value: number): string {
  return `¥${Number(value).toFixed(2)}`
}

function formatCommentTime(value: string): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.replace('T', ' ')
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

watch(selectedDate, loadSessions)
onMounted(async () => {
  await Promise.all([loadVenue(), loadSessions(), loadComments()])
})
</script>

<template>
  <div class="venue-detail" v-loading="loading">
    <RouterLink class="back-link" to="/"><el-icon><ArrowLeft /></el-icon>返回景点列表</RouterLink>
    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />

    <template v-if="venue">
      <section class="venue-overview">
        <VenueImage :src="venue.coverUrl" :alt="`${venue.name}封面`" />
        <div>
          <h1>{{ venue.name }}</h1>
          <p class="address"><el-icon><Location /></el-icon>{{ venue.address }}</p>
          <p class="description">{{ venue.description }}</p>
          <RouterLink class="coupon-link" :to="{ name: 'coupons', query: { venueId: venue.id } }">查看景点优惠券</RouterLink>
        </div>
      </section>

      <section class="session-section">
        <h2>选择参观日期</h2>
        <div class="date-list">
          <button
            v-for="option in dateOptions"
            :key="option.value"
            type="button"
            :class="{ active: selectedDate === option.value }"
            @click="selectDate(option.value)"
          >
            <span>{{ option.label }}</span><b>{{ option.date }}</b>
          </button>
        </div>

        <div class="session-heading">
          <h2>可预约场次</h2><span>所有时间均为当地时间</span>
        </div>
        <el-alert v-if="sessionError" :title="sessionError" type="error" show-icon :closable="false" />
        <div v-loading="sessionLoading" class="session-list">
          <article v-for="session in sessions" :key="session.id" :class="['session-row', session.saleState.toLowerCase()]">
            <div class="session-time">
              <strong>{{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</strong>
              <span>{{ saleStateText[session.saleState] }}</span>
              <small>{{ session.remainingCapacity == null ? '名额将在开售后显示' : `剩余 ${session.remainingCapacity} 个名额` }}</small>
            </div>

            <div class="ticket-list">
              <div v-for="ticketType in session.ticketTypes" :key="ticketType.sessionTicketTypeId" class="ticket-type">
                <div><strong>{{ ticketType.ticketTypeName }}</strong><b>{{ formatPrice(ticketType.salePrice) }}</b></div>
                <p>{{ ticketType.audienceRule || ticketType.description || '适用规则以现场说明为准' }}</p>
                <span>{{ ticketType.remainingQuantity == null ? '开售后显示余票' : `剩余 ${ticketType.remainingQuantity}` }}</span>
              </div>
            </div>

            <el-button
              type="primary"
              :disabled="session.saleState !== 'ON_SALE'"
              :plain="selectedSessionId === session.id"
              @click="selectSession(session)"
            >
              {{ selectedSessionId === session.id ? '立即预约' : '选择此场次' }}
            </el-button>
          </article>
        </div>
        <el-empty v-if="!sessionLoading && !sessionError && sessions.length === 0" description="所选日期暂无可预约场次" />
      </section>

      <section class="comment-section" aria-labelledby="comment-title">
        <div class="comment-heading">
          <div>
            <h2 id="comment-title"><el-icon><ChatDotRound /></el-icon>游客评价</h2>
            <p>共 {{ commentTotal }} 条评论，按点赞数排序</p>
          </div>
        </div>

        <form class="comment-form" @submit.prevent="submitComment">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="none"
            placeholder="分享你的游览体验，登录后即可发表"
            aria-label="评论内容"
          />
          <div class="comment-form-footer">
            <span>评论最多 500 个字符</span>
            <el-button type="primary" native-type="submit" :loading="commentSubmitting">
              发布评论
            </el-button>
          </div>
        </form>

        <el-alert v-if="commentError" :title="commentError" type="error" show-icon :closable="false" />
        <div v-loading="commentLoading" class="comment-list">
          <article v-for="comment in comments" :key="comment.id" class="comment-item">
            <header>
              <strong>{{ comment.authorName }}</strong>
              <time :datetime="comment.createdAt">{{ formatCommentTime(comment.createdAt) }}</time>
            </header>
            <p>{{ comment.content }}</p>
            <footer>
              <span><el-icon><Star /></el-icon>{{ comment.likeCount }} 人点赞</span>
              <div class="comment-actions">
                <el-button
                  type="primary"
                  link
                  :loading="commentActionId === comment.id"
                  @click="changeCommentLike(comment.id, true)"
                >
                  点赞
                </el-button>
                <el-button
                  link
                  :disabled="commentActionId !== null"
                  @click="changeCommentLike(comment.id, false)"
                >
                  取消点赞
                </el-button>
              </div>
            </footer>
          </article>
          <el-empty v-if="!commentLoading && !commentError && comments.length === 0" description="暂时还没有评论，来抢沙发吧" />
        </div>

        <el-pagination
          v-if="commentTotal > commentPageSize"
          class="comment-pagination"
          background
          small
          :current-page="commentPage"
          :page-size="commentPageSize"
          :total="commentTotal"
          layout="prev, pager, next"
          @current-change="changeCommentPage"
        />
      </section>
    </template>
  </div>
</template>

<style scoped>
.venue-detail { color: #1d2923; }
.back-link { display: inline-flex; gap: 7px; align-items: center; margin: 2px 0 18px; color: #435049; text-decoration: none; }
.venue-overview { display: grid; grid-template-columns: minmax(420px,1fr) minmax(360px,1fr); gap: 38px; padding-bottom: 28px; border-bottom: 1px solid #dfe4e1; }
.venue-overview > img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; border-radius: 12px; }
.venue-overview > div { align-self: center; }
.venue-overview h1 { margin: 0 0 18px; font-size: clamp(30px,3.2vw,42px); line-height: 1.25; }
.address { display: flex; gap: 8px; align-items: flex-start; margin: 0 0 22px; color: #69746e; }
.address .el-icon { flex: none; margin-top: 3px; }
.description { margin: 0; color: #56625c; line-height: 1.9; white-space: pre-line; }
.coupon-link { display: inline-flex; margin-top: 20px; padding: 9px 14px; color: #d86e00; font-weight: 650; text-decoration: none; background: #fff8ed; border: 1px solid #efb66f; border-radius: 8px; }
.session-section { padding-top: 26px; }
.session-section h2 { margin: 0 0 15px; font-size: 21px; }
.date-list { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 6px; }
.date-list button { display: grid; flex: 0 0 104px; gap: 5px; padding: 12px; color: #455049; cursor: pointer; background: #fff; border: 1px solid #dce1de; border-radius: 8px; }
.date-list button.active { color: #d86e00; background: #fff8ed; border-color: #ef8610; }
.date-list b { font-size: 14px; font-weight: 500; }
.session-heading { display: flex; align-items: end; justify-content: space-between; margin: 24px 0 12px; }
.session-heading h2 { margin: 0; }
.session-heading span { color: #808984; font-size: 13px; }
.session-list { min-height: 130px; overflow: hidden; background: #fff; border: 1px solid #dfe4e1; border-radius: 11px; }
.session-row { display: grid; grid-template-columns: 250px 1fr auto; gap: 20px; align-items: center; padding: 20px; border-bottom: 1px solid #e7eae8; }
.session-row:last-child { border-bottom: 0; }
.session-time { display: grid; grid-template-columns: auto auto; gap: 8px 12px; align-items: center; }
.session-time strong { font-size: 19px; }
.session-time span { width: max-content; padding: 4px 8px; color: #17734f; font-size: 12px; background: #eaf7f0; border-radius: 5px; }
.session-time small { grid-column: 1 / -1; color: #76807b; }
.not_started, .sold_out { background: #f7f8f7; }
.not_started .session-time span, .sold_out .session-time span { color: #65706a; background: #eef0ef; }
.ticket-list { display: grid; grid-template-columns: repeat(2,minmax(180px,1fr)); gap: 14px; }
.ticket-type { min-width: 0; padding-left: 15px; border-left: 1px solid #e4e8e5; }
.ticket-type div { display: flex; justify-content: space-between; gap: 10px; }
.ticket-type b { color: #e66f00; }
.ticket-type p { margin: 8px 0 4px; overflow: hidden; color: #68736d; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.ticket-type span { color: #858d89; font-size: 12px; }
.session-row > .el-button { min-height: 40px; --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
.comment-section { padding-top: 38px; }
.comment-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 14px; }
.comment-heading h2 { display: flex; gap: 8px; align-items: center; margin: 0; font-size: 21px; }
.comment-heading h2 .el-icon { color: #e66f00; }
.comment-heading p { margin: 7px 0 0; color: #7a837f; font-size: 13px; }
.comment-form { padding: 16px; background: #fff; border: 1px solid #dfe4e1; border-radius: 11px; }
.comment-form :deep(.el-textarea__inner) { min-height: 94px !important; padding: 12px 14px; line-height: 1.65; box-shadow: none; }
.comment-form-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: 10px; color: #808984; font-size: 12px; }
.comment-form-footer .el-button { min-width: 104px; --el-button-bg-color: #ef8610; --el-button-border-color: #ef8610; }
.comment-section > .el-alert { margin-top: 14px; }
.comment-list { min-height: 120px; margin-top: 14px; overflow: hidden; background: #fff; border: 1px solid #dfe4e1; border-radius: 11px; }
.comment-item { padding: 18px 20px; border-bottom: 1px solid #edf0ee; }
.comment-item:last-child { border-bottom: 0; }
.comment-item header, .comment-item footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.comment-item header strong { color: #2b3932; font-size: 14px; }
.comment-item time { color: #87908b; font-size: 12px; }
.comment-item p { margin: 10px 0 14px; color: #445049; line-height: 1.75; white-space: pre-wrap; word-break: break-word; }
.comment-item footer { color: #8a938e; font-size: 12px; }
.comment-item footer > span { display: inline-flex; gap: 5px; align-items: center; }
.comment-item footer > span .el-icon { color: #e66f00; }
.comment-actions { display: flex; align-items: center; gap: 4px; }
.comment-actions .el-button { margin-left: 0; }
.comment-pagination { justify-content: center; margin-top: 18px; }
@media (max-width: 900px) { .venue-overview { grid-template-columns: 1fr; gap: 22px; } .session-row { grid-template-columns: 1fr; } .session-row > .el-button { width: 100%; } }
@media (max-width: 600px) { .venue-overview { gap: 18px; } .venue-overview h1 { font-size: 29px; } .ticket-list { grid-template-columns: 1fr; } .session-row { padding: 17px; } .session-heading { align-items: flex-start; flex-direction: column; gap: 6px; } .comment-section { padding-top: 30px; } .comment-item { padding: 16px; } .comment-item header, .comment-item footer { align-items: flex-start; flex-direction: column; gap: 8px; } .comment-actions { width: 100%; } }
</style>
