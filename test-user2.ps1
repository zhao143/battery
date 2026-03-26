# BMS API Test for User2
$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BMS API Test - User2" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`n=== Test 1: User2 Login ===" -ForegroundColor Yellow
try {
    $loginResult = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"username":"user2","password":"password123"}'
    Write-Host "Login SUCCESS!" -ForegroundColor Green
    Write-Host "Token: $($loginResult.token.Substring(0, 50))..." -ForegroundColor White
    $user2Token = $loginResult.token
    Write-Host "User: $($loginResult.nickname) ($($loginResult.username))" -ForegroundColor White
    Write-Host "Roles: $($loginResult.roles -join ', ')" -ForegroundColor White
} catch {
    Write-Host "Login FAILED: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== Test 2: User2 Get Devices ===" -ForegroundColor Yellow
try {
    $devices = Invoke-RestMethod -Uri "http://localhost:8080/api/battery/devices" -Method Get -Header @{Authorization="Bearer $user2Token"}
    Write-Host "Devices count: $($devices.Count)" -ForegroundColor Green
    $devices | ForEach-Object { 
        Write-Host "  - $($_.deviceName)" -ForegroundColor White
        Write-Host "    UUID: $($_.deviceUuid)" -ForegroundColor Gray
        Write-Host "    Status: $(if($_.status -eq 1){'Online'}else{'Offline'})" -ForegroundColor $(if($_.status -eq 1){'Green'}else{'Gray'})
    }
} catch {
    Write-Host "Get devices FAILED: $_" -ForegroundColor Red
}

Write-Host "`n=== Test 3: User2 Get Latest Data ===" -ForegroundColor Yellow
try {
    $latestData = Invoke-RestMethod -Uri "http://localhost:8080/api/battery/latest" -Method Get -Header @{Authorization="Bearer $user2Token"}
    if ($latestData.message) {
        Write-Host "No data: $($latestData.message)" -ForegroundColor Gray
    } else {
        Write-Host "Latest data:" -ForegroundColor Green
        Write-Host "  Voltage: $($latestData.voltage) V" -ForegroundColor White
        Write-Host "  Current: $($latestData.current) A" -ForegroundColor White
        Write-Host "  Temperature: $($latestData.temperature) C" -ForegroundColor White
        Write-Host "  Charge: $($latestData.charge)%" -ForegroundColor White
        Write-Host "  Power: $($latestData.power) W" -ForegroundColor White
        Write-Host "  Alarm: $(if($latestData.alarmState -eq 1){'ALARM'}else{'Normal'})" -ForegroundColor $(if($latestData.alarmState -eq 1){'Red'}else{'Green'})
    }
} catch {
    Write-Host "Get latest data FAILED: $_" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "`nDevice UUID for User2 (for TCP connection):" -ForegroundColor Yellow
Write-Host "550e8400-e29b-41d4-a716-446655440002" -ForegroundColor White

Write-Host "`nTCP Test Commands:" -ForegroundColor Yellow
Write-Host "1. Connect to localhost:9000" -ForegroundColor Gray
Write-Host "2. Send: REGISTER#550e8400-e29b-41d4-a716-446655440002" -ForegroundColor Gray
Write-Host "3. Send data: 550e8400-e29b-41d4-a716-446655440002#12.5#2.3#25#80#28.8#1#0#0" -ForegroundColor Gray
