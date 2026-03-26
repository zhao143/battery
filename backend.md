# 电池管理系统（BMS）使用说明

## 项目概述
- 技术栈：`Spring Boot 3.3`、`Java 21`、`MySQL`、`MyBatis`、`Swagger UI`、`SSE`
- 功能：TCP双向通信、数据入库、后台Web控制页面、接口文档、实时监控

## 环境要求
- 安装 `Java 21`、`Maven 3.9+`、`MySQL 8+`
- MySQL 需预先创建数据库 `bms`，并确保用户 `root` 密码 `123456`

```sql
CREATE DATABASE IF NOT EXISTS bms CHARACTER SET utf8mb4;
```

## 快速开始
1. 启动应用（开发模式）
   - `mvn spring-boot:run`
2. 访问后台页面
   - `http://localhost:8080/`
3. 打开接口文档
   - `http://localhost:8080/swagger-ui.html`

## 运行与打包
- 生成可执行包：`mvn -DskipTests package`
- 运行可执行包：`java -jar target/bms-0.0.1-SNAPSHOT.jar`

## 配置
- 应用配置文件：`src/main/resources/application.yml`
  - Web端口：`server.port: 8080`
  - TCP端口：`tcp.port: 9000`
  - 数据源：`spring.datasource.url: jdbc:mysql://localhost:3306/bms`
  - Swagger UI 路径：`springdoc.swagger-ui.path: /swagger-ui.html`

## 页面与接口
- 后台控制页面：`GET /`（展示实时数据与控制面板）
- 最新数据：`GET /api/battery/latest`
- 最近数据：`GET /api/battery/recent?limit=100`
- 获取控制状态：`GET /api/control`
- 设置控制状态：`POST /api/control`，示例 body：`{"fan":1,"relay":0}`
- 实时数据流（SSE）：`GET /api/battery/stream`
- 生成测试数据（10条）：`POST /api/battery/mock`

## TCP通信
- 服务器监听：`tcp.port`（默认 `9000`）
- 客户端上行消息格式：`%.1f#%.1f#%d#%d#%2.1f#%d#%d#%d`
  - 电压(V)#电流(A)#温度(°C)#电量(%)#功率(W)#风扇状态#继电器状态#报警状态
  - 示例：`12.5#2.3#25#80#28.8#1#1#0`（每条消息以换行结束）
- 服务器下行控制指令：`CTRL#<fan>#<relay>`（如 `CTRL#1#0`）
- 说明：服务器收到客户端数据后解析入库，并立即回传当前控制状态；当通过后台页面或接口变更控制状态时会向在线客户端广播最新控制指令。

## 示例命令（Windows PowerShell）
- 生成 10 条测试数据：
  - `Invoke-WebRequest -Uri http://localhost:8080/api/battery/mock -Method POST -OutFile mock.json`
  - `Get-Content mock.json | Select-Object -First 20`
- 设置风扇开启、继电器关闭：
  - `Invoke-WebRequest -Uri http://localhost:8080/api/control -Method POST -Body '{"fan":1,"relay":0}' -ContentType 'application/json'`
- 拉取最近 10 条数据：
  - `Invoke-WebRequest -Uri http://localhost:8080/api/battery/recent?limit=10 -OutFile recent.json`

## 数据库
- 表结构由 `schema.sql` 自动创建：`src/main/resources/schema.sql`
- 表：`battery_data`
  - 字段：电压、电流、温度、电量、功率、风扇状态、继电器状态、报警状态、时间戳

## 常见问题
- 启动报错：数据库不存在或无法连接
  - 确认 MySQL 已启动、已创建库 `bms`，用户名/密码与 `application.yml` 一致
- TCP 不回包或不入库
  - 检查客户端是否按格式发送且每条消息以换行结束；查看日志是否有解析错误

## 代码位置参考
- 应用入口：`src/main/java/com/example/bms/BmsApplication.java:1`
- TCP 服务：`src/main/java/com/example/bms/tcp/TcpServer.java:1`
- 电池接口：`src/main/java/com/example/bms/controller/BatteryController.java:1`
- 控制接口：`src/main/java/com/example/bms/controller/ControlController.java:1`
- 实时流接口：`src/main/java/com/example/bms/controller/StreamController.java:1`
- 服务与推送：`src/main/java/com/example/bms/service/BatteryService.java:1`
- Mapper 接口：`src/main/java/com/example/bms/mapper/BatteryDataMapper.java:1`
- Mapper XML：`src/main/resources/mapper/BatteryDataMapper.xml:1`