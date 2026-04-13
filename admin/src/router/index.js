import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/views/Layout.vue'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', meta: { title: '首页' }, component: () => import('@/views/Dashboard.vue') },
      { path: 'notifications', name: 'AdminNotifications', component: () => import('@/views/notifications/NotificationList.vue') },
      {
        path: 'notifications/:id',
        name: 'AdminNotificationDetail',
        component: () => import('@/views/notifications/NotificationDetail.vue'),
      },
      {
        path: 'user',
        component: () => import('@/views/user/UserManage.vue'),
        redirect: { name: 'UserList' },
        children: [
          { path: 'list', name: 'UserList', component: () => import('@/views/user/UserList.vue') },
          {
            path: 'author-applications',
            name: 'AuthorApplicationList',
            component: () => import('@/views/author/ApplicationList.vue'),
          },
        ],
      },
      { path: 'user/add', name: 'UserAdd', component: () => import('@/views/user/UserForm.vue') },
      { path: 'user/edit/:id', name: 'UserEdit', component: () => import('@/views/user/UserForm.vue') },
      { path: 'author-applications', redirect: '/user/author-applications' },
      { path: 'content/add', name: 'ContentAdd', component: () => import('@/views/content/ContentForm.vue') },
      { path: 'content/edit/:id', name: 'ContentEdit', component: () => import('@/views/content/ContentForm.vue') },
      {
        path: 'knowledge',
        component: () => import('@/views/content/KnowledgeManage.vue'),
        redirect: { name: 'ContentList' },
        children: [
          { path: 'list', name: 'ContentList', component: () => import('@/views/content/ContentList.vue') },
          { path: 'category', name: 'CategoryList', component: () => import('@/views/content/CategoryList.vue') },
          {
            path: 'comments',
            name: 'KnowledgeCommentAudit',
            component: () => import('@/views/content/KnowledgeCommentAudit.vue'),
          },
        ],
      },
      { path: 'content', redirect: { name: 'ContentList' } },
      { path: 'category', redirect: { name: 'CategoryList' } },
      {
        path: 'system',
        component: () => import('@/views/system/SystemConfig.vue'),
        redirect: { name: 'RecycleList' },
        children: [
          { path: 'recycle', name: 'RecycleList', component: () => import('@/views/content/RecycleList.vue') },
          { path: 'audit-logs', name: 'AuditLogList', component: () => import('@/views/audit/AuditLogList.vue') },
          { path: 'monitor', name: 'Monitor', component: () => import('@/views/Monitor.vue') },
        ],
      },
      { path: 'content/recycle', redirect: { name: 'RecycleList' } },
      { path: 'audit-logs', redirect: { name: 'AuditLogList' } },
      { path: 'monitor', redirect: { name: 'Monitor' } },
      { path: 'forum/post-view', name: 'ForumPostAuditView', component: () => import('@/views/forum/ForumPostAuditView.vue') },
      {
        path: 'forum',
        component: () => import('@/views/forum/ForumManage.vue'),
        redirect: { name: 'ForumAudit' },
        children: [
          { path: 'posts', name: 'ForumAudit', component: () => import('@/views/forum/PostAudit.vue') },
          { path: 'comments', name: 'ForumCommentAudit', component: () => import('@/views/forum/ForumCommentAudit.vue') },
        ],
      },
      { path: 'equipment/add', name: 'EquipmentAdd', component: () => import('@/views/equipment/EquipmentForm.vue') },
      { path: 'equipment/edit/:id', name: 'EquipmentEdit', component: () => import('@/views/equipment/EquipmentForm.vue') },
      {
        path: 'equipment',
        component: () => import('@/views/equipment/EquipmentManage.vue'),
        redirect: { name: 'EquipmentList' },
        children: [
          { path: 'list', name: 'EquipmentList', component: () => import('@/views/equipment/EquipmentList.vue') },
          { path: 'types', name: 'EquipmentTypeList', component: () => import('@/views/equipment/EquipmentTypeList.vue') },
        ],
      },
      { path: 'news/add', name: 'NewsAdd', component: () => import('@/views/news/NewsForm.vue') },
      { path: 'news/edit/:id', name: 'NewsEdit', component: () => import('@/views/news/NewsForm.vue') },
      {
        path: 'news',
        component: () => import('@/views/news/NewsManage.vue'),
        redirect: { name: 'NewsList' },
        children: [
          { path: 'list', name: 'NewsList', component: () => import('@/views/news/NewsList.vue') },
          { path: 'categories', name: 'NewsCategoryList', component: () => import('@/views/news/NewsCategoryList.vue') },
          { path: 'comments', name: 'NewsCommentAudit', component: () => import('@/views/news/NewsCommentAudit.vue') },
        ],
      },
      { path: 'comments/detail', name: 'CommentAuditDetail', component: () => import('@/views/CommentAuditDetail.vue') },
      { path: 'comments', redirect: { name: 'NewsCommentAudit' } },
      { path: 'statistics', redirect: '/dashboard' },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const store = useUserStore()
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    next({ name: 'Login' })
    return
  }
  // 防止非管理员 token（如旧缓存）进入管理后台
  if (to.meta.requiresAuth && store.isLoggedIn && store.user?.role !== 'ADMIN') {
    store.logout()
    next({ name: 'Login' })
    return
  }
  if (to.meta.guest && store.isLoggedIn) {
    next({ path: '/' })
    return
  }
  next()
})

export default router
