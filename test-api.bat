# BMS API Test
# Test 1: Admin Login
echo "=== Test 1: Admin Login ==="
curl -s -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"password123\"}"
echo.

# Test 2: Get Users (if login works)
echo.
echo "=== Test 2: Get Users ==="
curl -s -X GET http://localhost:8080/api/admin/users ^
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
echo.
