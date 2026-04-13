<template>
  <div class="page">
    <h2>作者认证申请</h2>
    <p class="tip">
      成为作者后，您可以在「我的知识」中创建并提交消防知识，经管理员审核通过后将公开展示。下方表格为<strong>全部申请历史</strong>，按提交时间<strong>最新在上</strong>；当前可执行的操作以<strong>最新一条</strong>的记录状态为准。
    </p>

    <template v-if="loadStatus === 'loading'">
      <p class="status-tip">加载中…</p>
    </template>
    <template v-else-if="loadStatus === 'error'">
      <p class="status-tip reject">加载失败，请稍后重试。</p>
      <button @click="load">重新加载</button>
    </template>
    <template v-else>
      <div v-if="records.length" class="history-wrap">
        <h3 class="history-title">申请历史（最新在上）</h3>
        <div class="table-scroll">
          <table class="history-table">
            <thead>
              <tr>
                <th>提交时间</th>
                <th>状态</th>
                <th>申请说明</th>
                <th>附件</th>
                <th>审核时间</th>
                <th>备注 / 原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in records" :key="row.id">
                <td>{{ row.createTime ? formatDateTime(row.createTime) : '—' }}</td>
                <td><span class="st" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span></td>
                <td class="reason-cell">{{ row.applyReason || '—' }}</td>
                <td class="attach-cell">
                  <template v-if="splitAttach(row.attachments).length">
                    <a
                      v-for="(u, i) in splitAttach(row.attachments)"
                      :key="i"
                      :href="u"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="attach-link"
                    >{{ i + 1 }}</a>
                  </template>
                  <span v-else>—</span>
                </td>
                <td>{{ row.status !== 'PENDING' && row.reviewTime ? formatDateTime(row.reviewTime) : '—' }}</td>
                <td class="extra-cell">
                  <span v-if="row.status === 'APPROVED' && row.reviewRemark">{{ row.reviewRemark }}</span>
                  <span v-else-if="row.status === 'REJECTED' && row.rejectReason" class="reject-text">{{ row.rejectReason }}</span>
                  <span v-else>—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <template v-if="appStatus === null && !applied">
        <p class="status-tip">您尚未提交申请。</p>
        <div class="apply-form">
          <label class="label">申请原因</label>
          <textarea v-model="applyReason" placeholder="请填写申请成为作者的原因（建议 10 字以上）" rows="4"></textarea>
          <label class="label">附件链接（可选）</label>
          <div class="attach-row">
            <input type="file" ref="attachInput" class="file-hide" accept=".pdf,image/jpeg,image/png,image/gif,image/webp" @change="onAttachUpload" />
            <button type="button" class="btn-upload" :disabled="uploadingAttach" @click="attachInput?.click()">{{ uploadingAttach ? '上传中…' : '本地上传' }}</button>
            <span class="hint">支持图片或 PDF，最大 5MB，可多次上传</span>
          </div>
          <input v-model="attachments" placeholder="可填网盘/图片链接，多个请用英文逗号分隔" />
        </div>
        <button @click="submit" :disabled="submitting">提交申请</button>
      </template>
      <template v-else-if="appStatus === 'PENDING'">
        <p class="status-tip pending">最新一条为「审核中」，请耐心等待管理员处理。</p>
      </template>
      <template v-else-if="appStatus === 'APPROVED' && authorRoleActive">
        <p class="status-tip success">您已是作者，可以前往「我的知识」发布知识。</p>
        <router-link to="/knowledge/drafts" class="btn-link">进入我的知识</router-link>
      </template>
      <template v-else-if="appStatus === 'APPROVED' && !authorRoleActive">
        <p class="status-tip revoke">管理员已将您的账号调整为普通用户；上表中可见历史「已通过」记录。如需再次发布知识，请重新提交作者申请。</p>
        <div class="apply-form">
          <label class="label">申请原因</label>
          <textarea v-model="applyReason" placeholder="请填写申请成为作者的原因（建议 10 字以上）" rows="4"></textarea>
          <label class="label">附件链接（可选）</label>
          <div class="attach-row">
            <input type="file" ref="attachInputRevoke" class="file-hide" accept=".pdf,image/jpeg,image/png,image/gif,image/webp" @change="onAttachUpload" />
            <button type="button" class="btn-upload" :disabled="uploadingAttach" @click="attachInputRevoke?.click()">{{ uploadingAttach ? '上传中…' : '本地上传' }}</button>
            <span class="hint">支持图片或 PDF，最大 5MB，可多次上传</span>
          </div>
          <input v-model="attachments" placeholder="可填网盘/图片链接，多个请用英文逗号分隔" />
        </div>
        <button @click="submit" :disabled="submitting">重新提交申请</button>
      </template>
      <template v-else-if="appStatus === 'REJECTED'">
        <p class="status-tip reject">最新一条为「未通过」，您可修改说明后重新提交申请。</p>
        <div class="apply-form">
          <label class="label">再次申请原因</label>
          <textarea v-model="applyReason" placeholder="请完善申请理由后重新提交" rows="4"></textarea>
          <label class="label">附件链接（可选）</label>
          <div class="attach-row">
            <input type="file" ref="attachInput2" class="file-hide" accept=".pdf,image/jpeg,image/png,image/gif,image/webp" @change="onAttachUpload" />
            <button type="button" class="btn-upload" :disabled="uploadingAttach" @click="attachInput2?.click()">{{ uploadingAttach ? '上传中…' : '本地上传' }}</button>
            <span class="hint">支持图片或 PDF，最大 5MB</span>
          </div>
          <input v-model="attachments" placeholder="可填网盘/图片链接，多个请用英文逗号分隔" />
        </div>
        <button @click="submit" :disabled="submitting">重新申请</button>
      </template>
      <template v-else>
        <p class="status-tip">您尚未提交申请。</p>
        <button @click="submit" :disabled="submitting">提交申请</button>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { applyAuthor, getMyAuthorApplicationsOverview, getUserInfo, uploadAuthorAttachment } from '@/api/user'
