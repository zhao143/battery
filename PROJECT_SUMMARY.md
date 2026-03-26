# BMS电池管理系统 - 项目修改总结

## 一、数据库修改

### 1. 数据库名称

- 原数据库名：`bms`
- 新数据库名：`bms_uuid`

### 2. 新增10个表

```sql
-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_key VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL
);

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(100) NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

-- 设备表
CREATE TABLE device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_uuid VARCHAR(36) NOT NULL UNIQUE,
    device_name VARCHAR(100),
    device_type VARCHAR(50),
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 用户设备关联表
CREATE TABLE user_device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_device (user_id, device_id)
);

-- 电池数据表（添加device_uuid）
CREATE TABLE battery_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_uuid VARCHAR(36) NOT NULL,
    voltage DOUBLE NOT NULL,
    current DOUBLE NOT NULL,
    temperature INT NOT NULL,
    charge INT NOT NULL,
    power DOUBLE NOT NULL,
    fan_state INT NOT NULL,
    relay_state INT NOT NULL,
    alarm_state INT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_device_uuid (device_uuid),
    INDEX idx_created_at (created_at)
);

-- 阈值设置表
CREATE TABLE threshold_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_uuid VARCHAR(36),
    v_max DOUBLE,
    v_min DOUBLE,
    i_max DOUBLE,
    t_max INT,
    updated_at DATETIME NOT NULL,
    INDEX idx_device_uuid (device_uuid)
);

-- 报警历史表（添加device_uuid）
CREATE TABLE alarm_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    device_uuid VARCHAR(36) NOT NULL,
    voltage DOUBLE NOT NULL,
    current DOUBLE NOT NULL,
    temperature INT NOT NULL,
    charge INT NOT NULL,
    power DOUBLE NOT NULL,
    fan_state INT NOT NULL,
    relay_state INT NOT NULL,
    alarm_state INT NOT NULL,
    process_status INT NOT NULL DEFAULT 0,
    v_max DOUBLE,
    v_min DOUBLE,
    i_max DOUBLE,
    t_max INT,
    created_at DATETIME NOT NULL,
    INDEX idx_device_uuid (device_uuid),
    INDEX idx_created_at (created_at)
);
```

### 3. 预置数据

```sql
-- 预置用户（密码均为 password123 的BCrypt加密）
INSERT INTO sys_user (username, password, nickname, email, status, created_at, updated_at) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@bms.com', 1, NOW(), NOW()),
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '用户1', 'user1@bms.com', 1, NOW(), NOW()),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '用户2', 'user2@bms.com', 1, NOW(), NOW());

-- 预置角色
INSERT INTO sys_role (role_name, role_key, description, created_at) VALUES
('管理员', 'admin', '系统管理员', NOW()),
('普通用户', 'user', '普通用户', NOW());

-- 预置权限
INSERT INTO sys_permission (permission_name, permission_key, description) VALUES
('查看所有用户', 'user:list', '查看所有用户'),
('管理用户', 'user:manage', '管理用户'),
('查看所有设备', 'device:list', '查看所有设备'),
('查看自己设备', 'device:self', '查看自己的设备'),
('查看数据', 'data:view', '查看电池数据'),
('管理设备', 'device:manage', '管理设备');

-- 用户角色绑定
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin -> 管理员
(2, 2),  -- user1 -> 普通用户
(3, 2);  -- user2 -> 普通用户

-- 角色权限绑定
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),  -- 管理员拥有所有权限
(2, 4), (2, 5);  -- 普通用户只能查看自己设备和数据
```

***

## 二、代码修改总结

### 1. pom.xml - 新增依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

### 2. application.yml - 新增配置

```yaml
jwt:
  secret: bmsSecretKey2024VeryLongSecretKeyForHS256Algorithm
  expiration: 86400000  # 24小时
```

### 3. 新增文件清单

