import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Dashboard from '../views/Dashboard.vue'
import Devices from '../views/Devices.vue'
import DeviceDetail from '../views/DeviceDetail.vue'
import ThresholdSettings from '../views/ThresholdSettings.vue'
import AdminUsers from '../views/admin/Users.vue'
import AdminDevices from '../views/admin/Devices.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: Dashboard },
      { path: 'devices', name: 'Devices', component: Devices },
      { path: 'device/:uuid', name: 'DeviceDetail', component: DeviceDetail, props: true },
      { path: 'threshold', name: 'ThresholdSettings', component: ThresholdSettings },
      { path: 'admin/users', name: 'AdminUsers', component: AdminUsers },
      { path: 'admin/devices', name: 'AdminDevices', component: AdminDevices }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')

  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }

  if (to.path.startsWith('/admin')) {
    if (userStr) {
      const user = JSON.parse(userStr)
      const isAdmin = user.roles && user.roles.includes('admin')
      if (!isAdmin) {
        ElMessage.error('无权限访问')
        next('/dashboard')
        return
      }
    }
  }

  next()
})

export default router
