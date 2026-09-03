<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createVisitor, updateVisitor } from '../api/visitor'
import type { Visitor, VisitorCreatePayload } from '../types/visitor'
import { getRequestErrorMessage } from '../utils/request'

type VisitorFormModel = Omit<VisitorCreatePayload, 'phone'> & { phone: string }

const props = defineProps<{ visible: boolean; visitor: Visitor | null }>()
const emit = defineEmits<{ 'update:visible': [value: boolean]; saved: [] }>()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const formError = ref('')
const isEdit = computed(() => props.visitor !== null)
const dialogVisible = computed({ get: () => props.visible, set: (value) => emit('update:visible', value) })
const form = reactive<VisitorFormModel>({ name: '', idType: 'ID_CARD', idNumber: '', phone: '' })

const rules: FormRules<VisitorFormModel> = {
  name: [{ required: true, message: '请输入参观人姓名', trigger: 'blur' }, { max: 50, message: '姓名不能超过 50 个字符', trigger: 'blur' }],
  idType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  idNumber: [{ required: true, message: '请输入证件号码', trigger: 'blur' }, { max: 64, message: '证件号码不能超过 64 个字符', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }],
}

watch(() => props.visible, (visible) => {
  if (!visible) return
  form.name = props.visitor?.name ?? ''
  form.idType = props.visitor?.idType ?? 'ID_CARD'
  form.idNumber = props.visitor?.maskedIdNumber ?? ''
  form.phone = props.visitor?.phone ?? ''
  formError.value = ''
  nextTick(() => formRef.value?.clearValidate())
})

async function submit(): Promise<void> {
  if (!formRef.value || submitting.value) return
  if (!await formRef.value.validate().catch(() => false)) return
  submitting.value = true
  formError.value = ''
  try {
    if (props.visitor) {
      await updateVisitor(props.visitor.id, { name: form.name.trim(), phone: form.phone.trim() || null })
      ElMessage.success('参观人信息已更新')
    } else {
      await createVisitor({
        name: form.name.trim(),
        idType: form.idType,
        idNumber: form.idNumber.trim(),
        phone: form.phone.trim() || null,
      })
      ElMessage.success('参观人已新增')
    }
    emit('saved')
    dialogVisible.value = false
  } catch (error) {
    formError.value = getRequestErrorMessage(error, '保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog v-model="dialogVisible" class="visitor-form-dialog" :title="isEdit ? '编辑参观人' : '新增参观人'" width="500" append-to-body>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" hide-required-asterisk @submit.prevent="submit">
      <el-form-item label="参观人姓名" prop="name">
        <el-input v-model="form.name" maxlength="50" placeholder="请输入参观人姓名" size="large" />
      </el-form-item>
      <el-form-item label="证件类型" prop="idType">
        <el-select v-model="form.idType" :disabled="isEdit" size="large">
          <el-option label="身份证" value="ID_CARD" />
          <el-option label="护照" value="PASSPORT" />
        </el-select>
      </el-form-item>
      <el-form-item label="证件号码" prop="idNumber">
        <el-input v-model="form.idNumber" :disabled="isEdit" maxlength="64" placeholder="请输入证件号码" size="large" />
        <span v-if="isEdit" class="field-tip">证件信息不允许修改</span>
      </el-form-item>
      <el-form-item label="手机号（选填）" prop="phone">
        <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" size="large" />
      </el-form-item>
      <el-alert v-if="formError" :title="formError" type="error" show-icon :closable="false" />
      <el-button class="save-button" type="primary" native-type="submit" :loading="submitting">保存</el-button>
    </el-form>
  </el-dialog>
</template>

<style>
.visitor-form-dialog { --el-color-primary: #ef8610; max-width: calc(100vw - 24px); border-radius: 14px; }
.visitor-form-dialog .el-dialog__header { padding: 24px 26px 12px; }
.visitor-form-dialog .el-dialog__title { font-size: 22px; font-weight: 700; color: #1b2721; }
.visitor-form-dialog .el-dialog__body { padding: 14px 26px 26px; }
.visitor-form-dialog .el-form-item { margin-bottom: 18px; }
.visitor-form-dialog .el-form-item__label { padding-bottom: 7px; font-weight: 600; color: #344039; }
.visitor-form-dialog .el-select { width: 100%; }
.visitor-form-dialog .field-tip { margin-top: 6px; color: #8a928e; font-size: 12px; }
.visitor-form-dialog .el-alert { margin-bottom: 18px; }
.visitor-form-dialog .save-button { width: 100%; min-height: 46px; font-size: 16px; font-weight: 650; }
@media (max-width: 600px) { .visitor-form-dialog { position: fixed; bottom: 0; left: 0; width: 100% !important; max-width: none; max-height: calc(100vh - 20px); margin: 0; overflow: auto; border-radius: 18px 18px 0 0; } .visitor-form-dialog .el-dialog__header { padding: 25px 20px 12px; } .visitor-form-dialog .el-dialog__body { padding: 12px 20px 22px; } }
</style>