| 文件路径                                                                  | 说明                |
| --------------------------------------------------------------------- | ----------------- |
| `src/main/java/com/example/bms/util/JwtUtil.java`                     | JWT工具类            |
| `src/main/java/com/example/bms/config/SecurityConfig.java`            | Spring Security配置 |
| `src/main/java/com/example/bms/security/JwtAuthenticationFilter.java` | JWT认证过滤器          |
| `src/main/java/com/example/bms/security/JwtUserDetails.java`          | 用户详情类             |
| `src/main/java/com/example/bms/domain/SysUser.java`                   | 用户实体              |
| `src/main/java/com/example/bms/domain/SysRole.java`                   | 角色实体              |
| `src/main/java/com/example/bms/domain/SysPermission.java`             | 权限实体              |
| `src/main/java/com/example/bms/domain/Device.java`                    | 设备实体              |
| `src/main/java/com/example/bms/domain/UserDevice.java`                | 用户设备关联实体          |
| `src/main/java/com/example/bms/mapper/SysUserMapper.java`             | 用户Mapper接口        |
| `src/main/java/com/example/bms/mapper/SysRoleMapper.java`             | 角色Mapper接口        |
| `src/main/java/com/example/bms/mapper/DeviceMapper.java`              | 设备Mapper接口        |
| `src/main/java/com/example/bms/mapper/UserDeviceMapper.java`          | 用户设备Mapper接口      |
| `src/main/resources/mapper/SysUserMapper.xml`                         | 用户SQL映射           |
| `src/main/resources/mapper/SysRoleMapper.xml`                         | 角色SQL映射           |
| `src/main/resources/mapper/DeviceMapper.xml`                          | 设备SQL映射           |
| `src/main/resources/mapper/UserDeviceMapper.xml`                      | 用户设备SQL映射         |
| `src/main/java/com/example/bms/service/DeviceService.java`            | 设备服务层             |
| `src/main/java/com/example/bms/controller/AuthController.java`        | 认证API             |
| `src/main/java/com/example/bms/controller/AdminController.java`       | 管理员API            |

### 4. 修改的文件清单

| 文件路径                                                              | 修改内容                       |
| ----------------------------------------------------------------- | -------------------------- |
| `src/main/java/com/example/bms/domain/BatteryData.java`           | 添加deviceUuid字段             |
| `src/main/java/com/example/bms/domain/AlarmHistory.java`          | 添加deviceUuid字段             |
| `src/main/java/com/example/bms/mapper/BatteryDataMapper.java`     | 添加UUID查询方法                 |
| `src/main/resources/mapper/BatteryDataMapper.xml`                 | 添加device\_uuid字段和UUID查询SQL |
| `src/main/resources/mapper/AlarmHistoryMapper.xml`                | 添加device\_uuid字段           |
| `src/main/java/com/example/bms/service/BatteryService.java`       | 添加UUID相关方法                 |
| `src/main/java/com/example/bms/service/AlarmHistoryService.java`  | 添加deviceUuid保存             |
| `src/main/java/com/example/bms/controller/BatteryController.java` | 重写，支持用户设备关联                |
| `src/main/java/com/example/bms/tcp/TcpServer.java`                | 支持UUID格式和设备注册              |
| `src/main/resources/application.yml`                              | 修改数据库名，添加JWT配置             |
| `src/main/resources/schema.sql`                                   | 修改数据库名，完整建表SQL             |

***

## 三、TCP服务器协议

### 1. 设备注册

设备连接后首先发送注册消息：

REGISTER#12345678-1234-1234-1234-123456789ABC

服务器响应：

- 成功：`ACK#REGISTERED#12345678-1234-1234-1234-123456789ABC`
- 失败：`ERR#UNKNOWN_DEVICE`

### 2. 数据上报格式

```
UUID#电压#电流#温度#电量#功率#风扇#继电器#报警\r\n
```

示例：

```
12345678-1234-1234-1234-123456789ABC#12.5#2.3#25#80#28.8#1#0#0
```

