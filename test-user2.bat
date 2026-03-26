@echo off
echo ========================================
echo BMS API Test - User2
echo ========================================

echo.
echo === Test 1: User2 Login ===
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"user2\",\"password\":\"password123\"}"
echo.

echo.
echo === Test 2: User2 Get Devices ===
echo (Need to copy token from above response)
echo curl -s -X GET http://localhost:8080/api/battery/devices -H "Authorization: Bearer YOUR_TOKEN"
echo.

echo.
echo === Test 3: User2 Get Latest Data (if device connected) ===
echo curl -s -X GET http://localhost:8080/api/battery/latest/550e8400-e29b-41d4-a716-446655440002 -H "Authorization: Bearer YOUR_TOKEN"
echo.

echo.
echo ========================================
echo Test Complete!
echo ========================================
pause
