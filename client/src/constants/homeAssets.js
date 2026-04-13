/**
 * 首页图片统一目录：public/assets/home（用户可自行替换图片文件）
 */
export const HOME_ASSET_FILES = {
  banner: 'assets/home/banner.webp',
  banner1: 'assets/home/banner1.webp',
  banner2: 'assets/home/banner2.webp',
  banner3: 'assets/home/banner3.webp',
  banner4: 'assets/home/banner4.webp',
  knowledge: 'assets/home/knowledge.webp',
  community: 'assets/home/community.webp',
  equipment: 'assets/home/equipment.webp',
  news: 'assets/home/news.webp',
}

export function homeAssetUrl(relativePath) {
  const base = import.meta.env.BASE_URL || '/'
  const p = String(relativePath).replace(/^\//, '')
  const prefix = base.endsWith('/') ? base : `${base}/`
  return `${prefix}${p}`
}
