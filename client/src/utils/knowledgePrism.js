import Prism from 'prismjs'
import 'prismjs/themes/prism-tomorrow.min.css'

/**
 * 对知识详情正文容器内 `pre > code` 执行 Prism 高亮（需在 v-html 更新后的 nextTick 调用）
 */
export function highlightKnowledgeProse(rootEl) {
  if (!rootEl || typeof rootEl.querySelectorAll !== 'function') return
  rootEl.querySelectorAll('pre code').forEach((code) => {
    try {
      Prism.highlightElement(code)
    } catch (_) {
      /* 单块失败不影响其余 */
    }
  })
}
