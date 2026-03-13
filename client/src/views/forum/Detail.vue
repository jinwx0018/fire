<template>
  <div class="page" v-if="detail">
    <h2>{{ detail.title }}</h2>
    <div class="meta">点赞 {{ detail.likeCount ?? 0 }}</div>
    <div class="body">{{ detail.content }}</div>
    <button v-if="userStore.isLoggedIn" @click="like">点赞</button>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getPostDetail, likePost } from '@/api/forum'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const id = computed(() => route.params.id)
const detail = ref(null)

async function load() {
  try {
    detail.value = await getPostDetail(id.value)
  } catch (e) {
    console.error(e)
  }
}

async function like() {
  try {
    await likePost(id.value)
    load()
  } catch (e) {
    alert(e.message)
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.page .body { white-space: pre-wrap; margin: 12px 0; }
.page button { padding: 8px 16px; background: #1890ff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.loading { padding: 24px; text-align: center; }
</style>
