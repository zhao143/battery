# Test BMS API
$ErrorActionPreference = "Stop"

Write-Host "=== 测试1: Admin登录 ===" -ForegroundColor Cyan
try {
    $loginResult = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"admin","password":"password123"}'
    Write-Host "登录成功!" -ForegroundColor Green
    Write-Host "Token: $($loginResult.token.Substring(0, 50))..."
    $adminToken = $loginResult.token
} catch {
    Write-Host "登录失败: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== 测试2: Admin获取用户列表 ===" -ForegroundColor Cyan
try {
    $users = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Method Get -Header @{Authorization="Bearer $adminToken"}
    Write-Host "用户列表:" -ForegroundColor Green
    $users | ForEach-Object { Write-Host "  - $($_.username) (ID: $($_.id), Nickname: $($_.nickname))" }
} catch {
    Write-Host "获取用户列表失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试3: Admin创建设备 ===" -ForegroundColor Cyan
$deviceUuid1 = "550e8400-e29b-41d4-a716-446655440001"
$deviceUuid2 = "550e8400-e29b-41d4-a716-446655440002"

try {
    $deviceBody1 = @{
        deviceUuid = $deviceUuid1
        deviceName = "用户1的设备"
        deviceType = "BMS"
    } | ConvertTo-Json

    $device1 = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices" -Method Post -ContentType "application/json" -Header @{Authorization="Bearer $adminToken"} -Body $deviceBody1
    Write-Host "设备1创建成功: $deviceUuid1" -ForegroundColor Green
} catch {
    Write-Host "设备1可能已存在或创建失败: $_" -ForegroundColor Yellow
}

try {
    $deviceBody2 = @{
        deviceUuid = $deviceUuid2
        deviceName = "用户2的设备"
        deviceType = "BMS"
    } | ConvertTo-Json

    $device2 = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices" -Method Post -ContentType "application/json" -Header @{Authorization="Bearer $adminToken"} -Body $deviceBody2
    Write-Host "设备2创建成功: $deviceUuid2" -ForegroundColor Green
} catch {
    Write-Host "设备2可能已存在或创建失败: $_" -ForegroundColor Yellow
}

Write-Host "`n=== 测试4: Admin获取所有设备 ===" -ForegroundColor Cyan
try {
    $allDevices = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices" -Method Get -Header @{Authorization="Bearer $adminToken"}
    Write-Host "所有设备:" -ForegroundColor Green
    $allDevices | ForEach-Object { Write-Host "  - $($_.deviceName) (UUID: $($_.deviceUuid), Status: $($_.status))" }
} catch {
    Write-Host "获取设备列表失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试5: Admin绑定设备到用户 ===" -ForegroundColor Cyan
try {
    # 绑定设备1到用户1 (userId=2)
    Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices/1/bind/2" -Method Post -Header @{Authorization="Bearer $adminToken"}
    Write-Host "设备1绑定到用户1成功" -ForegroundColor Green

    # 绑定设备2到用户2 (userId=3)
    Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices/2/bind/3" -Method Post -Header @{Authorization="Bearer $adminToken"}
    Write-Host "设备2绑定到用户2成功" -ForegroundColor Green
} catch {
    Write-Host "绑定设备失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试6: User1登录 ===" -ForegroundColor Cyan
try {
    $user1Login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"user1","password":"password123"}'
    Write-Host "User1登录成功!" -ForegroundColor Green
    $user1Token = $user1Login.token
} catch {
    Write-Host "User1登录失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试7: User1获取设备列表 ===" -ForegroundColor Cyan
try {
    $user1Devices = Invoke-RestMethod -Uri "http://localhost:8080/api/battery/devices" -Method Get -Header @{Authorization="Bearer $user1Token"}
    Write-Host "User1的设备:" -ForegroundColor Green
    $user1Devices | ForEach-Object { Write-Host "  - $($_.deviceName) (UUID: $($_.deviceUuid))" }
} catch {
    Write-Host "获取User1设备失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试8: User2登录 ===" -ForegroundColor Cyan
try {
    $user2Login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"user2","password":"password123"}'
    Write-Host "User2登录成功!" -ForegroundColor Green
    $user2Token = $user2Login.token
} catch {
    Write-Host "User2登录失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试9: User2获取设备列表 ===" -ForegroundColor Cyan
try {
    $user2Devices = Invoke-RestMethod -Uri "http://localhost:8080/api/battery/devices" -Method Get -Header @{Authorization="Bearer $user2Token"}
    Write-Host "User2的设备:" -ForegroundColor Green
    $user2Devices | ForEach-Object { Write-Host "  - $($_.deviceName) (UUID: $($_.deviceUuid))" }
} catch {
    Write-Host "获取User2设备失败: $_" -ForegroundColor Red
}

Write-Host "`n=== 测试完成 ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "=== 需要烧录到设备的UUID ===" -ForegroundColor Yellow
Write-Host "用户1设备: $deviceUuid1" -ForegroundColor White
Write-Host "用户2设备: $deviceUuid2" -ForegroundColor White
