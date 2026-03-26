<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ devices.length }}</div>
            <div class="stat-label">设备数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ onlineCount }}</div>
            <div class="stat-label">在线设备</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ alarmCount }}</div>
            <div class="stat-label">报警次数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #f56c6c">
            <el-icon><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ selectedDeviceData?.charge ?? '--' }}%</div>
            <div class="stat-label">{{ selectedDeviceName || '当前电量' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>实时数据监控</span>
              <el-tag v-if="selectedDeviceName" type="primary" effect="dark">{{ selectedDeviceName }}</el-tag>
              <el-tag v-else type="info" effect="plain">请选择设备</el-tag>
            </div>
          </template>
          <div v-if="!selectedDeviceUuid" class="no-device-tip">
            <el-empty description="请点击左侧设备查看实时数据" :image-size="80" />
          </div>
          <div v-else-if="!selectedDeviceData" class="no-data-tip">
            <el-empty description="该设备暂无数据" :image-size="80" />
          </div>
          <div v-else class="data-grid">
            <div class="data-item">
              <div class="data-label">电压</div>
              <div class="data-value">{{ selectedDeviceData?.voltage?.toFixed(1) || '--' }} V</div>
            </div>
            <div class="data-item">
              <div class="data-label">电流</div>
              <div class="data-value">{{ selectedDeviceData?.current?.toFixed(1) || '--' }} A</div>
            </div>
            <div class="data-item">
              <div class="data-label">温度</div>
              <div class="data-value" :class="{ 'text-warning': selectedDeviceData?.temperature > 40 }">
                {{ selectedDeviceData?.temperature || '--' }} °C
              </div>
            </div>
            <div class="data-item">
              <div class="data-label">电量</div>
              <div class="data-value">{{ selectedDeviceData?.charge || '--' }}%</div>
            </div>
            <div class="data-item">
              <div class="data-label">功率</div>
              <div class="data-value">{{ selectedDeviceData?.power?.toFixed(1) || '--' }} W</div>
            </div>
            <div class="data-item">
              <div class="data-label">风扇</div>
              <div class="data-value">
                <el-tag :type="selectedDeviceData?.fanState ? 'success' : 'info'">
                  {{ selectedDeviceData?.fanState ? '开启' : '关闭' }}
                </el-tag>
              </div>
            </div>
            <div class="data-item">
              <div class="data-label">继电器</div>
              <div class="data-value">
                <el-tag :type="selectedDeviceData?.relayState ? 'success' : 'info'">
                  {{ selectedDeviceData?.relayState ? '开启' : '关闭' }}
                </el-tag>
              </div>
            </div>
            <div class="data-item">
              <div class="data-label">报警</div>
              <div class="data-value">
                <el-tag :type="selectedDeviceData?.alarmState ? 'danger' : 'success'">
                  {{ selectedDeviceData?.alarmState ? '报警' : '正常' }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>设备列表</span>
              <span class="device-hint">点击查看实时数据</span>
            </div>
          </template>
          <div class="device-status">
            <div
              v-for="device in devices"
              :key="device.id"
              class="device-item"
              :class="{ active: selectedDeviceUuid === device.deviceUuid }"
              @click="selectDevice(device)"
            >
              <div class="device-info">
                <div class="device-name">{{ device.deviceName || device.deviceUuid }}</div>
                <div class="device-uuid">{{ device.deviceUuid }}</div>
              </div>
              <div class="device-state">
                <el-tag :type="device.status === 1 ? 'success' : 'info'" size="small">
                  {{ device.status === 1 ? '在线' : '离线' }}
                </el-tag>
              </div>
            </div>
            <el-empty v-if="devices.length === 0" description="暂无设备" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu, CircleCheck, Warning, WarningFilled } from '@element-plus/icons-vue'
import { batteryApi } from '../api'

const router = useRouter()
const devices = ref([])
const selectedDeviceUuid = ref(null)
const selectedDeviceName = ref('')
const selectedDeviceData = ref(null)
const deviceDataMap = ref({})
let refreshTimer = null

const onlineCount = computed(() => devices.value.filter(d => d.status === 1).length)
const alarmCount = computed(() => 0)

const fetchDevices = async () => {
  try {
    devices.value = await batteryApi.getDevices()
  } catch (e) {
    console.error(e)
  }
}

const fetchAllDeviceData = async () => {
  try {
    const res = await batteryApi.getRecentAll(1)
    if (res && Array.isArray(res)) {
      const newDataMap = {}
      res.forEach(data => {
        if (data.deviceUuid) {
          newDataMap[data.deviceUuid] = data
        }
      })
      deviceDataMap.value = newDataMap
      if (selectedDeviceUuid.value && newDataMap[selectedDeviceUuid.value]) {
        selectedDeviceData.value = newDataMap[selectedDeviceUuid.value]
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const selectDevice = (device) => {
  selectedDeviceUuid.value = device.deviceUuid
  selectedDeviceName.value = device.deviceName || device.deviceUuid
  selectedDeviceData.value = deviceDataMap.value[device.deviceUuid] || null
}

const goToDevice = (uuid) => {
  router.push(`/device/${uuid}`)
}

onMounted(() => {
  fetchDevices()
  fetchAllDeviceData()
  refreshTimer = setInterval(() => {
    fetchDevices()
    fetchAllDeviceData()
  }, 2000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.dashboard {
  padding: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
}

.stat-icon .el-icon {
  font-size: 28px;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

.chart-card {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-hint {
  font-size: 12px;
  color: #999;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.data-item {
  text-align: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.data-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.data-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.data-value.text-warning {
  color: #e6a23c;
}

.no-device-tip,
.no-data-tip {
  padding: 60px 0;
  text-align: center;
}

.device-status {
  max-height: 450px;
  overflow-y: auto;
}

.device-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.device-item:hover {
  background: #ecf5ff;
  transform: translateX(4px);
}

.device-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}

.device-name {
  font-weight: 500;
  color: #333;
}

.device-uuid {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
