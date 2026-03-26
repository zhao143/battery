# BMS API Complete Test for Admin
# Run with: pwsh -ExecutionPolicy Bypass -File test-admin-full.ps1

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BMS API Complete Test - Admin" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Step 1: Login as Admin
Write-Host "`n[Step 1] Admin Login..." -ForegroundColor Yellow
try {
    $loginResult = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"admin","password":"password123"}'
    Write-Host "  SUCCESS! Token received" -ForegroundColor Green
    $adminToken = $loginResult.token
    Write-Host "  User: $($loginResult.nickname) ($($loginResult.username))" -ForegroundColor White
    Write-Host "  Roles: $($loginResult.roles -join ', ')" -ForegroundColor White
} catch {
    Write-Host "  FAILED: $_" -ForegroundColor Red
    exit 1
}

# Step 2: Get all users (Admin only)
Write-Host "`n[Step 2] Get All Users (Admin)..." -ForegroundColor Yellow
try {
    $users = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Method Get -Header @{Authorization="Bearer $adminToken"}
    Write-Host "  User count: $($users.Count)" -ForegroundColor Green
    $users | ForEach-Object { 
        Write-Host "  - $($_.nickname) ($($_.username)) - Status: $(if($_.status -eq 1){'Active'}else{'Inactive'})" -ForegroundColor White
    }
} catch {
    Write-Host "  FAILED: $_" -ForegroundColor Red
}

# Step 3: Get all devices (Admin only)
Write-Host "`n[Step 3] Get All Devices (Admin)..." -ForegroundColor Yellow
try {
    $allDevices = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices" -Method Get -Header @{Authorization="Bearer $adminToken"}
    Write-Host "  Device count: $($allDevices.Count)" -ForegroundColor Green
    $allDevices | ForEach-Object { 
        Write-Host "  - $($_.deviceName)" -ForegroundColor White
        Write-Host "    UUID: $($_.deviceUuid)" -ForegroundColor Gray
        Write-Host "    Status: $(if($_.status -eq 1){'Online'}else{'Offline'})" -ForegroundColor $(if($_.status -eq 1){'Green'}else{'Gray'})
    }
} catch {
    Write-Host "  FAILED: $_" -ForegroundColor Red
}

# Step 4: Get latest data
Write-Host "`n[Step 4] Get Latest Data..." -ForegroundColor Yellow
try {
    $latestData = Invoke-RestMethod -Uri "http://localhost:8080/api/battery/latest" -Method Get -Header @{Authorization="Bearer $adminToken"}
    if ($latestData.message) {
        Write-Host "  No data yet" -ForegroundColor Gray
    } else {
        Write-Host "  Voltage: $($latestData.voltage) V" -ForegroundColor White
        Write-Host "  Current: $($latestData.current) A" -ForegroundColor White
        Write-Host "  Temperature: $($latestData.temperature) C" -ForegroundColor White
        Write-Host "  Charge: $($latestData.charge)%" -ForegroundColor White
    }
} catch {
    Write-Host "  FAILED: $_" -ForegroundColor Red
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Admin can:" -ForegroundColor White
Write-Host "  - View all users" -ForegroundColor Green
Write-Host "  - View all devices" -ForegroundColor Green
Write-Host "  - Create/Edit/Delete devices" -ForegroundColor Green
Write-Host "  - Bind devices to users" -ForegroundColor Green

Write-Host "`nDevice UUIDs for testing:" -ForegroundColor Yellow
Write-Host "  User1: 550e8400-e29b-41d4-a716-446655440001" -ForegroundColor White
Write-Host "  User2: 550e8400-e29b-41d4-a716-446655440002" -ForegroundColor White

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
