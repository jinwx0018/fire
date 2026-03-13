import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/views/Layout.vue'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { guest: true } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('@/views/ForgotPassword.vue'), meta: { guest: true } },
  { path: '/reset-password', name: 'ResetPassword', component: () => import('@/views/ResetPassword.vue'), meta: { guest: true } },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'knowledge', name: 'KnowledgeList', component: () => import('@/views/knowledge/List.vue') },
      { path: 'knowledge/:id', name: 'KnowledgeDetail', component: () => import('@/views/knowledge/Detail.vue') },
      { path: 'forum', name: 'ForumList', component: () => import('@/views/forum/List.vue') },
      { path: 'forum/:id', name: 'ForumDetail', component: () => import('@/views/forum/Detail.vue') },
      { path: 'recommend', name: 'Recommend', component: () => import('@/views/recommend/List.vue') },
      { path: 'equipment', name: 'EquipmentList', component: () => import('@/views/equipment/List.vue') },
      { path: 'equipment/:id', name: 'EquipmentDetail', component: () => import('@/views/equipment/Detail.vue') },
      { path: 'news', name: 'NewsList', component: () => import('@/views/news/List.vue') },
      { path: 'news/:id', name: 'NewsDetail', component: () => import('@/views/news/Detail.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } },
      { path: 'profile/password', name: 'ProfilePassword', component: () => import('@/views/ProfilePassword.vue'), meta: { requiresAuth: true } },
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
