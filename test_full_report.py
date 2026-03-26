#!/usr/bin/env python3
"""
BMS Complete Test Report Generator
Tests all features and generates detailed report
"""

import requests
import json
import time
import sys
from datetime import datetime

BASE_URL = "http://localhost:8080"

class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header(text):
    print(f"\n{Colors.HEADER}{'='*70}{Colors.ENDC}")
    print(f"{Colors.HEADER}{text.center(70)}{Colors.ENDC}")
    print(f"{Colors.HEADER}{'='*70}{Colors.ENDC}")

def print_success(text):
    print(f"{Colors.GREEN}✓ {text}{Colors.ENDC}")

def print_error(text):
    print(f"{Colors.FAIL}✗ {text}{Colors.ENDC}")

def print_warning(text):
    print(f"{Colors.WARNING}⚠ {text}{Colors.ENDC}")

def print_info(text):
    print(f"{Colors.CYAN}ℹ {text}{Colors.ENDC}")

class BMSTest:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})
        self.results = []
        self.admin_token = None
        self.user1_token = None
        self.user2_token = None

    def log_result(self, test_name, passed, message="", data=None):
        self.results.append({
            "test": test_name,
            "passed": passed,
            "message": message,
            "data": data,
            "timestamp": datetime.now().strftime("%H:%M:%S")
        })
        status = f"{Colors.GREEN}PASS{Colors.ENDC}" if passed else f"{Colors.FAIL}FAIL{Colors.ENDC}"
        print(f"  [{status}] {test_name}")
        if message:
            print(f"         {message}")

    def test_admin_login(self):
        print_header("测试 Admin 登录")
        try:
            resp = self.session.post(f"{BASE_URL}/api/auth/login", 
                json={"username": "admin", "password": "password123"})
            if resp.status_code == 200:
                data = resp.json()
                self.admin_token = data.get("token")
                self.log_result("Admin登录", True, f"Token: {data.get('token')[:30]}...", data)
                return True
            else:
                self.log_result("Admin登录", False, resp.text[:100])
                return False
        except Exception as e:
            self.log_result("Admin登录", False, str(e))
            return False

    def test_user1_login(self):
        print_header("测试 User1 登录")
        try:
            resp = self.session.post(f"{BASE_URL}/api/auth/login",
                json={"username": "user1", "password": "password123"})
            if resp.status_code == 200:
                data = resp.json()
                self.user1_token = data.get("token")
                self.log_result("User1登录", True, f"角色: {data.get('roles')}", data)
                return True
            else:
                self.log_result("User1登录", False, resp.text[:100])
                return False
        except Exception as e:
            self.log_result("User1登录", False, str(e))
            return False

    def test_user2_login(self):
        print_header("测试 User2 登录")
        try:
            resp = self.session.post(f"{BASE_URL}/api/auth/login",
                json={"username": "user2", "password": "password123"})
            if resp.status_code == 200:
                data = resp.json()
                self.user2_token = data.get("token")
                self.log_result("User2登录", True, f"角色: {data.get('roles')}", data)
                return True
            else:
                self.log_result("User2登录", False, resp.text[:100])
                return False
        except Exception as e:
            self.log_result("User2登录", False, str(e))
            return False

    def test_admin_get_users(self):
        print_header("测试 Admin 获取所有用户")
        try:
            resp = self.session.get(f"{BASE_URL}/api/admin/users",
                headers={"Authorization": f"Bearer {self.admin_token}"})
            if resp.status_code == 200:
                users = resp.json()
                self.log_result("Admin获取用户列表", True, f"共{len(users)}个用户", users)
                return users
            else:
                self.log_result("Admin获取用户列表", False, f"HTTP {resp.status_code}")
                return None
        except Exception as e:
            self.log_result("Admin获取用户列表", False, str(e))
            return None

    def test_admin_get_devices(self):
        print_header("测试 Admin 获取所有设备")
        try:
            resp = self.session.get(f"{BASE_URL}/api/admin/devices",
                headers={"Authorization": f"Bearer {self.admin_token}"})
            if resp.status_code == 200:
                devices = resp.json()
                self.log_result("Admin获取设备列表", True, f"共{len(devices)}个设备", devices)
                return devices
            else:
                self.log_result("Admin获取设备列表", False, f"HTTP {resp.status_code}")
                return None
        except Exception as e:
            self.log_result("Admin获取设备列表", False, str(e))
            return None

    def test_admin_create_device(self):
        print_header("测试 Admin 创建设备")
        import uuid
        test_uuid = f"test-{uuid.uuid4()}"
        try:
            resp = self.session.post(f"{BASE_URL}/api/admin/devices",
                headers={"Authorization": f"Bearer {self.admin_token}"},
                json={"deviceUuid": test_uuid, "deviceName": "测试设备", "deviceType": "BMS"})
            print(f"       Response status: {resp.status_code}")
            print(f"       Response text: {resp.text[:200]}")
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    self.log_result("Admin创建设备", True, f"设备ID: {data.get('deviceId')}", data)
                    return data.get("deviceId")
                except:
                    self.log_result("Admin创建设备", False, f"无法解析JSON: {resp.text[:100]}")
                    return None
            else:
                self.log_result("Admin创建设备", False, f"HTTP {resp.status_code}: {resp.text[:100]}")
                return None
        except Exception as e:
            self.log_result("Admin创建设备", False, str(e))
            return None

    def test_user_get_devices(self, token, username):
        print_header(f"测试 {username} 获取设备")
        try:
            resp = self.session.get(f"{BASE_URL}/api/battery/devices",
                headers={"Authorization": f"Bearer {token}"})
            if resp.status_code == 200:
                devices = resp.json()
                self.log_result(f"{username}获取设备", True, f"共{len(devices)}个设备", devices)
                return devices
            else:
                self.log_result(f"{username}获取设备", False, f"HTTP {resp.status_code}")
                return None
        except Exception as e:
            self.log_result(f"{username}获取设备", False, str(e))
            return None

    def test_get_latest(self, token, username):
        print_header(f"测试 {username} 获取最新数据")
        try:
            resp = self.session.get(f"{BASE_URL}/api/battery/latest",
                headers={"Authorization": f"Bearer {token}"})
            print(f"       Response status: {resp.status_code}")
            print(f"       Response text: {resp.text[:200]}")
            if resp.status_code == 200:
                if resp.text.strip():
                    try:
                        data = resp.json()
                        if isinstance(data, list):
                            self.log_result(f"{username}获取最新数据", True, f"共{len(data)}条数据", data)
                        else:
                            self.log_result(f"{username}获取最新数据", True, str(data)[:50], data)
                        return data
                    except json.JSONDecodeError as je:
                        self.log_result(f"{username}获取最新数据", False, f"JSON解析错误: {je}, text: {resp.text[:100]}")
                        return None
                else:
                    self.log_result(f"{username}获取最新数据", True, "空响应")
                    return []
            else:
                self.log_result(f"{username}获取最新数据", False, f"HTTP {resp.status_code}")
                return None
        except Exception as e:
            self.log_result(f"{username}获取最新数据", False, str(e))
            return None

    def test_get_recent(self, token, username):
        print_header(f"测试 {username} 获取历史数据")
        try:
            resp = self.session.get(f"{BASE_URL}/api/battery/recent?limit=10",
                headers={"Authorization": f"Bearer {token}"})
            if resp.status_code == 200:
                data = resp.json()
                self.log_result(f"{username}获取历史数据", True, f"共{len(data)}条记录", data)
                return data
            else:
                self.log_result(f"{username}获取历史数据", False, f"HTTP {resp.status_code}")
                return None
        except Exception as e:
            self.log_result(f"{username}获取历史数据", False, str(e))
            return None

    def test_get_latest_by_uuid(self, token, username, uuid):
        print_header(f"测试 {username} 按UUID获取数据: {uuid}")
        try:
            resp = self.session.get(f"{BASE_URL}/api/battery/latest/{uuid}",
                headers={"Authorization": f"Bearer {token}"})
            if resp.status_code == 200:
                data = resp.json()
                if data.get("message"):
                    self.log_result(f"{username}按UUID获取数据", True, f"暂无数据 ({data.get('message')})")
                else:
                    self.log_result(f"{username}按UUID获取数据", True, f"电压:{data.get('voltage')}V", data)
                return data
            else:
                self.log_result(f"{username}按UUID获取数据", False, f"HTTP {resp.status_code}")
                return None
        except Exception as e:
            self.log_result(f"{username}按UUID获取数据", False, str(e))
            return None

    def test_unauthorized_access(self):
        print_header("测试 未授权访问")
        try:
            resp = self.session.get(f"{BASE_URL}/api/admin/users")
            if resp.status_code in [401, 403]:
                self.log_result("未授权访问被拒绝", True, f"HTTP {resp.status_code} (预期行为)")
                return True
            else:
                self.log_result("未授权访问被拒绝", False, f"HTTP {resp.status_code}")
                return False
        except Exception as e:
            self.log_result("未授权访问被拒绝", False, str(e))
            return False

    def test_invalid_token(self):
        print_header("测试 无效Token访问")
        try:
            resp = self.session.get(f"{BASE_URL}/api/battery/devices",
                headers={"Authorization": "Bearer invalid-token-12345"})
            if resp.status_code in [401, 403]:
                self.log_result("无效Token被拒绝", True, f"HTTP {resp.status_code} (预期行为)")
                return True
            else:
                self.log_result("无效Token被拒绝", False, f"HTTP {resp.status_code}")
                return False
        except Exception as e:
            self.log_result("无效Token被拒绝", False, str(e))
            return False

    def generate_report(self):
        print_header("测试报告汇总")

        passed = sum(1 for r in self.results if r["passed"])
        failed = sum(1 for r in self.results if not r["passed"])
        total = len(self.results)

        print(f"\n总测试数: {total}")
        print(f"{Colors.GREEN}通过: {passed}{Colors.ENDC}")
        print(f"{Colors.FAIL}失败: {failed}{Colors.ENDC}")
        if total > 0:
            print(f"通过率: {passed/total*100:.1f}%")

        print_header("详细测试结果")
        print(f"{'序号':<4} {'测试项目':<40} {'结果':<10} {'说明'}")
        print("-" * 80)

        for i, r in enumerate(self.results, 1):
            status = f"{Colors.GREEN}PASS{Colors.ENDC}" if r["passed"] else f"{Colors.FAIL}FAIL{Colors.ENDC}"
            msg = r["message"][:30] if r["message"] else ""
            print(f"{i:<4} {r['test']:<40} {status:<10} {msg}")

        return passed, failed, total

    def run_all_tests(self):
        print_header("BMS完整功能测试")
        print(f"测试时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"服务器: {BASE_URL}")

        print("\n" + "="*70)
        print("第一阶段: 用户登录测试")
        print("="*70)

        self.test_admin_login()
        self.test_user1_login()
        self.test_user2_login()

        if not self.admin_token:
            print_error("Admin登录失败，无法继续测试")
            self.generate_report()
            return

        print("\n" + "="*70)
        print("第二阶段: Admin功能测试")
        print("="*70)

        self.test_admin_get_users()
        self.test_admin_get_devices()
        device_id = self.test_admin_create_device()

        print("\n" + "="*70)
        print("第三阶段: 普通用户功能测试")
        print("="*70)

        if self.user1_token:
            self.test_user_get_devices(self.user1_token, "User1")
            self.test_get_latest(self.user1_token, "User1")
            self.test_get_recent(self.user1_token, "User1")
            self.test_get_latest_by_uuid(self.user1_token, "User1", "550e8400-e29b-41d4-a716-446655440001")

        if self.user2_token:
            self.test_user_get_devices(self.user2_token, "User2")
            self.test_get_latest(self.user2_token, "User2")
            self.test_get_recent(self.user2_token, "User2")
            self.test_get_latest_by_uuid(self.user2_token, "User2", "550e8400-e29b-41d4-a716-446655440002")

        print("\n" + "="*70)
        print("第四阶段: 权限安全测试")
        print("="*70)

        self.test_unauthorized_access()
        self.test_invalid_token()

        passed, failed, total = self.generate_report()

        print(f"\n{Colors.BOLD}测试完成!{Colors.ENDC}")

        if failed == 0:
            print_success("所有测试通过!")
        else:
            print_error(f"{failed}个测试失败")

        return passed, failed, total

if __name__ == "__main__":
    test = BMSTest()
    test.run_all_tests()
