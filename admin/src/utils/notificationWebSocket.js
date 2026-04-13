import { SITE_NOTIFICATIONS_EVENT } from '@/constants/siteNotifications'

/** 供 Layout 区分「HTTP 轮询」与「WS 推送」，避免竞态下未读数已先被轮询更新导致不弹窗 */
const NOTIFICATION_WS_DETAIL = { fromWebSocket: true }

/**
 * 连接后端 `/ws/notifications?token=...`，收到推送后触发站内通知刷新事件（与轮询并存）。
 * @param {string} tokenKey sessionStorage/localStorage 中的 access token 键名
 */
export function createNotificationWebSocket(tokenKey) {
  let ws = null
  let reconnectTimer = null
  let manualClose = false

  function readToken() {
    return window.sessionStorage.getItem(tokenKey) || window.localStorage.getItem(tokenKey) || ''
  }

  function buildUrl(token) {
    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    if (base.startsWith('http://') || base.startsWith('https://')) {
      try {
        const u = new URL(base)
        const wsProto = u.protocol === 'https:' ? 'wss:' : 'ws:'
        const root = u.pathname.replace(/\/$/, '')
        return `${wsProto}//${u.host}${root}/ws/notifications?token=${encodeURIComponent(token)}`
      } catch (_) {
        /* fall through */
      }
    }
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const prefix = base.startsWith('/') ? base : `/${base}`
    return `${proto}//${window.location.host}${prefix}/ws/notifications?token=${encodeURIComponent(token)}`
  }

  function scheduleReconnect() {
    if (manualClose) return
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null
      open()
    }, 2600)
  }

  function open() {
    const token = readToken()
    if (!token) return
    manualClose = false
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }
    if (ws) {
      try {
        ws.close()
      } catch (_) {}
      ws = null
    }
    try {
      ws = new WebSocket(buildUrl(token))
    } catch (_) {
      scheduleReconnect()
      return
    }
    ws.onmessage = () => {
      window.dispatchEvent(new CustomEvent(SITE_NOTIFICATIONS_EVENT, { detail: NOTIFICATION_WS_DETAIL }))
    }
    ws.onclose = () => {
      ws = null
      scheduleReconnect()
    }
    ws.onerror = () => {
      try {
        if (ws) ws.close()
      } catch (_) {}
    }
  }

  function close() {
    manualClose = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      try {
        ws.close()
      } catch (_) {}
      ws = null
    }
  }

  function restart() {
    close()
    manualClose = false
    open()
  }

  return { open, close, restart }
}