import { formatDateTime } from '@/utils/formatDateTime'

const loadStatus = ref('loading')
const records = ref([])
const authorRoleActive = ref(true)
const submitting = ref(false)
const applyReason = ref('')
const attachments = ref('')
const attachInput = ref(null)
const attachInput2 = ref(null)
const attachInputRevoke = ref(null)
const uploadingAttach = ref(false)
const userStore = useUserStore()

const applied = computed(() => records.value.length > 0)
const appStatus = computed(() => {
  const r = records.value[0]
  return r?.status ?? null
})

function statusLabel(s) {
  if (s === 'PENDING') return '审核中'
  if (s === 'APPROVED') return '已通过'
  if (s === 'REJECTED') return '未通过'
  return '—'
}

function statusClass(s) {
  if (s === 'PENDING') return 'st-pending'
  if (s === 'APPROVED') return 'st-ok'
  if (s === 'REJECTED') return 'st-reject'
  return ''
}

function splitAttach(raw) {
  if (!raw || typeof raw !== 'string') return []
  return raw
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
}

async function onAttachUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadingAttach.value = true
  try {
    const data = await uploadAuthorAttachment(file)
    const url = data?.url || data
    if (url) {
      attachments.value = attachments.value?.trim()
        ? `${attachments.value.trim()},${url}`
        : url
      alert('已上传，链接已填入下方')
    }
  } catch (err) {
    alert(err.message || '上传失败')
  } finally {
    uploadingAttach.value = false
    e.target.value = ''
  }
}

async function load() {
  loadStatus.value = 'loading'
  try {
    const data = await getMyAuthorApplicationsOverview()
    authorRoleActive.value = data.authorRoleActive !== false
    const list = data.records ?? data.list ?? []
    records.value = Array.isArray(list) ? list : []
    loadStatus.value = 'done'
    try {
      const userData = await getUserInfo()
      if (userData?.role != null) userStore.updateUser({ role: userData.role })
    } catch (_) {}
  } catch (e) {
    console.error(e)
    loadStatus.value = 'error'
  }
}

async function submit() {
  if (!applyReason.value || applyReason.value.trim().length < 10) {
    alert('请填写至少 10 个字的申请原因')
    return
  }
  submitting.value = true
  try {
    await applyAuthor({
      applyReason: applyReason.value.trim(),
      attachments: attachments.value?.trim() || undefined,
    })
    alert('申请已提交，请等待管理员审核')
    applyReason.value = ''
    attachments.value = ''
    await load()
  } catch (e) {
    alert(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 24px; border-radius: 8px; max-width: 920px; }
.tip { color: #666; font-size: 14px; margin-bottom: 20px; line-height: 1.5; }
.status-tip { margin-bottom: 12px; font-size: 14px; }
.status-tip.pending { color: #faad14; }
.status-tip.success { color: #52c41a; }
.status-tip.reject { color: #ff4d4f; }
.status-tip.revoke { color: #d48806; background: #fffbe6; padding: 10px 12px; border-radius: 6px; border: 1px solid #ffe58f; }
.apply-form { margin-bottom: 12px; }
.label { display: block; font-size: 13px; margin: 8px 0 6px; color: #666; }
textarea, input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 8px 10px;
  font-size: 13px;
}
button { padding: 8px 20px; background: #1890ff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
button:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-link { display: inline-block; margin-top: 8px; color: #1890ff; text-decoration: none; }
.btn-link:hover { text-decoration: underline; }
.attach-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: 8px; }
.file-hide { position: absolute; width: 0; height: 0; opacity: 0; }
.btn-upload {
  padding: 6px 12px; font-size: 13px; cursor: pointer;
  background: #fff; color: #1890ff; border: 1px solid #1890ff; border-radius: 4px;
}
.btn-upload:disabled { opacity: 0.6; cursor: not-allowed; }
.attach-row .hint { font-size: 12px; color: #999; flex: 1; min-width: 200px; }

.history-wrap { margin-bottom: 24px; }
.history-title { margin: 0 0 12px; font-size: 15px; color: #333; }
.table-scroll { overflow-x: auto; border: 1px solid #eee; border-radius: 8px; }
.history-table { width: 100%; border-collapse: collapse; font-size: 13px; min-width: 640px; }
.history-table th,
.history-table td { padding: 10px; border-bottom: 1px solid #f0f0f0; text-align: left; vertical-align: top; }
.history-table th { background: #fafafa; color: #666; font-weight: 600; }
.history-table tr:last-child td { border-bottom: none; }
.reason-cell { max-width: 220px; white-space: pre-wrap; word-break: break-word; }
.attach-cell .attach-link { margin-right: 6px; color: #1890ff; }
.extra-cell { max-width: 180px; white-space: pre-wrap; word-break: break-word; }
.reject-text { color: #cf1322; }
.st { font-weight: 600; }
.st-pending { color: #d48806; }
.st-ok { color: #389e0d; }
.st-reject { color: #cf1322; }
</style>
