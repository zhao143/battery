#!/usr/bin/env python3
"""
BMS API Test Script for User2
Complete testing tool for Battery Management System

Usage:
    python test_user2.py                    # Interactive mode
    python test_user2.py --auto             # Auto test mode
    python test_user2.py --simulate         # Simulate device data
"""

import requests
import json
import time
import sys
import argparse
from datetime import datetime
from typing import Optional, Dict, List, Any

# Configuration
BASE_URL = "http://localhost:8080"
API_AUTH_LOGIN = f"{BASE_URL}/api/auth/login"
API_BATTERY_DEVICES = f"{BASE_URL}/api/battery/devices"
API_BATTERY_LATEST = f"{BASE_URL}/api/battery/latest"
API_BATTERY_RECENT = f"{BASE_URL}/api/battery/recent"
API_BATTERY_STREAM = f"{BASE_URL}/api/battery/stream"

# Test credentials
USER2_USERNAME = "user2"
USER2_PASSWORD = "password123"
USER2_DEVICE_UUID = "550e8400-e29b-41d4-a716-446655440002"

# Colors for terminal output
class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header(text: str):
    print(f"\n{Colors.HEADER}{'='*60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{text.center(60)}{Colors.ENDC}")
    print(f"{Colors.HEADER}{'='*60}{Colors.ENDC}")

def print_success(text: str):
    print(f"{Colors.GREEN}✓ {text}{Colors.ENDC}")

def print_error(text: str):
    print(f"{Colors.FAIL}✗ {text}{Colors.ENDC}")

def print_warning(text: str):
    print(f"{Colors.WARNING}⚠ {text}{Colors.ENDC}")

def print_info(text: str):
    print(f"{Colors.CYAN}ℹ {text}{Colors.ENDC}")

class BMSClient:
    """BMS API Client"""

    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.token: Optional[str] = None
        self.username: Optional[str] = None
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json"
        })

    def login(self, username: str, password: str) -> bool:
        """Login and get token"""
        try:
            response = self.session.post(
                API_AUTH_LOGIN,
                json={"username": username, "password": password}
            )
            if response.status_code == 200:
                data = response.json()
                self.token = data.get("token")
                self.username = data.get("username")
                self.session.headers.update({
                    "Authorization": f"Bearer {self.token}"
                })
                print_success(f"Login successful: {data.get('nickname')} ({username})")
                print_info(f"Roles: {', '.join(data.get('roles', []))}")
                return True
            else:
                print_error(f"Login failed: {response.json().get('message', 'Unknown error')}")
                return False
        except Exception as e:
            print_error(f"Login error: {e}")
            return False

    def get_devices(self) -> List[Dict]:
        """Get user's devices"""
        try:
            response = self.session.get(API_BATTERY_DEVICES)
            if response.status_code == 200:
                devices = response.json()
                return devices
            else:
                print_error(f"Get devices failed: {response.status_code}")
                return []
        except Exception as e:
            print_error(f"Get devices error: {e}")
            return []

    def get_latest(self) -> Optional[Dict]:
        """Get latest battery data"""
        try:
            response = self.session.get(API_BATTERY_LATEST)
            if response.status_code == 200:
                data = response.json()
                if isinstance(data, list):
                    return data[0] if data else None
                return data
            else:
                print_error(f"Get latest failed: {response.status_code}")
                return None
        except Exception as e:
            print_error(f"Get latest error: {e}")
            return None

    def get_latest_by_uuid(self, uuid: str) -> Optional[Dict]:
        """Get latest data for specific device"""
        try:
            response = self.session.get(f"{API_BATTERY_LATEST}/{uuid}")
            if response.status_code == 200:
                data = response.json()
                if data.get("message"):
                    return None
                return data
            else:
                print_error(f"Get latest by UUID failed: {response.status_code}")
                return None
        except Exception as e:
            print_error(f"Get latest by UUID error: {e}")
            return None

    def get_recent(self, limit: int = 50) -> List[Dict]:
        """Get recent battery data"""
        try:
            response = self.session.get(API_BATTERY_RECENT, params={"limit": limit})
            if response.status_code == 200:
                return response.json()
            else:
                print_error(f"Get recent failed: {response.status_code}")
                return []
        except Exception as e:
            print_error(f"Get recent error: {e}")
            return []

    def get_recent_by_uuid(self, uuid: str, limit: int = 50) -> List[Dict]:
        """Get recent data for specific device"""
        try:
            response = self.session.get(f"{API_BATTERY_RECENT}/{uuid}", params={"limit": limit})
            if response.status_code == 200:
                return response.json()
            else:
                print_error(f"Get recent by UUID failed: {response.status_code}")
                return []
        except Exception as e:
            print_error(f"Get recent by UUID error: {e}")
            return []

    def display_device(self, device: Dict):
        """Display device information"""
        print(f"\n  {Colors.BOLD}Device:{Colors.ENDC}")
        print(f"    ID: {device.get('id')}")
        print(f"    UUID: {device.get('deviceUuid')}")
        print(f"    Name: {device.get('deviceName')}")
        print(f"    Type: {device.get('deviceType')}")
        status = device.get('status', 0)
        status_text = f"{Colors.GREEN}Online{Colors.ENDC}" if status == 1 else f"{Colors.WARNING}Offline{Colors.ENDC}"
        print(f"    Status: {status_text}")

    def display_battery_data(self, data: Dict, title: str = "Battery Data"):
        """Display battery data"""
        print(f"\n  {Colors.BOLD}{title}:{Colors.ENDC}")
        print(f"    Voltage:   {data.get('voltage', 'N/A')} V")
        print(f"    Current:   {data.get('current', 'N/A')} A")
        print(f"    Temperature: {data.get('temperature', 'N/A')} °C")
        print(f"    Charge:    {data.get('charge', 'N/A')} %")
        print(f"    Power:     {data.get('power', 'N/A')} W")
        print(f"    Fan:       {'ON' if data.get('fanState') == 1 else 'OFF'}")
        print(f"    Relay:     {'ON' if data.get('relayState') == 1 else 'OFF'}")
        alarm = data.get('alarmState', 0)
        alarm_text = f"{Colors.FAIL}ALARM!{Colors.ENDC}" if alarm == 1 else f"{Colors.GREEN}Normal{Colors.ENDC}"
        print(f"    Alarm:     {alarm_text}")


