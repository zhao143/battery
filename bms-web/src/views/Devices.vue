<template>
  <div class="devices-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isAdmin ? '所有设备' : '我的设备' }}</span>
          <el-button type="primary" @click="dialogVisible = true">绑定设备</el-button>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="8" v-for="device in devices" :key="device.id">
          <el-card class="device-card" shadow="hover" @click="goToDetail(device.deviceUuid)">
            <div class="device-header">
              <div class="device-icon">
                <el-icon><Cpu /></el-icon>
              </div>
              <el-tag :type="device.status === 1 ? 'success' : 'info'" size="small">
                {{ device.status === 1 ? '在线' : '离线' }}
              </el-tag>
            </div>
            <div class="device-info">
              <h3>{{ device.deviceName || '未命名设备' }}</h3>
              <p class="device-type">{{ device.deviceType || 'BMS设备' }}</p>
              <p class="device-uuid">{{ device.deviceUuid }}</p>
            </div>
            <div class="device-footer">
              <span>创建时间: {{ formatDate(device.createdAt) }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-empty v-if="devices.length === 0" description="暂无设备，请点击绑定设备按钮" />

      <el-dialog v-model="dialogVisible" title="绑定设备" width="500px">
        <el-form :model="form" label-width="100px">
          <el-form-item label="设备UUID" required>
            <el-input
              v-model="form.deviceUuid"
              placeholder="请输入要绑定的设备UUID"
            />
          </el-form-item>
          <el-alert
            title="提示"
            type="info"
            description="请输入已有的设备UUID进行绑定"
            :closable="false"
            style="margin-top: 10px;"
          />
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">绑定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu } from '@element-plus/icons-vue'
import { batteryApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const devices = ref([])
const dialogVisible = ref(false)
const loading = ref(false)
const form = ref({
  deviceUuid: ''
})

const isAdmin = computed(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    return user.roles && user.roles.includes('admin')
  }
  return false
})

const fetchDevices = async () => {
  try {
    devices.value = await batteryApi.getDevices()
  } catch (e) {
    console.error(e)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '--'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const goToDetail = (uuid) => {
  router.push(`/device/${uuid}`)
}

const handleSubmit = async () => {
  if (!form.value.deviceUuid || !form.value.deviceUuid.trim()) {
    ElMessage.warning('请输入设备UUID')
    return
  }
  loading.value = true
  try {
    const res = await batteryApi.bindDevice(form.value.deviceUuid.trim())
    ElMessage.success(res.message || '设备绑定成功')
    dialogVisible.value = false
    form.value = { deviceUuid: '' }
    fetchDevices()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '绑定失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDevices()
})
</script>

<style scoped>
.devices-page {
  padding: 20px;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
}

.device-card {
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.device-card:hover {
  transform: translateY(-4px);
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.device-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.device-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.device-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
}

.device-type {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.device-uuid {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #999;
  word-break: break-all;
}

.device-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #eee;
  font-size: 12px;
  color: #999;
}
</style>
