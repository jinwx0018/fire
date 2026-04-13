/** provide/inject：各详情页注入，用于楼中楼「展开其余回复」状态 */
export const COMMENT_REPLY_BRANCH_KEY = Symbol('commentReplyBranch')

/**
 * 评论区树形展示：扁平列表 → 嵌套 → 再扁平为带 depth 的行。
 * sortMode: time=最新优先（同层按时间倒序）；hot=热度优先（同层按点赞数倒序，再按时间）。
 */
export function compareCommentsBySort(a, b, sortMode) {
  if (sortMode === 'hot') {
    const la = Number(a.likeCount ?? 0)
    const lb = Number(b.likeCount ?? 0)
    if (lb !== la) return lb - la
  }
  const ta = a.createTime ? new Date(a.createTime).getTime() : 0
  const tb = b.createTime ? new Date(b.createTime).getTime() : 0
  if (tb !== ta) return tb - ta
  const ida = Number(a.id ?? 0)
  const idb = Number(b.id ?? 0)
  return idb - ida
}

export function buildCommentTree(flat, sortMode = 'time') {
  if (!flat?.length) return []
  const byId = {}
  for (const c of flat) {
    byId[c.id] = { ...c, children: [] }
  }
  const roots = []
  for (const c of flat) {
    const node = byId[c.id]
    if (c.parentId != null && byId[c.parentId]) {
      byId[c.parentId].children.push(node)
    } else {
      roots.push(node)
    }
  }
  const sortCh = (arr) => {
    arr.sort((a, b) => compareCommentsBySort(a, b, sortMode))
    for (const n of arr) {
      if (n.children?.length) sortCh(n.children)
    }
  }
  sortCh(roots)
  return roots
}

export function flattenCommentTree(nodes, depth = 0) {
  const out = []
  for (const n of nodes) {
    out.push({ ...n, depth, children: undefined })
    if (n.children?.length) {
      out.push(...flattenCommentTree(n.children, depth + 1))
    }
  }
  return out
}

/** 当前页评论行数超过该值时，默认折叠列表，需点击「展开全部」 */
export const COMMENT_LIST_COLLAPSE_THRESHOLD = 8

/** 同一条评论下直接子回复超过该值时，折叠楼中楼，需点击「展开其余」 */
export const COMMENT_REPLY_BRANCH_THRESHOLD = 3

/**
 * 仅保留 id 落在 idSet 中的节点及其必要祖先链已由 DFS 前序截断保证；
 * 用于「展开全部评论」折叠时与 flatten 前 N 条一致。
 */
export function filterTreeByIdSet(nodes, idSet) {
  if (!nodes?.length) return []
  const out = []
  for (const n of nodes) {
    if (!idSet.has(n.id)) continue
    const children = n.children?.length ? filterTreeByIdSet(n.children, idSet) : []
    out.push({ ...n, children })
  }
  return out
}
