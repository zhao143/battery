# BMS Interactive Test Script
# Run with: pwsh -ExecutionPolicy Bypass -File test-interactive.ps1

param(
    [switch]$Auto
)

$ErrorActionPreference = "Continue"
$global:token = $null
$global:username = $null

function Show-Menu {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "         BMS Interactive Test Menu" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Current: $(if($global:username){$global:username}else{'Not logged in'})" -ForegroundColor $(if($global:username){'Green'}else{'Gray'})
    Write-Host ""
    Write-Host "1. Login as Admin" -ForegroundColor Yellow
    Write-Host "2. Login as User1" -ForegroundColor Yellow
    Write-Host "3. Login as User2" -ForegroundColor Yellow
    Write-Host "4. View All Devices (Admin only)" -ForegroundColor Yellow
    Write-Host "5. View My Devices" -ForegroundColor Yellow
    Write-Host "6. View All Users (Admin only)" -ForegroundColor Yellow
    Write-Host "7. Create New Device (Admin only)" -ForegroundColor Yellow
    Write-Host "8. Bind Device to User (Admin only)" -ForegroundColor Yellow
    Write-Host "9. Simulate Device Data (TCP)" -ForegroundColor Yellow
    Write-Host "10. Get Latest Data" -ForegroundColor Yellow
    Write-Host "11. Logout" -ForegroundColor Yellow
    Write-Host "0. Exit" -ForegroundColor Red
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host -NoNewline "Select option: " -ForegroundColor White
}

