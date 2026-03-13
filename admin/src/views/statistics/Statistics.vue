<template>
  <div class="page">
    <h2>数据统计</h2>
    <div class="cards">
      <div class="card" v-if="contentStats">
        <h3>内容维度</h3>
        <pre>{{ JSON.stringify(contentStats, null, 2) }}</pre>
      </div>
      <div class="card" v-if="userStats">
        <h3>用户维度</h3>
        <pre>{{ JSON.stringify(userStats, null, 2) }}</pre>
      </div>
      <div class="card" v-if="interactionStats">
        <h3>互动维度</h3>
        <pre>{{ JSON.stringify(interactionStats, null, 2) }}</pre>
      </div>
    </div>
    <p v-if="err" class="err">{{ err }}</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getContentStats, getUserStats, getInteractionStats } from '@/api/statistics'

const contentStats = ref(null)
const userStats = ref(null)
const interactionStats = ref(null)
const err = ref('')

async function load() {
  err.value = ''
  try {
    contentStats.value = await getContentStats()
  } catch (e) {
    contentStats.value = null
    err.value = e.message
  }
  try {
    userStats.value = await getUserStats()
  } catch (_) {
    userStats.value = null
  }
  try {
    interactionStats.value = await getInteractionStats()
  } catch (_) {
    interactionStats.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.page h2 { margin-bottom: 16px; }
.cards { display: flex; flex-wrap: wrap; gap: 16px; }
.card { flex: 1; min-width: 280px; padding: 12px; border: 1px solid #eee; border-radius: 8px; }
.card h3 { font-size: 14px; margin-bottom: 8px; }
.card pre { font-size: 12px; overflow: auto; max-height: 200px; }
.err { color: #c00; }
</style>
