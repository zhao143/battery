<template>
  <div class="admin-devices">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <el-button type="primary" @click="showAddDialog">
            <el-icon><Plus /></el-icon>新增设备
          </el-button>
        </div>
      </template>
      <div v-if="devices.length === 0" style="text-align: center; padding: 40px; color: #999;">
        暂无设备数据
      </div>
      <el-table v-else :data="devices" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="deviceUuid" label="设备UUID" min-width="280">
          <template #default="{ row }">
            <div class="uuid-cell">
              <span class="uuid-text">{{ row.deviceUuid }}</span>
              <el-button type="primary" link size="small" @click="copyUuid(row.deviceUuid)">复制</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="deviceType" label="设备类型" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定用户" min-width="150">
          <template #default="{ row }">
            <div v-if="row.boundUsers && row.boundUsers.length > 0" class="bound-users">
              <el-tag v-for="userId in row.boundUsers" :key="userId" size="small" closable @close="handleUnbind(row, userId)">
                {{ getUserName(userId) }}
              </el-tag>
            </div>
            <span v-else class="no-user">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="showBindDialog(row)">绑定用户</el-button>
            <el-button type="primary" link @click="showEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑设备' : '新增设备'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item v-if="!isEdit" label="设备UUID" prop="deviceUuid">
          <el-input v-model="form.deviceUuid" placeholder="留空自动生成UUID" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="form.deviceType" placeholder="请选择设备类型" style="width: 100%">
            <el-option label="BMS" value="BMS" />
            <el-option label="锂电池" value="锂电池" />
            <el-option label="铅酸电池" value="铅酸电池" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindDialogVisible" title="绑定用户到设备" width="450px">
      <div v-if="currentDevice" class="bind-info">
        <p><strong>设备：</strong>{{ currentDevice.deviceName || currentDevice.deviceUuid }}</p>
        <p class="uuid-tip">UUID: {{ currentDevice.deviceUuid }}</p>
      </div>
      <el-divider />
      <p class="bind-title">选择要绑定到此设备的用户：</p>
      <el-select v-model="selectedUserId" placeholder="请选择用户" style="width: 100%" clearable>
        <el-option
          v-for="user in availableUsers"
          :key="user.id"
          :label="`${user.nickname || user.username} (${user.username})`"
          :value="user.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBind" :disabled="!selectedUserId">绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminApi } from '../../api'

const devices = ref([])
const users = ref([])
const dialogVisible = ref(false)
const bindDialogVisible = ref(false)
const isEdit = ref(false)
const currentDevice = ref(null)
const selectedUserId = ref(null)
let statusTimer = null
const formRef = ref()
const form = reactive({
  id: null,
  deviceUuid: '',
  deviceName: '',
  deviceType: 'BMS'
})

const rules = {
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

const availableUsers = computed(() => {
  if (!currentDevice.value) return users.value
  return users.value.filter(u => {
    const bound = currentDevice.value.boundUsers || []
    return !bound.includes(u.id)
  })
})

const getUserName = (userId) => {
  const user = users.value.find(u => u.id === userId)
  return user ? (user.nickname || user.username) : `用户${userId}`
}

const copyUuid = (uuid) => {
  navigator.clipboard.writeText(uuid)
  ElMessage.success('UUID已复制到剪贴板')
}

const fetchDevices = async () => {
  try {
    const res = await adminApi.getDevices()
    devices.value = res
  } catch (e) {
    console.error('Get devices error:', e)
    ElMessage.error('获取设备列表失败')
  }
}

const fetchUsers = async () => {
  try {
    users.value = await adminApi.getUsers()
  } catch (e) {
    console.error(e)
  }
}

const showAddDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, deviceUuid: '', deviceName: '', deviceType: 'BMS' })
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const showBindDialog = (row) => {
  currentDevice.value = row
  selectedUserId.value = null
  bindDialogVisible.value = true
}

const handleBind = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请选择要绑定的用户')
    return
  }
  try {
    await adminApi.bindDevice(currentDevice.value.id, selectedUserId.value)
    ElMessage.success('绑定成功')
    bindDialogVisible.value = false
    fetchDevices()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '绑定失败')
  }
}

const handleUnbind = async (row, userId) => {
  try {
    await ElMessageBox.confirm('确定要解除此绑定吗？', '提示', { type: 'warning' })
    await adminApi.unbindDevice(row.id, userId)
    ElMessage.success('解绑成功')
    fetchDevices()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '解绑失败')
    }
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (isEdit.value) {
      await adminApi.updateDevice(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await adminApi.createDevice(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchDevices()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除设备 "${row.deviceName || row.deviceUuid}" 吗？`, '提示', { type: 'warning' })
    await adminApi.deleteDevice(row.id)
    ElMessage.success('删除成功')
    fetchDevices()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchDevices()
  fetchUsers()
  statusTimer = setInterval(fetchDevices, 5000)
})

onUnmounted(() => {
  if (statusTimer) {
    clearInterval(statusTimer)
  }
})
</script>

<style scoped>
.admin-devices {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.uuid-text {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.uuid-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bind-info {
  margin-bottom: 16px;
}

.bind-info p {
  margin: 8px 0;
}

.uuid-tip {
  font-family: monospace;
  font-size: 12px;
  color: #666;
}

.bind-title {
  margin-bottom: 16px;
  color: #333;
}

.bound-users {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.no-user {
  color: #999;
  font-size: 13px;
}
</style>