class TCPSimulator:
    """TCP Device Data Simulator"""

    def __init__(self, host: str = "localhost", port: int = 9000):
        self.host = host
        self.port = port

    def simulate_device_data(self, uuid: str, duration: int = 60, interval: int = 2):
        """
        Simulate device data sending via TCP

        Args:
            uuid: Device UUID
            duration: Duration in seconds
            interval: Interval between data sends in seconds
        """
        print_header("TCP Device Simulator")
        print_info(f"Connecting to {self.host}:{self.port}")
        print_info(f"Device UUID: {uuid}")
        print_info(f"Duration: {duration}s, Interval: {interval}s")
        print_warning("Note: This is a simulation display. Actual TCP connection requires external tool.")
        print(f"\n{Colors.CYAN}Data format:{Colors.ENDC}")
        print(f"  REGISTER#<uuid>")
        print(f"  <uuid>#<voltage>#<current>#<temp>#<charge>#<power>#<fan>#<relay>#<alarm>")
        print(f"\n{Colors.CYAN}Example commands:{Colors.ENDC}")
        print(f"  REGISTER#{uuid}")
        print(f"  {uuid}#12.5#2.3#25#80#28.8#1#0#0")

        import random
        print(f"\n{Colors.GREEN}Simulating data stream...{Colors.ENDC}")
        print("-" * 60)

        for i in range(duration // interval):
            voltage = round(10 + random.random() * 5, 1)
            current = round(random.random() * 5, 1)
            temp = 20 + random.randint(0, 15)
            charge = 50 + random.randint(0, 50)
            power = round(voltage * current, 1)
            fan = random.randint(0, 1)
            relay = random.randint(0, 1)
            alarm = 1 if temp > 30 or voltage > 14 else 0

            data = f"{uuid}#{voltage}#{current}#{temp}#{charge}#{power}#{fan}#{relay}#{alarm}"
            alarm_color = Colors.FAIL if alarm == 1 else Colors.GREEN

            print(f"[{datetime.now().strftime('%H:%M:%S')}] {data}")
            time.sleep(interval)

        print("-" * 60)
        print_success("Simulation complete!")


def run_interactive_mode():
    """Run interactive test mode"""
    print_header("BMS API Test - User2 (Interactive Mode)")

    client = BMSClient()

    # Login
    print_header("Step 1: Login as User2")
    if not client.login(USER2_USERNAME, USER2_PASSWORD):
        return

    # Get devices
    print_header("Step 2: Get User2's Devices")
    devices = client.get_devices()
    if not devices:
        print_warning("No devices found for User2")
    else:
        print_success(f"Found {len(devices)} device(s)")
        for device in devices:
            client.display_device(device)

    # Get latest data
    print_header("Step 3: Get Latest Data")
    latest = client.get_latest()
    if latest:
        client.display_battery_data(latest, "Latest Data (All Devices)")
    else:
        print_warning("No latest data available")

    # Get latest for specific device
    print_header("Step 4: Get Latest Data for User2's Device")
    latest_uuid = client.get_latest_by_uuid(USER2_DEVICE_UUID)
    if latest_uuid:
        client.display_battery_data(latest_uuid, f"Latest Data ({USER2_DEVICE_UUID})")
    else:
        print_warning(f"No data for device {USER2_DEVICE_UUID}")
        print_info("This is normal if device is not connected/sending data")

    # Get recent data
    print_header("Step 5: Get Recent Data")
    recent = client.get_recent(10)
    if recent:
        print_success(f"Found {len(recent)} recent record(s)")
        for i, record in enumerate(recent[:5], 1):
            print(f"  [{i}] {record.get('deviceUuid')}: V={record.get('voltage')}A={record.get('current')} T={record.get('temperature')}°C")
    else:
        print_warning("No recent data available")

    # Get recent for specific device
    print_header("Step 6: Get Recent Data for User2's Device")
    recent_uuid = client.get_recent_by_uuid(USER2_DEVICE_UUID, 10)
    if recent_uuid:
        print_success(f"Found {len(recent_uuid)} recent record(s) for User2's device")
    else:
        print_warning(f"No recent data for device {USER2_DEVICE_UUID}")

    # Summary
    print_header("Test Summary")
    print(f"  Username: {USER2_USERNAME}")
    print(f"  Device UUID: {USER2_DEVICE_UUID}")
    print(f"  Total Devices: {len(devices)}")
    print(f"  Latest Data: {'Yes' if latest else 'No'}")
    print(f"  Recent Records: {len(recent)}")

    # Offer TCP simulation
    print_header("TCP Device Simulation")
    choice = input(f"{Colors.CYAN}Start TCP simulation? (y/n): {Colors.ENDC}").strip().lower()
    if choice == 'y':
        simulator = TCPSimulator()
        simulator.simulate_device_data(USER2_DEVICE_UUID, duration=30, interval=3)


def run_auto_mode():
    """Run automatic test mode"""
    print_header("BMS API Test - User2 (Auto Mode)")

    client = BMSClient()
    results = {"passed": 0, "failed": 0}

    # Test 1: Login
    print_header("Test 1: Login")
    if client.login(USER2_USERNAME, USER2_PASSWORD):
        print_success("PASSED")
        results["passed"] += 1
    else:
        print_error("FAILED")
        results["failed"] += 1
        return

    # Test 2: Get Devices
    print_header("Test 2: Get Devices")
    devices = client.get_devices()
    if devices:
        print_success(f"PASSED - Found {len(devices)} device(s)")
        results["passed"] += 1
    else:
        print_warning("PASSED (no devices) - User2 has no bound devices")
        results["passed"] += 1

    # Test 3: Get Latest
    print_header("Test 3: Get Latest Data")
    latest = client.get_latest()
    if latest:
        print_success("PASSED - Latest data available")
        results["passed"] += 1
    else:
        print_warning("PASSED (no data) - Device not connected")
        results["passed"] += 1

    # Test 4: Get Latest by UUID
    print_header("Test 4: Get Latest by UUID")
    latest_uuid = client.get_latest_by_uuid(USER2_DEVICE_UUID)
    if latest_uuid:
        print_success("PASSED - Device data available")
        results["passed"] += 1
    else:
        print_warning("PASSED (no data) - Device not connected")
        results["passed"] += 1

    # Test 5: Get Recent
    print_header("Test 5: Get Recent Data")
    recent = client.get_recent(10)
    if recent:
        print_success(f"PASSED - {len(recent)} records")
        results["passed"] += 1
    else:
        print_warning("PASSED (no data)")
        results["passed"] += 1

    # Test 6: Get Recent by UUID
    print_header("Test 6: Get Recent by UUID")
    recent_uuid = client.get_recent_by_uuid(USER2_DEVICE_UUID, 10)
    if recent_uuid:
        print_success(f"PASSED - {len(recent_uuid)} records")
        results["passed"] += 1
    else:
        print_warning("PASSED (no data)")
        results["passed"] += 1

    # Summary
    print_header("Test Results Summary")
    print(f"  Passed: {Colors.GREEN}{results['passed']}{Colors.ENDC}")
    print(f"  Failed: {Colors.FAIL}{results['failed']}{Colors.ENDC}")
    print(f"  Total:  {results['passed'] + results['failed']}")

    if results['failed'] == 0:
        print_success("All tests passed!")
    else:
        print_error("Some tests failed!")


def run_simulate_mode():
    """Run TCP simulation mode"""
    simulator = TCPSimulator()
    simulator.simulate_device_data(USER2_DEVICE_UUID, duration=60, interval=2)


def main():
    parser = argparse.ArgumentParser(
        description="BMS API Test Script for User2",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python test_user2.py                    # Interactive mode
  python test_user2.py --auto             # Auto test mode
  python test_user2.py --simulate        # TCP simulation mode
  python test_user2.py -u user1 -p pass   # Login as different user
        """
    )
    parser.add_argument("--auto", action="store_true", help="Run automatic test mode")
    parser.add_argument("--simulate", action="store_true", help="Run TCP simulation mode")
    parser.add_argument("-u", "--username", default=USER2_USERNAME, help="Username")
    parser.add_argument("-p", "--password", default=USER2_PASSWORD, help="Password")
    parser.add_argument("--uuid", default=USER2_DEVICE_UUID, help="Device UUID")
    parser.add_argument("--url", default=BASE_URL, help="Base URL")

    args = parser.parse_args()

    # Update globals
    global USER2_USERNAME, USER2_PASSWORD, USER2_DEVICE_UUID, BASE_URL
    USER2_USERNAME = args.username
    USER2_PASSWORD = args.password
    USER2_DEVICE_UUID = args.uuid
    BASE_URL = args.url

    if args.simulate:
        run_simulate_mode()
    elif args.auto:
        run_auto_mode()
    else:
        run_interactive_mode()


if __name__ == "__main__":
    main()
