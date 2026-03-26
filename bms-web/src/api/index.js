import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  console.log('Token from localStorage:', token ? 'exists' : 'null', 'URL:', config.url)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  console.log('API Request:', config.method?.toUpperCase(), config.url)
  return config
})

api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  me: () => api.get('/auth/me')
}

export const batteryApi = {
  getDevices: () => api.get('/battery/devices'),
  createDevice: (data) => api.post('/battery/devices', data),
  bindDevice: (deviceUuid) => api.post('/battery/devices/bind', { deviceUuid }),
  getLatest: () => api.get('/battery/latest'),
  getLatestByUuid: (uuid) => api.get(`/battery/latest/${uuid}`),
  getRecent: (uuid, limit) => api.get(`/battery/recent/${uuid}?limit=${limit || 100}`),
  getRecentAll: (limit) => api.get(`/battery/recent?limit=${limit || 100}`),
}

export const controlApi = {
  setControl: (fan, relay) => api.post('/control', { fan, relay }),
  sendCmd: (cmd) => api.post('/tcp/send', { cmd }),
}

export const thresholdApi = {
  get: (deviceUuid) => api.get('/threshold', { params: { deviceUuid } }),
  setVMax: (deviceUuid, value) => api.post('/threshold/vmax', { deviceUuid, value }),
  setVMin: (deviceUuid, value) => api.post('/threshold/vmin', { deviceUuid, value }),
  setIMax: (deviceUuid, value) => api.post('/threshold/imax', { deviceUuid, value }),
  setTMax: (deviceUuid, value) => api.post('/threshold/tmax', { deviceUuid, value }),
  saveAll: (deviceUuid, vMax, vMin, iMax, tMax) => api.post('/threshold/saveAll', { deviceUuid, vMax, vMin, iMax, tMax }),
}

export const adminApi = {
  getUsers: () => api.get('/admin/users'),
  createUser: (data) => api.post('/admin/users', data),
  updateUser: (id, data) => api.put(`/admin/users/${id}`, data),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
  getDevices: () => api.get('/admin/devices'),
  createDevice: (data) => api.post('/admin/devices', data),
  updateDevice: (id, data) => api.put(`/admin/devices/${id}`, data),
  deleteDevice: (id) => api.delete(`/admin/devices/${id}`),
  bindDevice: (deviceId, userId) => api.post(`/admin/devices/${deviceId}/bind/${userId}`),
  unbindDevice: (deviceId, userId) => api.post(`/admin/devices/${deviceId}/unbind/${userId}`)
}

export default api
