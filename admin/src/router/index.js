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
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
      { path: 'user', name: 'UserList', component: () => import('@/views/user/UserList.vue') },
      { path: 'user/add', name: 'UserAdd', component: () => import('@/views/user/UserForm.vue') },
      { path: 'user/edit/:id', name: 'UserEdit', component: () => import('@/views/user/UserForm.vue') },
      { path: 'content', name: 'ContentList', component: () => import('@/views/content/ContentList.vue') },
      { path: 'content/add', name: 'ContentAdd', component: () => import('@/views/content/ContentForm.vue') },
      { path: 'content/edit/:id', name: 'ContentEdit', component: () => import('@/views/content/ContentForm.vue') },
      { path: 'category', name: 'CategoryList', component: () => import('@/views/content/CategoryList.vue') },
      { path: 'forum', name: 'ForumAudit', component: () => import('@/views/forum/PostAudit.vue') },
      { path: 'equipment', name: 'EquipmentList', component: () => import('@/views/equipment/EquipmentList.vue') },
      { path: 'equipment/add', name: 'EquipmentAdd', component: () => import('@/views/equipment/EquipmentForm.vue') },
      { path: 'equipment/edit/:id', name: 'EquipmentEdit', component: () => import('@/views/equipment/EquipmentForm.vue') },
      { path: 'news', name: 'NewsList', component: () => import('@/views/news/NewsList.vue') },
      { path: 'news/add', name: 'NewsAdd', component: () => import('@/views/news/NewsForm.vue') },
      { path: 'news/edit/:id', name: 'NewsEdit', component: () => import('@/views/news/NewsForm.vue') },
      { path: 'statistics', name: 'Statistics', component: () => import('@/views/statistics/Statistics.vue') },
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
  if (to.meta.guest && store.isLoggedIn) {
    next({ path: '/' })
    return
  }
  next()
})

export default router