### 3. 服务器控制命令

服务器向设备发送：

```
CTRL#风扇状态#继电器状态
```

示例：`CTRL#1#0` 表示开启风扇，关闭继电器

***

## 四、API接口文档

### 1. 认证接口

#### POST /api/auth/login - 用户登录

请求：

```json
{
  "username": "admin",
  "password": "password123"
}
```

响应：

```json
{
  "token": "eyJhbGc...",
  "username": "admin",
  "nickname": "管理员",
  "userId": 1,
  "roles": ["admin"]
}
```

#### GET /api/auth/me - 获取当前用户信息

请求头：`Authorization: Bearer <token>`

### 2. 电池数据接口

#### GET /api/battery/devices - 获取用户设备列表

需要登录，返回当前用户绑定的设备列表

#### GET /api/battery/latest/{deviceUuid} - 获取设备最新数据

需要登录，只能查看有权限访问的设备

#### GET /api/battery/recent/{deviceUuid} - 获取设备历史数据

参数：`limit` 默认100条

#### GET /api/battery/stream/{deviceUuid} - SSE实时数据流

### 3. 管理接口（仅admin）

#### GET /api/admin/users - 获取所有用户

#### POST /api/admin/users - 创建用户

#### PUT /api/admin/users/{id} - 更新用户

#### DELETE /api/admin/users/{id} - 删除用户

#### GET /api/admin/devices - 获取所有设备

#### POST /api/admin/devices - 创建设备

```json
{
  "deviceUuid": "12345678-1234-1234-1234-123456789ABC",
  "deviceName": "电池设备1",
  "deviceType": "BMS"
}
```

#### PUT /api/admin/devices/{id} - 更新设备

#### DELETE /api/admin/devices/{id} - 删除设备

#### POST /api/admin/devices/{deviceId}/bind/{userId} - 绑定设备到用户

#### POST /api/admin/devices/{deviceId}/unbind/{userId} - 解绑设备

***

## 五、测试步骤

### 步骤1：启动项目

```bash
mvn spring-boot:run
```

项目启动时会自动创建`bms_uuid`数据库和所有表，并插入预置数据。

### 步骤2：管理员登录创建设备

```bash
# 1. 登录获取token
POST /api/auth/login
{"username": "admin", "password": "password123"}

# 2. 创建设备
POST /api/admin/devices
Authorization: Bearer <token>
{
  "deviceUuid": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "用户1的设备",
  "deviceType": "BMS"
}

# 3. 绑定设备到用户1
POST /api/admin/devices/1/bind/2
```

### 步骤3：用户1登录测试

```bash
# 1. 用户1登录
POST /api/auth/login
{"username": "user1", "password": "password123"}

# 2. 查看自己的设备
GET /api/battery/devices
Authorization: Bearer <user1_token>

# 3. 查看设备数据
GET /api/battery/latest/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <user1_token>
```

### 步骤4：模拟设备连接

```bash
# 使用TCP客户端连接 localhost:9000
# 发送注册
REGISTER#550e8400-e29b-41d4-a716-446655440000

# 发送数据
550e8400-e29b-41d4-a716-446655440000#12.5#2.3#25#80#28.8#1#0#0
```

***

## 六、用户权限说明

| 用户    | 角色   | 权限                         |
| ----- | ---- | -------------------------- |
| admin | 管理员  | 查看/管理所有用户、查看/管理所有设备、查看所有数据 |
| user1 | 普通用户 | 查看自己的设备、查看自己设备的数据          |
| user2 | 普通用户 | 查看自己的设备、查看自己设备的数据          |

***

## 七、注意事项

1. 设备UUID需要预先在管理界面创建设备后才能注册
2. 用户必须先绑定设备才能查看该设备的数据
3. TCP服务器会自动将设备状态设置为在线/离线
4. 所有需要权限的接口都需要在请求头携带 `Authorization: Bearer <token>`

