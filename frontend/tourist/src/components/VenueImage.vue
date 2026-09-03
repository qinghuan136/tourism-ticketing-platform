<script setup lang="ts">
import { ref, watch } from 'vue'
import fallbackImage from '../assets/venue-fallback.jpg'

const props = defineProps<{
  src?: string | null
  alt: string
}>()

const imageSource = ref(props.src || fallbackImage)

watch(
  () => props.src,
  (src) => {
    imageSource.value = src || fallbackImage
  },
)

function useFallback(): void {
  if (imageSource.value !== fallbackImage) imageSource.value = fallbackImage
}
</script>

<template>
  <img :src="imageSource" :alt="alt" @error="useFallback">
</template>
