<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Aim, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listNearbyByCoordinate, listNearbyByName, pageVenues } from '../api/catalog'
import VenueImage from '../components/VenueImage.vue'
import type { CatalogVenue, NearbyVenue } from '../types/catalog'
import { getRequestErrorMessage } from '../utils/request'

const route = useRoute()
const router = useRouter()
const venues = ref<CatalogVenue[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')
const nearbyMode = ref<'location' | 'name'>('name')
const nearbyVenues = ref<NearbyVenue[]>([])
const nearbyLoading = ref(false)
const nearbyError = ref('')
const nearbySearched = ref(false)
const nearbyForm = reactive({ name: '', city: '' })

const keyword = computed(() => typeof route.query.keyword === 'string' ? route.query.keyword : '')
const page = computed(() => Math.max(1, Number(route.query.page) || 1))
const size = computed(() => Math.min(100, Math.max(1, Number(route.query.size) || 9)))

function formatPrice(price: number | null): string {
  return price == null ? '暂不可预约' : `¥${Number(price).toFixed(0)} 起`
}

async function loadVenues(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    const result = await pageVenues({
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: size.value,
    })
    venues.value = result.items
    total.value = result.total
  } catch (error) {
    venues.value = []
    total.value = 0
    loadError.value = getRequestErrorMessage(error, '景点加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function changePage(nextPage: number): Promise<void> {
  await router.push({ name: 'home', query: { ...route.query, page: nextPage === 1 ? undefined : String(nextPage) } })
}

async function changeSize(nextSize: number): Promise<void> {
  await router.push({ name: 'home', query: { ...route.query, page: undefined, size: String(nextSize) } })
}

async function searchNearbyByName(): Promise<void> {
  const name = nearbyForm.name.trim()
  if (!name) {
    ElMessage.warning('请输入地点名称')
    return
  }

  nearbyLoading.value = true
  nearbyError.value = ''
  nearbySearched.value = true
  try {
    nearbyVenues.value = await listNearbyByName({
      name,
      city: nearbyForm.city.trim() || undefined,
      radiusKm: 5,
      limit: 20,
    })
  } catch (error) {
    nearbyVenues.value = []
    nearbyError.value = getRequestErrorMessage(error, '附近景点查询失败')
  } finally {
    nearbyLoading.value = false
  }
}

function searchNearbyByLocation(): void {
  if (!navigator.geolocation) {
    nearbyError.value = '当前浏览器不支持定位，请改用地点搜索'
    return
  }

  nearbyLoading.value = true
  nearbyError.value = ''
  nearbySearched.value = true
  navigator.geolocation.getCurrentPosition(
    async ({ coords }) => {
      try {
        nearbyVenues.value = await listNearbyByCoordinate({
          longitude: coords.longitude,
          latitude: coords.latitude,
          radiusKm: 5,
          limit: 20,
        })
      } catch (error) {
        nearbyVenues.value = []
        nearbyError.value = getRequestErrorMessage(error, '附近景点查询失败')
      } finally {
        nearbyLoading.value = false
      }
    },
    () => {
      nearbyError.value = '无法获取当前位置，请允许定位或改用地点搜索'
      nearbyLoading.value = false
    },
    { enableHighAccuracy: false, timeout: 8000 },
  )
}

watch(() => [route.query.keyword, route.query.page, route.query.size], loadVenues)
onMounted(loadVenues)
</script>

<template>
  <div class="catalog-page">
    <section class="catalog-hero">
      <div class="hero-copy">
        <h1>发现值得去的城市景点</h1>
        <p>提前预约，避开人流高峰，享受更好的游览体验。</p>
        <div v-if="keyword" class="search-summary">
          正在搜索“{{ keyword }}”
          <RouterLink to="/">清除条件</RouterLink>
        </div>
      </div>

      <div class="nearby-search">
        <h2>附近景点</h2>
        <div class="mode-tabs" role="tablist" aria-label="附近景点查询方式">
          <button :class="{ active: nearbyMode === 'name' }" type="button" @click="nearbyMode = 'name'">按地点搜索</button>
          <button :class="{ active: nearbyMode === 'location' }" type="button" @click="nearbyMode = 'location'">使用当前位置</button>
        </div>

        <form v-if="nearbyMode === 'name'" class="nearby-form" @submit.prevent="searchNearbyByName">
          <label>
            <span>地点名称</span>
            <el-input v-model="nearbyForm.name" :prefix-icon="Location" maxlength="100" placeholder="例如：广州塔" />
          </label>
          <label>
            <span>城市（选填）</span>
            <el-input v-model="nearbyForm.city" maxlength="50" placeholder="例如：广州" />
          </label>
          <el-button type="primary" native-type="submit" :loading="nearbyLoading">
            查找附近景点
          </el-button>
        </form>

        <div v-else class="location-action">
          <p>授权浏览器读取当前位置，查找 5 公里内的可预约景点。</p>
          <el-button type="primary" :loading="nearbyLoading" @click="searchNearbyByLocation">
            <el-icon><Aim /></el-icon>获取当前位置
          </el-button>
        </div>
      </div>
    </section>

    <el-alert v-if="nearbyError" class="nearby-error" :title="nearbyError" type="warning" show-icon :closable="false" />

    <section v-if="nearbySearched && !nearbyError" class="nearby-results" v-loading="nearbyLoading">
      <div class="section-heading">
        <div><h2>附近景点</h2><p>按距离由近到远展示</p></div>
        <span>{{ nearbyVenues.length }} 个结果</span>
      </div>
      <el-empty v-if="!nearbyLoading && nearbyVenues.length === 0" description="附近暂时没有可预约景点" />
      <div v-else class="nearby-list">
        <RouterLink v-for="venue in nearbyVenues" :key="venue.id" :to="`/venues/${venue.id}`">
          <VenueImage :src="venue.coverUrl" :alt="`${venue.name}封面`" />
          <div><strong>{{ venue.name }}</strong><span>{{ venue.address }}</span></div>
          <b>{{ Number(venue.distanceKm).toFixed(2) }} km</b>
        </RouterLink>
      </div>
    </section>

    <section class="venue-section">
      <div class="section-heading">
        <div><h2>景点列表</h2><p>共找到 {{ total }} 个景点</p></div>
      </div>

      <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" />
      <div v-loading="loading" class="venue-grid">
        <article v-for="venue in venues" :key="venue.id" :class="['venue-card', { unavailable: venue.minimumPrice == null }]">
          <VenueImage :src="venue.coverUrl" :alt="`${venue.name}封面`" />
          <div class="venue-body">
            <h3>{{ venue.name }}</h3>
            <p class="venue-address"><el-icon><Location /></el-icon>{{ venue.address }}</p>
            <p class="venue-description">{{ venue.description }}</p>
            <footer>
              <strong :class="{ unavailable: venue.minimumPrice == null }">{{ formatPrice(venue.minimumPrice) }}</strong>
              <RouterLink :to="`/venues/${venue.id}`">查看详情</RouterLink>
            </footer>
          </div>
        </article>
      </div>

      <el-empty v-if="!loading && !loadError && venues.length === 0" description="没有找到符合条件的景点" />
      <el-pagination
        v-if="total > 0"
        class="catalog-pagination"
        background
        :current-page="page"
        :page-size="size"
        :page-sizes="[6, 9, 12, 20]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="changePage"
        @size-change="changeSize"
      />
    </section>
  </div>
</template>

<style scoped>
.catalog-page { --green: #0d3b2c; --orange: #ef8610; color: #18251f; }
.catalog-hero { display: grid; grid-template-columns: minmax(420px,.9fr) minmax(520px,1.1fr); gap: 46px; min-height: 300px; padding: 42px 48px; color: #fff; background: var(--green); border-radius: 18px; }
.hero-copy { align-self: center; }
.hero-copy h1 { margin: 0; font-size: clamp(36px,3.5vw,44px); line-height: 1.2; letter-spacing: -.04em; white-space: nowrap; }
.hero-copy p { margin: 22px 0 0; color: #d3e0da; line-height: 1.8; }
.search-summary { margin-top: 24px; color: #ffd08d; font-size: 14px; }
.search-summary a { margin-left: 12px; color: #fff; }
.nearby-search h2 { margin: 0 0 12px; font-size: 21px; }
.mode-tabs { display: flex; gap: 4px; margin-bottom: 16px; }
.mode-tabs button { padding: 9px 14px; color: #cad9d2; cursor: pointer; background: rgb(255 255 255 / 8%); border: 0; border-bottom: 2px solid transparent; }
.mode-tabs button.active { color: #ffb95d; border-bottom-color: var(--orange); }
.nearby-form { display: grid; grid-template-columns: 1fr .8fr auto; gap: 12px; align-items: end; padding: 18px; background: rgb(255 255 255 / 6%); border: 1px solid rgb(255 255 255 / 15%); border-radius: 10px; }
.nearby-form label span { display: block; margin-bottom: 8px; color: #e0e9e5; font-size: 13px; }
.nearby-form :deep(.el-input__wrapper) { min-height: 42px; box-shadow: none; }
.nearby-form .el-button, .location-action .el-button { min-height: 42px; --el-button-bg-color: var(--orange); --el-button-border-color: var(--orange); --el-button-hover-bg-color: #f39a31; --el-button-hover-border-color: #f39a31; }
.location-action { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 21px; background: rgb(255 255 255 / 6%); border: 1px solid rgb(255 255 255 / 15%); border-radius: 10px; }
.location-action p { margin: 0; color: #d8e3de; font-size: 14px; }
.nearby-error { margin-top: 18px; }
.venue-section, .nearby-results { padding: 42px 0 10px; }
.nearby-results { padding-bottom: 0; }
.section-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 18px; }
.section-heading h2 { margin: 0; font-size: 26px; }
.section-heading p, .section-heading > span { margin: 7px 0 0; color: #77817c; font-size: 14px; }
.nearby-list { display: grid; grid-template-columns: repeat(2,1fr); gap: 12px; }
.nearby-list a { display: grid; grid-template-columns: 90px 1fr auto; align-items: center; gap: 14px; padding: 10px; color: inherit; text-decoration: none; background: #fff; border: 1px solid #e2e6e3; border-radius: 10px; }
.nearby-list img { width: 90px; height: 64px; object-fit: cover; border-radius: 7px; }
.nearby-list div { min-width: 0; }
.nearby-list strong, .nearby-list span { display: block; }
.nearby-list span { margin-top: 7px; overflow: hidden; color: #7a837f; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.nearby-list b { color: #d97706; font-size: 13px; }
.venue-grid { display: grid; min-height: 160px; grid-template-columns: repeat(3,1fr); gap: 18px; }
.venue-card { overflow: hidden; background: #fff; border: 1px solid #e1e5e2; border-radius: 12px; }
.venue-card.unavailable { background: #f7f8f7; }
.venue-card > img { width: 100%; aspect-ratio: 16 / 9; object-fit: cover; }
.venue-body { padding: 18px; }
.venue-body h3 { margin: 0; font-size: 20px; }
.venue-address { display: flex; gap: 6px; align-items: flex-start; min-height: 38px; margin: 12px 0; color: #68736d; font-size: 13px; line-height: 1.5; }
.venue-address .el-icon { flex: none; margin-top: 3px; }
.venue-description { display: -webkit-box; min-height: 66px; margin: 0; overflow: hidden; color: #657069; font-size: 14px; line-height: 1.6; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.venue-body footer { display: flex; align-items: center; justify-content: space-between; margin-top: 20px; }
.venue-body footer strong { color: #e66f00; font-size: 20px; }
.venue-body footer strong.unavailable { color: #8a938e; font-size: 15px; }
.venue-body footer a { padding: 9px 14px; color: #fff; font-size: 14px; font-weight: 650; text-decoration: none; background: var(--orange); border-radius: 7px; }
.catalog-pagination { justify-content: center; margin-top: 26px; }
@media (max-width: 1000px) { .catalog-hero { grid-template-columns: 1fr; gap: 30px; } .venue-grid { grid-template-columns: repeat(2,1fr); } }
@media (max-width: 620px) { .catalog-hero { padding: 30px 20px; border-radius: 13px; } .hero-copy h1 { font-size: 34px; white-space: normal; } .nearby-form { grid-template-columns: 1fr; } .location-action { align-items: stretch; flex-direction: column; } .nearby-list, .venue-grid { grid-template-columns: 1fr; } .venue-section, .nearby-results { padding-top: 34px; } .catalog-pagination { overflow-x: auto; justify-content: flex-start; } }
</style>
