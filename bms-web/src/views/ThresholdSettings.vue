<template>
  <div class="threshold-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>阈值设置</span>
          <el-select v-model="selectedDevice" placeholder="选择设备" @change="onDeviceChange" style="width: 300px">
            <el-option v-for="device in devices" :key="device.id" :label="device.deviceName || device.deviceUuid" :value="device.deviceUuid" />
          </el-select>
        </div>
      </template>

      <div v-if="!selectedDevice" class="empty-tip">
        <el-empty description="请选择设备以查看和修改阈值设置" />
      </div>

      <div v-else class="threshold-form">
        <el-form :model="form" label-width="120px" class="threshold-settings">
          <el-form-item label="最高电压 (V)">
            <el-input-number v-model="form.vMax" :precision="1" :step="0.1" :min="0" style="width: 200px" />
            <el-button type="primary" @click="setThreshold('vmax')" :loading="loading" style="margin-left: 10px">设置</el-button>
          </el-form-item>

          <el-form-item label="最低电压 (V)">
            <el-input-number v-model="form.vMin" :precision="1" :step="0.1" :min="0" style="width: 200px" />
            <el-button type="primary" @click="setThreshold('vmin')" :loading="loading" style="margin-left: 10px">设置</el-button>
          </el-form-item>

          <el-form-item label="最大电流 (A)">
            <el-input-number v-model="form.iMax" :precision="1" :step="0.1" :min="0" style="width: 200px" />
            <el-button type="primary" @click="setThreshold('imax')" :loading="loading" style="margin-left: 10px">设置</el-button>
          </el-form-item>

          <el-form-item label="最高温度 (°C)">
            <el-input-number v-model="form.tMax" :step="1" :min="0" style="width: 200px" />
            <el-button type="primary" @click="setThreshold('tmax')" :loading="loading" style="margin-left: 10px">设置</el-button>
          </el-form-item>

          <el-form-item>
            <el-button type="success" @click="setAllThresholds" :loading="loadingAll" size="large">
              一键保存全部
            </el-button>
          </el-form-item>
        </el-form>

        <el-divider />

        <div class="threshold-info">
          <h4>当前阈值设置</h4>
          <el-descriptions :column="2" border v-if="currentThreshold && (currentThreshold.vmax !== null || currentThreshold.vmin !== null || currentThreshold.imax !== null || currentThreshold.tmax !== null)">
            <el-descriptions-item label="最高电压">{{ currentThreshold.vmax !== null && currentThreshold.vmax !== undefined ? currentThreshold.vmax : '未设置' }} V</el-descriptions-item>
            <el-descriptions-item label="最低电压">{{ currentThreshold.vmin !== null && currentThreshold.vmin !== undefined ? currentThreshold.vmin : '未设置' }} V</el-descriptions-item>
            <el-descriptions-item label="最大电流">{{ currentThreshold.imax !== null && currentThreshold.imax !== undefined ? currentThreshold.imax : '未设置' }} A</el-descriptions-item>
            <el-descriptions-item label="最高温度">{{ currentThreshold.tmax !== null && currentThreshold.tmax !== undefined ? currentThreshold.tmax : '未设置' }} °C</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="暂无阈值设置" :image-size="60" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { batteryApi, thresholdApi } from '../api'

const devices = ref([])
const selectedDevice = ref('')
const loading = ref(false)
const loadingAll = ref(false)
const currentThreshold = ref(null)

const form = reactive({
  vMax: 0,
  vMin: 0,
  iMax: 0,
  tMax: 0
})

const fetchDevices = async () => {
  try {
    devices.value = await batteryApi.getDevices()
    if (devices.value.length > 0 && !selectedDevice.value) {
      selectedDevice.value = devices.value[0].deviceUuid
      await fetchThreshold()
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchThreshold = async () => {
  if (!selectedDevice.value) return
  try {
    const res = await thresholdApi.get(selectedDevice.value)
    console.log('Threshold response:', res)
    currentThreshold.value = res
    if (res && (res.vmax !== undefined || res.vmin !== undefined || res.imax !== undefined || res.tmax !== undefined)) {
      form.vMax = res.vmax !== undefined && res.vmax !== null ? res.vmax : 0
      form.vMin = res.vmin !== undefined && res.vmin !== null ? res.vmin : 0
      form.iMax = res.imax !== undefined && res.imax !== null ? res.imax : 0
      form.tMax = res.tmax !== undefined && res.tmax !== null ? res.tmax : 0
    } else {
      form.vMax = 0
      form.vMin = 0
      form.iMax = 0
      form.tMax = 0
    }
  } catch (e) {
    console.error(e)
  }
}

const onDeviceChange = () => {
  fetchThreshold()
}

const setThreshold = async (type) => {
  loading.value = true
  try {
    let value, apiMethod
    switch (type) {
      case 'vmax':
        value = form.vMax
        apiMethod = thresholdApi.setVMax
        break
      case 'vmin':
        value = form.vMin
        apiMethod = thresholdApi.setVMin
        break
      case 'imax':
        value = form.iMax
        apiMethod = thresholdApi.setIMax
        break
      case 'tmax':
        value = form.tMax
        apiMethod = thresholdApi.setTMax
        break
    }
    await apiMethod(selectedDevice.value, value)
    ElMessage.success('阈值设置成功')
    await fetchThreshold()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '设置失败')
  } finally {
    loading.value = false
  }
}

const setAllThresholds = async () => {
  loadingAll.value = true
  try {
    await thresholdApi.saveAll(
      selectedDevice.value,
      form.vMax,
      form.vMin,
      form.iMax,
      form.tMax
    )
    ElMessage.success('全部阈值设置成功')
    await fetchThreshold()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '设置失败')
  } finally {
    loadingAll.value = false
  }
}

onMounted(() => {
  fetchDevices()
})
</script>

<style scoped>
.threshold-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-tip {
  padding: 60px 0;
}

.threshold-form {
  max-width: 600px;
}

.threshold-settings {
  margin-top: 20px;
}

.threshold-info {
  margin-top: 20px;
}

.threshold-info h4 {
  margin-bottom: 16px;
  color: #333;
}
</style>
