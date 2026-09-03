<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import { getCurrentVenue, updateCurrentVenue } from '../api/venue'
import type { Venue, VenueUpdateRequest } from '../types/venue'
import { getRequestErrorMessage } from '../utils/request'

const formRef = ref<FormInstance>()
const venue = ref<Venue | null>(null)
const coverImage = ref<File>()
const loading = ref(false)
const saving = ref(false)
const loadError = ref('')

const form = reactive<VenueUpdateRequest>({
  name: '',
  address: '',
  description: '',
  status: 'ENABLED',
  longitude: null,
  latitude: null,
})

const rules: FormRules<VenueUpdateRequest> = {
  name: [
    { required: true, whitespace: true, message: '请输入景点名称', trigger: 'blur' },
    { max: 100, message: '景点名称不能超过 100 个字符', trigger: 'blur' },
  ],
  address: [
    { required: true, whitespace: true, message: '请输入景点地址', trigger: 'blur' },
    { max: 255, message: '景点地址不能超过 255 个字符', trigger: 'blur' },
  ],
  description: [
    { max: 2000, message: '景点简介不能超过 2000 个字符', trigger: 'blur' },
  ],
}

function fillForm(data: Venue): void {
  form.name = data.name
  form.address = data.address
  form.description = data.description ?? ''
  form.status = data.status
  form.longitude = data.longitude
  form.latitude = data.latitude
  coverImage.value = undefined
}

async function loadVenue(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    venue.value = await getCurrentVenue()
    fillForm(venue.value)
  } catch (error) {
    loadError.value = getRequestErrorMessage(error, '景点信息加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function handleCoverChange(file: UploadFile): void {
  coverImage.value = file.raw
}

function handleCoverRemove(): void {
  coverImage.value = undefined
}

async function handleSave(): Promise<void> {
  if (!formRef.value || saving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateCurrentVenue({
      ...form,
      name: form.name.trim(),
      address: form.address.trim(),
      description: form.description.trim(),
      coverImage: coverImage.value,
    })
    ElMessage.success('景点信息已保存')
    await loadVenue()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '景点信息保存失败，请稍后重试'))
  } finally {
    saving.value = false
  }
}

onMounted(loadVenue)
</script>

<template>
  <section class="venue-page">
    <header class="page-heading">
      <h1>景点信息</h1>
      <p>维护当前景点对游客展示的基础资料。</p>
    </header>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button link type="primary" @click="loadVenue">重新加载</el-button>
      </template>
    </el-alert>

    <div v-else class="venue-panel" v-loading="loading">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        hide-required-asterisk
        @submit.prevent="handleSave"
      >
        <div class="form-layout">
          <div class="cover-column">
            <span class="field-label">景点封面</span>
            <div class="cover-box">
              <img v-if="venue?.coverUrl" :src="venue.coverUrl" :alt="`${venue.name}封面`" />
              <span v-else>暂无封面</span>
            </div>
            <el-upload
              action="#"
              accept="image/*"
              :auto-upload="false"
              :limit="1"
              :on-change="handleCoverChange"
              :on-remove="handleCoverRemove"
            >
              <el-button>选择新封面</el-button>
              <template #tip>
                <div class="upload-tip">不选择则保留原封面</div>
              </template>
            </el-upload>
          </div>

          <div class="form-column">
            <el-form-item label="景点名称" prop="name">
              <el-input v-model="form.name" maxlength="100" show-word-limit />
            </el-form-item>

            <el-form-item label="详细地址" prop="address">
              <el-input v-model="form.address" maxlength="255" show-word-limit />
            </el-form-item>

            <el-form-item label="景点简介" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="5"
                maxlength="2000"
                show-word-limit
                placeholder="介绍景点特色、参观内容等"
              />
            </el-form-item>

            <el-form-item label="运营状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio-button value="ENABLED">正常开放</el-radio-button>
                <el-radio-button value="DISABLED">暂停开放</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <div class="coordinate-grid">
              <el-form-item label="经度（可选）" prop="longitude">
                <el-input-number
                  v-model="form.longitude"
                  :min="-180"
                  :max="180"
                  :precision="6"
                  controls-position="right"
                />
              </el-form-item>
              <el-form-item label="纬度（可选）" prop="latitude">
                <el-input-number
                  v-model="form.latitude"
                  :min="-90"
                  :max="90"
                  :precision="6"
                  controls-position="right"
                />
              </el-form-item>
            </div>

            <div class="form-actions">
              <el-button :disabled="saving" @click="venue && fillForm(venue)">恢复原值</el-button>
              <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
            </div>
          </div>
        </div>
      </el-form>
    </div>
  </section>
</template>

<style scoped>
.venue-page {
  max-width: 1080px;
  color: #202733;
}

.page-heading {
  margin-bottom: 24px;
}

.page-heading h1 {
  margin: 0 0 8px;
  font-size: 27px;
}

.page-heading p {
  margin: 0;
  font-size: 14px;
  color: #75808e;
}

.venue-panel {
  --el-color-primary: #e99000;

  min-height: 360px;
  padding: 28px;
  background: #fff;
  border: 1px solid #dfe3e8;
  border-radius: 9px;
  box-shadow: 0 3px 10px rgb(32 39 51 / 4%);
}

.form-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 34px;
}

.field-label,
.venue-panel :deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-size: 13px;
  font-weight: 650;
  color: #3f4855;
}

.field-label {
  display: block;
}

.cover-box {
  display: grid;
  width: 100%;
  aspect-ratio: 4 / 3;
  margin-bottom: 14px;
  overflow: hidden;
  color: #8a94a0;
  background: #f4f6f8;
  border: 1px solid #e1e5e9;
  border-radius: 7px;
  place-items: center;
}

.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-tip {
  margin-top: 7px;
  font-size: 12px;
  color: #8a94a0;
}

.venue-panel :deep(.el-input__wrapper),
.venue-panel :deep(.el-textarea__inner) {
  border-radius: 6px;
}

.venue-panel :deep(.el-input__wrapper) {
  min-height: 42px;
}

.venue-panel :deep(.el-form-item) {
  margin-bottom: 22px;
}

.coordinate-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.coordinate-grid :deep(.el-input-number) {
  width: 100%;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 4px;
  border-top: 1px solid #edf0f3;
}

.form-actions :deep(.el-button) {
  min-width: 96px;
  margin-top: 20px;
}

.form-actions :deep(.el-button--primary) {
  --el-button-bg-color: #e99000;
  --el-button-border-color: #e99000;
  --el-button-hover-bg-color: #f0a11a;
  --el-button-hover-border-color: #f0a11a;
}

@media (max-width: 800px) {
  .form-layout {
    grid-template-columns: 1fr;
  }

  .cover-column {
    max-width: 320px;
  }
}

@media (max-width: 520px) {
  .page-heading h1 {
    font-size: 24px;
  }

  .venue-panel {
    padding: 20px 16px;
  }

  .coordinate-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
