/**
 * 知识详情正文：为 h2/h3 补齐 id 并生成目录（用于侧栏导航与锚点滚动）
 * 入参应为已通过 sanitizeHtml 的 HTML
 */
export function injectHeadingIdsAndToc(rawHtml) {
  const html = String(rawHtml || '')
  if (!html.trim()) {
    return { html: '', toc: [] }
  }
  try {
    const parser = new DOMParser()
    const doc = parser.parseFromString(html, 'text/html')
    const toc = []
    let seq = 0
    doc.body.querySelectorAll('h2, h3').forEach((el) => {
      const text = el.textContent?.trim() || ''
      if (!text) return
      const level = el.tagName === 'H2' ? 2 : 3
      let hid = el.getAttribute('id')?.trim()
      if (!hid) {
        hid = `k-h-${seq}`
        el.setAttribute('id', hid)
      }
      seq += 1
      toc.push({ id: hid, text, level })
    })
    return { html: doc.body.innerHTML, toc }
  } catch {
    return { html, toc: [] }
  }
}
