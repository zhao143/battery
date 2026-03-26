<template>
  <el-container class="layout-container">
    <el-aside width="220px">
      <div class="logo">
        <el-icon class="logo-icon"><Monitor /></el-icon>
        <span>电池管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1a1a2e"
        text-color="#fff"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item v-if="!isAdmin" index="/devices">
          <el-icon><Cpu /></el-icon>
          <span>我的设备</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/devices">
          <el-icon><Cpu /></el-icon>
          <span>所有设备</span>
        </el-menu-item>
        <el-menu-item index="/threshold">
          <el-icon><Setting /></el-icon>
          <span>阈值设置</span>
        </el-menu-item>
        <el-menu-item index="/admin/users" v-if="isAdmin">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/devices" v-if="isAdmin">
          <el-icon><Setting /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-left">
          <h2>{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><UserFilled /></el-icon>
              <span>{{ user?.nickname || user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Monitor, DataAnalysis, Cpu, User, Setting, UserFilled, ArrowDown } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const user = ref(null)

const activeMenu = computed(() => route.path)
const isAdmin = computed(() => user.value?.roles?.includes('admin'))

const pageTitle = computed(() => {
  const titles = {
    '/dashboard': '数据概览',
    '/devices': isAdmin.value ? '所有设备' : '我的设备',
    '/threshold': '阈值设置',
    '/admin/users': '用户管理',
    '/admin/devices': '设备管理'
  }
  return titles[route.path] || '电池管理系统'
})

const handleCommand = (command) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.el-aside {
  background-color: #1a1a2e;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logo-icon {
  font-size: 28px;
  color: #409eff;
}

.el-menu {
  border: none;
}

.el-menu-item {
  height: 50px;
  line-height: 50px;
}

.el-menu-item.is-active {
  background: linear-gradient(90deg, #409eff 0%, #337ecc 100%) !important;
}

.el-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.3s;
}

.user-info:hover {
  background: #f5f7fa;
}

.el-main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
