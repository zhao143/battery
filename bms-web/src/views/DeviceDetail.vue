<template>
  <div class="device-detail">
    <el-button @click="$router.back()" style="margin-bottom: 16px">
      <el-icon><ArrowLeft /></el-icon>返回
    </el-button>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="data-card">
          <template #header>
            <div class="card-header">
              <span>实时数据</span>
              <el-tag type="success" effect="dark">实时</el-tag>
            </div>
          </template>
          <div class="data-grid">
            <div class="data-item">
              <div class="data-icon" style="background: #409eff">
                <el-icon><Lightning /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">电压</div>
                <div class="data-value">{{ latestData?.voltage?.toFixed(1) || '--' }} V</div>
              </div>
            </div>
            <div class="data-item">
              <div class="data-icon" style="background: #67c23a">
                <el-icon><Cpu /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">电流</div>
                <div class="data-value">{{ latestData?.current?.toFixed(1) || '--' }} A</div>
              </div>
            </div>
            <div class="data-item">
              <div class="data-icon" style="background: #e6a23c">
                <el-icon><Odometer /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">温度</div>
                <div class="data-value" :class="{ 'text-warning': latestData?.temperature > 40 }">
                  {{ latestData?.temperature || '--' }} °C
                </div>
              </div>
            </div>
            <div class="data-item">
              <div class="data-icon" style="background: #909399">
                <el-icon><Grid /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">电量</div>
                <div class="data-value">{{ latestData?.charge || '--' }}%</div>
              </div>
            </div>
            <div class="data-item">
              <div class="data-icon" style="background: #f56c6c">
                <el-icon><TrendCharts /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">功率</div>
                <div class="data-value">{{ latestData?.power?.toFixed(1) || '--' }} W</div>
              </div>
            </div>
            <div class="data-item">
              <div class="data-icon" style="background: #c71585">
                <el-icon><SwitchButton /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">风扇</div>
                <div class="data-value">
                  <el-switch 
                    v-model="fanState" 
                    :loading="controlLoading"
                    @change="handleFanChange"
                    active-text="开" 
                    inactive-text="关" 
                  />
                </div>
              </div>
            </div>
            <div class="data-item">
              <div class="data-icon" style="background: #00bcd4">
                <el-icon><Switch /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">继电器</div>
                <div class="data-value">
                  <el-switch 
                    v-model="relayState" 
                    :loading="controlLoading"
                    @change="handleRelayChange"
                    active-text="开" 
                    inactive-text="关" 
                  />
                </div>
              </div>
            </div>
            <div class="data-item">
              <div class="data-icon" style="background: #ff6b6b">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="data-info">
                <div class="data-label">报警</div>
                <div class="data-value">
                  <el-tag :type="latestData?.alarmState ? 'danger' : 'success'">
                    {{ latestData?.alarmState ? '报警' : '正常' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="info-card">
          <template #header>
            <span>设备信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="设备UUID">{{ uuid }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="deviceInfo?.status === 1 ? 'success' : 'info'">
                {{ deviceInfo?.status === 1 ? '在线' : '离线' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="设备名称">{{ deviceInfo?.deviceName || '--' }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">{{ deviceInfo?.deviceType || '--' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(deviceInfo?.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>历史数据曲线</span>
          <el-select v-model="limit" size="small" style="width: 120px" @change="fetchHistory">
            <el-option label="最近50条" :value="50" />
            <el-option label="最近100条" :value="100" />
            <el-option label="最近200条" :value="200" />
          </el-select>
        </div>
      </template>
      <div ref="chartRef" style="width: 100%; height: 400px"></div>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>历史数据列表</span>
          <el-select v-model="limit" size="small" style="width: 120px" @change="fetchHistory">
            <el-option label="最近50条" :value="50" />
            <el-option label="最近100条" :value="100" />
            <el-option label="最近200条" :value="200" />
          </el-select>
        </div>
      </template>
      <el-table :data="historyData" stripe>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="voltage" label="电压(V)" width="100" />
        <el-table-column prop="current" label="电流(A)" width="100" />
        <el-table-column prop="temperature" label="温度(°C)" width="100" />
        <el-table-column prop="charge" label="电量(%)" width="100" />
        <el-table-column prop="power" label="功率(W)" width="100" />
        <el-table-column prop="fanState" label="风扇" width="80">
          <template #default="{ row }">
            <el-tag :type="row.fanState ? 'success' : 'info'" size="small">
              {{ row.fanState ? '开' : '关' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="relayState" label="继电器" width="80">
          <template #default="{ row }">
            <el-tag :type="row.relayState ? 'success' : 'info'" size="small">
              {{ row.relayState ? '开' : '关' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="alarmState" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.alarmState ? 'danger' : 'success'" size="small">
              {{ row.alarmState ? '报警' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { 
  ArrowLeft, Lightning, Cpu, Odometer, 
  TrendCharts, SwitchButton, Warning, Grid
} from '@element-plus/icons-vue'
import { batteryApi, controlApi } from '../api'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const route = useRoute()
const uuid = route.params.uuid
const latestData = ref(null)
const historyData = ref([])
const deviceInfo = ref(null)
const limit = ref(50)
const controlLoading = ref(false)
const fanState = ref(false)
const relayState = ref(false)
const chartRef = ref(null)
let chartInstance = null
let eventSource = null
let refreshTimer = null

const fetchLatest = async () => {
  try {
    const res = await batteryApi.getLatestByUuid(uuid)
    if (res && !res.message) {
      latestData.value = res
      fanState.value = res.fanState === 1
      relayState.value = res.relayState === 1
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchHistory = async () => {
  try {
    historyData.value = await batteryApi.getRecent(uuid, limit.value)
    await nextTick()
    updateChart()
  } catch (e) {
    console.error(e)
  }
}

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        let result = params[0].name + '<br/>'
        params.forEach(p => {
          const unit = p.seriesName === '温度' ? '°C' : p.seriesName === '电压' ? 'V' : p.seriesName === '电流' ? 'A' : '%'
          result += `${p.seriesName}: ${p.value}${unit}<br/>`
        })
        return result
      }
    },
    legend: {
      data: ['电压', '电流', '温度', '电量'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: [],
      boundaryGap: false
    },
    yAxis: [
      {
        type: 'value',
        name: '电压/电流/温度',
        position: 'left',
        min: 0,
        max: 100,
        axisLabel: {
          formatter: '{value}'
        }
      },
      {
        type: 'value',
        name: '电量',
        position: 'right',
        min: 0,
        max: 100,
        axisLabel: {
          formatter: '{value}%'
        }
      }
    ],
    series: [
      {
        name: '电压',
        type: 'line',
        data: [],
        smooth: true,
        itemStyle: { color: '#409eff' }
      },
      {
        name: '电流',
        type: 'line',
        data: [],
        smooth: true,
        yAxisIndex: 0,
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '温度',
        type: 'line',
        data: [],
        smooth: true,
        yAxisIndex: 0,
        itemStyle: { color: '#e6a23c' }
      },
      {
        name: '电量',
        type: 'line',
        data: [],
        smooth: true,
        yAxisIndex: 1,
        itemStyle: { color: '#909399' }
      }
    ]
  }
  chartInstance.setOption(option)
}

const updateChart = () => {
  if (!chartInstance || historyData.value.length === 0) return
  
  const times = historyData.value.map(d => {
    const date = new Date(d.createdAt)
    return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`
  }).reverse()
  
  const voltages = historyData.value.map(d => d.voltage).reverse()
  const currents = historyData.value.map(d => d.current).reverse()
  const temperatures = historyData.value.map(d => d.temperature).reverse()
  const charges = historyData.value.map(d => d.charge).reverse()
  
  chartInstance.setOption({
    xAxis: { data: times },
    series: [
      { data: voltages },
      { data: currents },
      { data: temperatures },
      { data: charges }
    ]
  })
}

const fetchDeviceInfo = async () => {
  try {
    const devices = await batteryApi.getDevices()
    deviceInfo.value = devices.find(d => d.deviceUuid === uuid)
  } catch (e) {
    console.error(e)
  }
}

const connectSSE = () => {
  eventSource = new EventSource(`/api/battery/stream/${uuid}`)
  eventSource.onmessage = (event) => {
    try {
      latestData.value = JSON.parse(event.data)
      fanState.value = latestData.value.fanState === 1
      relayState.value = latestData.value.relayState === 1
    } catch (e) {
      console.error(e)
    }
  }
  eventSource.onerror = () => {
    eventSource?.close()
    setTimeout(connectSSE, 5000)
  }
}

const handleFanChange = async (val) => {
  controlLoading.value = true
  try {
    await controlApi.setControl(val ? 1 : 0, relayState.value ? 1 : 0)
    ElMessage.success(`风扇已${val ? '开启' : '关闭'}`)
  } catch (e) {
    ElMessage.error('控制失败')
    fanState.value = !val
  } finally {
    controlLoading.value = false
  }
}

const handleRelayChange = async (val) => {
  controlLoading.value = true
  try {
    await controlApi.setControl(fanState.value ? 1 : 0, val ? 1 : 0)
    ElMessage.success(`继电器已${val ? '开启' : '关闭'}`)
  } catch (e) {
    ElMessage.error('控制失败')
    relayState.value = !val
  } finally {
    controlLoading.value = false
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchLatest()
  fetchHistory()
  fetchDeviceInfo()
  connectSSE()
  initChart()
  refreshTimer = setInterval(() => {
    fetchLatest()
    fetchHistory()
  }, 2000)
})

onUnmounted(() => {
  eventSource?.close()
  if (refreshTimer) clearInterval(refreshTimer)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.device-detail {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.data-card {
  margin-bottom: 20px;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.data-item {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.data-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.data-icon .el-icon {
  font-size: 24px;
  color: #fff;
}

.data-info {
  flex: 1;
}

.data-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.data-value {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.data-value.text-warning {
  color: #e6a23c;
}

.info-card {
  height: 100%;
}
</style>
