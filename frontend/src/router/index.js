import { createRouter, createWebHistory } from 'vue-router';
import AdminLayout from '../layouts/AdminLayout.vue';
import { navigationItems } from '../constants/navigation';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      children: navigationItems.map((item) => ({
        path: item.path.replace(/^\//, ''),
        name: item.name,
        component: item.component,
        meta: item.meta
      }))
    }
  ],
  scrollBehavior() {
    return { top: 0 };
  }
});

export default router;