function Login {
    param($user)
    try {
        $result = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body "{`"username`":`"$user`",`"password`":`"password123`"}"
        $global:token = $result.token
        $global:username = $result.username
        Write-Host "  Login SUCCESS: $($result.nickname) ($($result.username))" -ForegroundColor Green
        Write-Host "  Roles: $($result.roles -join ', ')" -ForegroundColor Gray
        return $true
    } catch {
        Write-Host "  Login FAILED: $_" -ForegroundColor Red
        return $false
    }
}

function Get-AllDevices {
    if (-not $global:token) { Write-Host "  Please login first!" -ForegroundColor Red; return }
    try {
        $devices = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices" -Method Get -Header @{Authorization="Bearer $global:token"}
        Write-Host "  Total devices: $($devices.Count)" -ForegroundColor Green
        $devices | ForEach-Object {
            Write-Host "    - $($_.deviceName) (UUID: $($_.deviceUuid)) Status: $(if($_.status -eq 1){'Online'}else{'Offline'})" -ForegroundColor White
        }
    } catch {
        Write-Host "  FAILED (Admin only): $_" -ForegroundColor Red
    }
}

function Get-MyDevices {
    if (-not $global:token) { Write-Host "  Please login first!" -ForegroundColor Red; return }
    try {
        $devices = Invoke-RestMethod -Uri "http://localhost:8080/api/battery/devices" -Method Get -Header @{Authorization="Bearer $global:token"}
        Write-Host "  My devices: $($devices.Count)" -ForegroundColor Green
        $devices | ForEach-Object {
            Write-Host "    - $($_.deviceName) (UUID: $($_.deviceUuid)) Status: $(if($_.status -eq 1){'Online'}else{'Offline'})" -ForegroundColor White
        }
    } catch {
        Write-Host "  FAILED: $_" -ForegroundColor Red
    }
}

function Get-AllUsers {
    if (-not $global:token) { Write-Host "  Please login first!" -ForegroundColor Red; return }
    try {
        $users = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Method Get -Header @{Authorization="Bearer $global:token"}
        Write-Host "  Total users: $($users.Count)" -ForegroundColor Green
        $users | ForEach-Object {
            Write-Host "    - $($_.nickname) ($($_.username)) Status: $(if($_.status -eq 1){'Active'}else{'Inactive'})" -ForegroundColor White
        }
    } catch {
        Write-Host "  FAILED (Admin only): $_" -ForegroundColor Red
    }
}

function Create-Device {
    if (-not $global:token) { Write-Host "  Please login first!" -ForegroundColor Red; return }
    $uuid = Read-Host "  Enter device UUID (or press Enter for random)"
    if ([string]::IsNullOrWhiteSpace($uuid)) {
        $uuid = [guid]::NewGuid().ToString()
    }
    $name = Read-Host "  Enter device name"
    if ([string]::IsNullOrWhiteSpace($name)) { $name = "New Device" }
    $type = Read-Host "  Enter device type (default: BMS)"
    if ([string]::IsNullOrWhiteSpace($type)) { $type = "BMS" }

    try {
        $body = @{
            deviceUuid = $uuid
            deviceName = $name
            deviceType = $type
        } | ConvertTo-Json
        $result = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices" -Method Post -ContentType "application/json" -Header @{Authorization="Bearer $global:token"} -Body $body
        Write-Host "  Device created: $name" -ForegroundColor Green
    } catch {
        Write-Host "  FAILED: $_" -ForegroundColor Red
    }
}

function Bind-Device {
    if (-not $global:token) { Write-Host "  Please login first!" -ForegroundColor Red; return }
    Get-AllDevices
    $deviceId = Read-Host "  Enter device ID to bind"
    $userId = Read-Host "  Enter user ID to bind to"
    try {
        Invoke-RestMethod -Uri "http://localhost:8080/api/admin/devices/$deviceId/bind/$userId" -Method Post -Header @{Authorization="Bearer $global:token"}
        Write-Host "  Device bound successfully!" -ForegroundColor Green
    } catch {
        Write-Host "  FAILED: $_" -ForegroundColor Red
    }
}

function Simulate-DeviceData {
    Write-Host "`n  === TCP Device Simulation ===" -ForegroundColor Cyan
    Write-Host "  This will simulate a device connecting and sending data" -ForegroundColor Gray
    Write-Host "  1. User1 Device: 550e8400-e29b-41d4-a716-446655440001" -ForegroundColor White
    Write-Host "  2. User2 Device: 550e8400-e29b-41d4-a716-446655440002" -ForegroundColor White
    Write-Host "  3. Custom UUID" -ForegroundColor White

    $choice = Read-Host "  Select device (1-3)"
    $uuid = switch($choice) {
        "1" { "550e8400-e29b-41d4-a716-446655440001" }
        "2" { "550e8400-e29b-41d4-a716-446655440002" }
        "3" { Read-Host "  Enter UUID" }
        default { "550e8400-e29b-41d4-a716-446655440001" }
    }

    Write-Host "`n  Sending REGISTER command..." -ForegroundColor Yellow
    $registerCmd = "REGISTER#$uuid"
    Write-Host "  Command: $registerCmd" -ForegroundColor White

    Write-Host "`n  Simulating data stream (Ctrl+C to stop)..." -ForegroundColor Yellow
    Write-Host "  Format: UUID#电压#电流#温度#电量#功率#风扇#继电器#报警" -ForegroundColor Gray

    $iteration = 0
    $random = New-Object System.Random

    try {
        while ($true) {
            $iteration++
            $voltage = [math]::Round(10 + $random.NextDouble() * 5, 1)
            $current = [math]::Round($random.NextDouble() * 5, 1)
            $temp = 20 + $random.Next(0, 15)
            $charge = 50 + $random.Next(0, 50)
            $power = [math]::Round($voltage * $current, 1)
            $fan = $random.Next(0, 2)
            $relay = $random.Next(0, 2)
            $alarm = if ($temp -gt 30 -or $voltage -gt 14) { 1 } else { 0 }

            $data = "$uuid#$voltage#$current#$temp#$charge#$power#$fan#$relay#$alarm"
            Write-Host "  [$iteration] $data" -ForegroundColor $(if($alarm -eq 1){'Red'}else{'Green'})

            Start-Sleep -Seconds 2
        }
    } catch {
        Write-Host "`n  Simulation stopped" -ForegroundColor Gray
    }
}

function Get-LatestData {
    if (-not $global:token) { Write-Host "  Please login first!" -ForegroundColor Red; return }
    try {
        $data = Invoke-RestMethod -Uri "http://localhost:8080/api/battery/latest" -Method Get -Header @{Authorization="Bearer $global:token"}
        if ($data.message) {
            Write-Host "  No data available" -ForegroundColor Gray
        } else {
            Write-Host "  Voltage: $($data.voltage) V" -ForegroundColor White
            Write-Host "  Current: $($data.current) A" -ForegroundColor White
            Write-Host "  Temperature: $($data.temperature) C" -ForegroundColor White
            Write-Host "  Charge: $($data.charge)%" -ForegroundColor White
            Write-Host "  Power: $($data.power) W" -ForegroundColor White
            Write-Host "  Alarm: $(if($data.alarmState -eq 1){'ALARM!'}else{'Normal'})" -ForegroundColor $(if($data.alarmState -eq 1){'Red'}else{'Green'})
        }
    } catch {
        Write-Host "  FAILED: $_" -ForegroundColor Red
    }
}

# Main loop
if ($Auto) {
    Write-Host "Running in auto mode..." -ForegroundColor Cyan
    Login -user "admin"
    Get-AllUsers
    Get-AllDevices
    Get-LatestData
} else {
    while ($true) {
        Show-Menu
        $choice = Read-Host ""
        switch ($choice) {
            "1" { Login -user "admin" }
            "2" { Login -user "user1" }
            "3" { Login -user "user2" }
            "4" { Get-AllDevices }
            "5" { Get-MyDevices }
            "6" { Get-AllUsers }
            "7" { Create-Device }
            "8" { Bind-Device }
            "9" { Simulate-DeviceData }
            "10" { Get-LatestData }
            "11" { $global:token = $null; $global:username = null; Write-Host "  Logged out" -ForegroundColor Gray }
            "0" { Write-Host "Goodbye!" -ForegroundColor Cyan; break }
            default { Write-Host "  Invalid option" -ForegroundColor Red }
        }
    }
}
