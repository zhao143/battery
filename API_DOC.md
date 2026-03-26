# BMS电池管理系统 - 后端API接口文档

> 用于前端开发参考

---

## 一、接口基础信息

| 项目 | 说明 |
|------|------|
| 基础URL | `http://localhost:8080` |
| 认证方式 | JWT Bearer Token |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |

---

## 二、认证接口

### 2.1 用户登录

**POST** `/api/auth/login`

请求体：
```json
{
  "username": "admin",
  "password": "password123"
}
```

成功响应（200）：
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "admin",
  "nickname": "管理员",
  "userId": 1,
  "roles": ["admin"]
}
```

失败响应（401）：
```json
{
  "message": "用户名或密码错误"
}
```

---

### 2.2 获取当前用户信息

**GET** `/api/auth/me`

请求头：
```
Authorization: Bearer <token>
```

成功响应（200）：
```json
{
  "username": "admin",
  "nickname": "管理员",
  "userId": 1,
  "roles": ["admin"]
}
```

---

## 三、电池数据接口

### 3.1 获取用户设备列表

**GET** `/api/battery/devices`

请求头：
```
Authorization: Bearer <token>
```

成功响应（200）：
```json
[
  {
    "id": 1,
    "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
    "deviceName": "用户1的设备",
    "deviceType": "BMS",
    "status": 1,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
]
```

---

### 3.2 获取所有设备最新数据

**GET** `/api/battery/latest`

请求头：
```
Authorization: Bearer <token>
```

成功响应（200）：
```json
{
  "id": 100,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "voltage": 12.5,
  "current": 2.3,
  "temperature": 25,
  "charge": 80,
  "power": 28.8,
  "fanState": 1,
  "relayState": 0,
  "alarmState": 0,
  "createdAt": "2024-01-01T12:00:00"
}
```

---

### 3.3 获取指定设备最新数据

**GET** `/api/battery/latest/{deviceUuid}`

路径参数：
| 参数 | 说明 |
|------|------|
| deviceUuid | 设备UUID |

请求头：
```
Authorization: Bearer <token>
```

成功响应（200）：
```json
{
  "id": 100,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "voltage": 12.5,
  "current": 2.3,
  "temperature": 25,
  "charge": 80,
  "power": 28.8,
  "fanState": 1,
  "relayState": 0,
  "alarmState": 0,
  "createdAt": "2024-01-01T12:00:00"
}
```

无权限响应（403）：
```json
{
  "message": "无权限访问该设备"
}
```

无数据响应（200）：
```json
{
  "message": "暂无数据"
}
```

---

### 3.4 获取所有设备历史数据

**GET** `/api/battery/recent`

查询参数：
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| limit | int | 100 | 返回数据条数 |

请求头：
```
Authorization: Bearer <token>
```

成功响应（200）：
```json
[
  {
    "id": 100,
    "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
    "voltage": 12.5,
    "current": 2.3,
    "temperature": 25,
    "charge": 80,
    "power": 28.8,
    "fanState": 1,
    "relayState": 0,
    "alarmState": 0,
    "createdAt": "2024-01-01T12:00:00"
  },
  {
    "id": 99,
    "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
    "voltage": 12.4,
    "current": 2.2,
    "temperature": 24,
    "charge": 79,
    "power": 27.3,
    "fanState": 1,
    "relayState": 0,
    "alarmState": 0,
    "createdAt": "2024-01-01T11:55:00"
  }
]
```

---

### 3.5 获取指定设备历史数据

**GET** `/api/battery/recent/{deviceUuid}`

路径参数：
| 参数 | 说明 |
|------|------|
| deviceUuid | 设备UUID |

查询参数：
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| limit | int | 100 | 返回数据条数 |

请求头：
```
Authorization: Bearer <token>
```

成功响应（200）：同3.4

无权限响应（403）：
```json
{
  "message": "无权限访问该设备"
}
```

---

### 3.6 SSE实时数据流（所有设备）

**GET** `/api/battery/stream`

请求头：
```
Authorization: Bearer <token>
```

响应：SSE流，Content-Type为 `text/event-stream`

数据格式：
```json
{
  "id": 100,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "voltage": 12.5,
  "current": 2.3,
  "temperature": 25,
  "charge": 80,
  "power": 28.8,
  "fanState": 1,
  "relayState": 0,
  "alarmState": 0,
  "createdAt": "2024-01-01T12:00:00"
}
```

---

### 3.7 SSE实时数据流（指定设备）

**GET** `/api/battery/stream/{deviceUuid}`

路径参数：
| 参数 | 说明 |
|------|------|
| deviceUuid | 设备UUID |

请求头：
```
Authorization: Bearer <token>
```

响应：同3.6

无权限响应（403）：
```json
{
  "message": "无权限访问该设备"
}
```

---

## 四、管理接口（仅admin）

### 4.1 获取所有用户

**GET** `/api/admin/users`

请求头：
```
Authorization: Bearer <admin_token>
```

成功响应（200）：
```json
[
  {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "email": "admin@bms.com",
    "phone": null,
    "status": 1,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  },
  {
    "id": 2,
    "username": "user1",
    "nickname": "用户1",
    "email": "user1@bms.com",
    "phone": null,
    "status": 1,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
]
```

无权限响应（403）：
```json
{
  "message": "无权限"
}
```

---

### 4.2 创建用户

**POST** `/api/admin/users`

请求头：
```
Authorization: Bearer <admin_token>
```

请求体：
```json
{
  "username": "newuser",
  "password": "password123",
  "nickname": "新用户",
  "email": "newuser@bms.com",
  "phone": "13800138000"
}
```

成功响应（200）：
```json
{
  "message": "用户创建成功",
  "userId": 4
}
```

用户名已存在响应（400）：
```json
{
  "message": "用户名已存在"
}
```

---

### 4.3 更新用户

**PUT** `/api/admin/users/{id}`

路径参数：
| 参数 | 说明 |
|------|------|
| id | 用户ID |

请求头：
```
Authorization: Bearer <admin_token>
```

请求体：
```json
{
  "nickname": "新昵称",
  "email": "newemail@bms.com",
  "phone": "13900139000",
  "status": 1
}
```

成功响应（200）：
```json
{
  "message": "用户更新成功"
}
```

---

### 4.4 删除用户

**DELETE** `/api/admin/users/{id}`

路径参数：
| 参数 | 说明 |
|------|------|
| id | 用户ID |

请求头：
```
Authorization: Bearer <admin_token>
```

成功响应（200）：
```json
{
  "message": "用户删除成功"
}
```

---

### 4.5 获取所有设备

**GET** `/api/admin/devices`

请求头：
```
Authorization: Bearer <admin_token>
```

成功响应（200）：
```json
[
  {
    "id": 1,
    "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
    "deviceName": "用户1的设备",
    "deviceType": "BMS",
    "status": 1,
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:00:00"
  }
]
```

---

### 4.6 创建设备

**POST** `/api/admin/devices`

请求头：
```
Authorization: Bearer <admin_token>
```

请求体：
```json
{
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "用户1的设备",
  "deviceType": "BMS"
}
```

成功响应（200）：
```json
{
  "message": "设备创建成功",
  "deviceId": 1
}
```

---

### 4.7 更新设备

**PUT** `/api/admin/devices/{id}`

路径参数：
| 参数 | 说明 |
|------|------|
| id | 设备ID |

请求头：
```
Authorization: Bearer <admin_token>
```

请求体：
```json
{
  "deviceName": "新设备名",
  "deviceType": "BMS"
}
```

成功响应（200）：
```json
{
  "message": "设备更新成功"
}
```

---

### 4.8 删除设备

**DELETE** `/api/admin/devices/{id}`

路径参数：
| 参数 | 说明 |
|------|------|
| id | 设备ID |

请求头：
```
Authorization: Bearer <admin_token>
```

成功响应（200）：
```json
{
  "message": "设备删除成功"
}
```

---

### 4.9 绑定设备到用户

**POST** `/api/admin/devices/{deviceId}/bind/{userId}`

路径参数：
| 参数 | 说明 |
|------|------|
| deviceId | 设备ID |
| userId | 用户ID |

请求头：
```
Authorization: Bearer <admin_token>
```

成功响应（200）：
```json
{
  "message": "设备绑定成功"
}
```

---

### 4.10 解绑设备

**POST** `/api/admin/devices/{deviceId}/unbind/{userId}`

路径参数：
| 参数 | 说明 |
|------|------|
| deviceId | 设备ID |
| userId | 用户ID |

请求头：
```
Authorization: Bearer <admin_token>
```

成功响应（200）：
```json
{
  "message": "设备解绑成功"
}
```

---

## 五、数据模型

### 5.1 电池数据 (BatteryData)

```json
{
  "id": 100,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "voltage": 12.5,
  "current": 2.3,
  "temperature": 25,
  "charge": 80,
  "power": 28.8,
  "fanState": 1,
  "relayState": 0,
  "alarmState": 0,
  "createdAt": "2024-01-01T12:00:00"
}
```

字段说明：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 数据ID |
| deviceUuid | String | 设备UUID |
| voltage | Double | 电压 (V) |
| current | Double | 电流 (A) |
| temperature | Integer | 温度 (°C) |
| charge | Integer | 电量 (%) |
| power | Double | 功率 (W) |
| fanState | Integer | 风扇状态：0=关，1=开 |
| relayState | Integer | 继电器状态：0=关，1=开 |
| alarmState | Integer | 报警状态：0=正常，1=报警 |
| createdAt | DateTime | 创建时间 |

---

### 5.2 设备 (Device)

```json
{
  "id": 1,
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "用户1的设备",
  "deviceType": "BMS",
  "status": 1,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

字段说明：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 设备ID |
| deviceUuid | String | 设备UUID |
| deviceName | String | 设备名称 |
| deviceType | String | 设备类型 |
| status | Integer | 在线状态：0=离线，1=在线 |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

---

### 5.3 用户 (SysUser)

```json
{
  "id": 1,
  "username": "admin",
  "nickname": "管理员",
  "email": "admin@bms.com",
  "phone": null,
  "status": 1,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

字段说明：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |
| username | String | 用户名（登录账号） |
| nickname | String | 昵称 |
| email | String | 邮箱 |
| phone | String | 手机号 |
| status | Integer | 状态：0=禁用，1=启用 |
| createdAt | DateTime | 创建时间 |
| updatedAt | DateTime | 更新时间 |

---

## 六、前端开发注意事项

### 6.1 认证流程
1. 用户登录后保存token到本地存储（如localStorage）
2. 每次请求在请求头携带：`Authorization: Bearer <token>`
3. token过期或无效时，后端返回401，前端需跳转登录页

### 6.2 权限控制
- `admin`用户：可访问所有API
- 普通用户：只能访问自己的设备数据

### 6.3 实时数据
- 使用SSE（Server-Sent Events）获取实时数据
- 使用EventSource连接 `/api/battery/stream` 或 `/api/battery/stream/{deviceUuid}`

### 6.4 错误处理
| 状态码 | 说明 | 处理方式 |
|--------|------|----------|
| 200 | 成功 | 正常处理响应数据 |
| 400 | 请求参数错误 | 提示用户检查输入 |
| 401 | 未登录或token无效 | 跳转登录页 |
| 403 | 无权限 | 提示用户权限不足 |
| 404 | 资源不存在 | 提示资源不存在 |
| 500 | 服务器错误 | 提示系统错误 |

### 6.5 接口前缀
所有API接口都有前缀 `/api`，例如：
- `/api/auth/login`
- `/api/battery/devices`
- `/api/admin/users`
