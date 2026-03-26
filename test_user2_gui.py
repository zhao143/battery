#!/usr/bin/env python3
"""
BMS API Test Script with GUI
Complete testing tool for Battery Management System with Graphical User Interface

Usage:
    python test_user2_gui.py
"""

import requests
import json
import time
import threading
import socket
import argparse
from datetime import datetime
from typing import Optional, Dict, List
from tkinter import *
from tkinter import ttk, messagebox, scrolledtext

# Configuration
BASE_URL = "http://localhost:8080"
API_AUTH_LOGIN = f"{BASE_URL}/api/auth/login"
API_BATTERY_DEVICES = f"{BASE_URL}/api/battery/devices"
API_BATTERY_LATEST = f"{BASE_URL}/api/battery/latest"
API_BATTERY_RECENT = f"{BASE_URL}/api/battery/recent"

# Test credentials
DEFAULT_USERNAME = "user2"
DEFAULT_PASSWORD = "password123"
DEFAULT_DEVICE_UUID = "550e8400-e29b-41d4-a716-446655440002"


class BMSGui:
    """BMS GUI Application"""

    def __init__(self, root):
        self.root = root
        self.root.title("BMS API 测试工具 - User2")
        self.root.geometry("900x700")
        self.root.resizable(True, True)

        self.token = None
        self.username = None
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

        self.setup_ui()

    def setup_ui(self):
        """Setup GUI components"""
        # Style
        style = ttk.Style()
        style.theme_use('clam')
        style.configure('TButton', font=('Arial', 10))
        style.configure('TLabel', font=('Arial', 10))
        style.configure('Header.TLabel', font=('Arial', 12, 'bold'))

        # Main container
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(N, S, E, W))
        self.root.columnconfigure(0, weight=1)
        self.root.rowconfigure(0, weight=1)

        # ===== Login Section =====
        login_frame = ttk.LabelFrame(main_frame, text="登录", padding="10")
        login_frame.grid(row=0, column=0, columnspan=2, sticky=(E, W), pady=5)

        ttk.Label(login_frame, text="用户名:").grid(row=0, column=0, sticky=W, padx=5)
        self.entry_username = ttk.Entry(login_frame, width=20)
        self.entry_username.insert(0, DEFAULT_USERNAME)
        self.entry_username.grid(row=0, column=1, padx=5)

        ttk.Label(login_frame, text="密码:").grid(row=0, column=2, sticky=W, padx=5)
        self.entry_password = ttk.Entry(login_frame, width=20, show="*")
        self.entry_password.insert(0, DEFAULT_PASSWORD)
        self.entry_password.grid(row=0, column=3, padx=5)

        ttk.Label(login_frame, text="设备UUID:").grid(row=0, column=4, sticky=W, padx=5)
        self.entry_uuid = ttk.Entry(login_frame, width=35)
        self.entry_uuid.insert(0, DEFAULT_DEVICE_UUID)
        self.entry_uuid.grid(row=0, column=5, padx=5)

        self.btn_login = ttk.Button(login_frame, text="登录", command=self.login)
        self.btn_login.grid(row=0, column=6, padx=10)

        self.lbl_status = ttk.Label(login_frame, text="未登录", foreground="red")
        self.lbl_status.grid(row=0, column=7, padx=5)

        # ===== Tab Control =====
        self.notebook = ttk.Notebook(main_frame)
        self.notebook.grid(row=1, column=0, columnspan=2, sticky=(N, S, E, W), pady=5)

        main_frame.columnconfigure(0, weight=1)
        main_frame.rowconfigure(1, weight=1)

        # ===== Tab 1: 设备管理 =====
        self.tab_devices = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_devices, text="设备管理")
        self.setup_devices_tab()

        # ===== Tab 2: 最新数据 =====
        self.tab_latest = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_latest, text="最新数据")
        self.setup_latest_tab()

        # ===== Tab 3: 历史数据 =====
        self.tab_history = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_history, text="历史数据")
        self.setup_history_tab()

        # ===== Tab 4: TCP模拟 =====
        self.tab_tcp = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_tcp, text="TCP模拟")
        self.setup_tcp_tab()

        # ===== Tab 5: 日志 =====
        self.tab_log = ttk.Frame(self.notebook)
        self.notebook.add(self.tab_log, text="日志")
        self.setup_log_tab()

        # ===== Bottom Buttons =====
        btn_frame = ttk.Frame(main_frame)
        btn_frame.grid(row=2, column=0, columnspan=2, sticky=E, pady=5)

        ttk.Button(btn_frame, text="刷新所有", command=self.refresh_all).pack(side=LEFT, padx=5)
        ttk.Button(btn_frame, text="清除日志", command=self.clear_log).pack(side=LEFT, padx=5)

    def setup_devices_tab(self):
        """Setup devices tab"""
        frame = ttk.Frame(self.tab_devices, padding="5")
        frame.grid(row=0, column=0, sticky=(N, S, E, W))
        self.tab_devices.columnconfigure(0, weight=1)
        self.tab_devices.rowconfigure(0, weight=1)

        # Treeview
        columns = ('id', 'uuid', 'name', 'type', 'status')
        self.devices_tree = ttk.Treeview(frame, columns=columns, show='headings', height=15)

        self.devices_tree.heading('id', text='ID')
        self.devices_tree.heading('uuid', text='设备UUID')
        self.devices_tree.heading('name', text='设备名称')
        self.devices_tree.heading('type', text='设备类型')
        self.devices_tree.heading('status', text='状态')

        self.devices_tree.column('id', width=50)
        self.devices_tree.column('uuid', width=300)
        self.devices_tree.column('name', width=150)
        self.devices_tree.column('type', width=100)
        self.devices_tree.column('status', width=80)

        scrollbar = ttk.Scrollbar(frame, orient=VERTICAL, command=self.devices_tree.yview)
        self.devices_tree.configure(yscroll=scrollbar.set)

        self.devices_tree.grid(row=0, column=0, sticky=(N, S, E, W))
        scrollbar.grid(row=0, column=1, sticky=(N, S))

        btn_frame = ttk.Frame(frame)
        btn_frame.grid(row=1, column=0, sticky=W, pady=5)
        ttk.Button(btn_frame, text="刷新设备", command=self.get_devices).pack(side=LEFT, padx=5)

    def setup_latest_tab(self):
        """Setup latest data tab"""
        frame = ttk.Frame(self.tab_latest, padding="5")
        frame.grid(row=0, column=0, sticky=(N, S, E, W))
        self.tab_latest.columnconfigure(0, weight=1)
        self.tab_latest.rowconfigure(0, weight=1)

        # Info display
        info_frame = ttk.LabelFrame(frame, text="最新电池数据", padding="10")
        info_frame.grid(row=0, column=0, sticky=(N, S, E, W))
        frame.rowconfigure(0, weight=1)
        frame.columnconfigure(0, weight=1)

        self.lbl_voltage = ttk.Label(info_frame, text="电压: -- V", font=('Arial', 14))
        self.lbl_voltage.grid(row=0, column=0, padx=20, pady=10, sticky=W)

        self.lbl_current = ttk.Label(info_frame, text="电流: -- A", font=('Arial', 14))
        self.lbl_current.grid(row=0, column=1, padx=20, pady=10, sticky=W)

        self.lbl_temp = ttk.Label(info_frame, text="温度: -- °C", font=('Arial', 14))
        self.lbl_temp.grid(row=1, column=0, padx=20, pady=10, sticky=W)

        self.lbl_charge = ttk.Label(info_frame, text="电量: -- %", font=('Arial', 14))
        self.lbl_charge.grid(row=1, column=1, padx=20, pady=10, sticky=W)

        self.lbl_power = ttk.Label(info_frame, text="功率: -- W", font=('Arial', 14))
        self.lbl_power.grid(row=2, column=0, padx=20, pady=10, sticky=W)

        self.lbl_alarm = ttk.Label(info_frame, text="报警: --", font=('Arial', 14))
        self.lbl_alarm.grid(row=2, column=1, padx=20, pady=10, sticky=W)

        btn_frame = ttk.Frame(frame)
        btn_frame.grid(row=1, column=0, sticky=W, pady=5)
        ttk.Button(btn_frame, text="刷新数据", command=self.get_latest).pack(side=LEFT, padx=5)

    def setup_history_tab(self):
        """Setup history tab"""
        frame = ttk.Frame(self.tab_history, padding="5")
        frame.grid(row=0, column=0, sticky=(N, S, E, W))
        self.tab_history.columnconfigure(0, weight=1)
        self.tab_history.rowconfigure(0, weight=1)

        # Treeview
        columns = ('time', 'uuid', 'voltage', 'current', 'temp', 'charge', 'power', 'alarm')
        self.history_tree = ttk.Treeview(frame, columns=columns, show='headings', height=18)

        self.history_tree.heading('time', text='时间')
        self.history_tree.heading('uuid', text='设备UUID')
        self.history_tree.heading('voltage', text='电压')
        self.history_tree.heading('current', text='电流')
        self.history_tree.heading('temp', text='温度')
        self.history_tree.heading('charge', text='电量')
        self.history_tree.heading('power', text='功率')
        self.history_tree.heading('alarm', text='报警')

        self.history_tree.column('time', width=150)
        self.history_tree.column('uuid', width=250)
        self.history_tree.column('voltage', width=80)
        self.history_tree.column('current', width=80)
        self.history_tree.column('temp', width=80)
        self.history_tree.column('charge', width=80)
        self.history_tree.column('power', width=80)
        self.history_tree.column('alarm', width=60)

        scrollbar = ttk.Scrollbar(frame, orient=VERTICAL, command=self.history_tree.yview)
        self.history_tree.configure(yscroll=scrollbar.set)

        self.history_tree.grid(row=0, column=0, sticky=(N, S, E, W))
        scrollbar.grid(row=0, column=1, sticky=(N, S))

        btn_frame = ttk.Frame(frame)
        btn_frame.grid(row=1, column=0, sticky=W, pady=5)
        ttk.Button(btn_frame, text="刷新历史", command=self.get_history).pack(side=LEFT, padx=5)
        ttk.Label(btn_frame, text="记录数:").pack(side=LEFT, padx=5)
        self.spin_limit = Spinbox(btn_frame, from_=10, to=500, width=5)
        self.spin_limit.delete(0, END)
        self.spin_limit.insert(0, "50")
        self.spin_limit.pack(side=LEFT)

    def setup_tcp_tab(self):
        """Setup TCP simulation tab"""
        frame = ttk.Frame(self.tab_tcp, padding="10")
        frame.grid(row=0, column=0, sticky=(N, S, E, W))
        self.tab_tcp.columnconfigure(0, weight=1)
        self.tab_tcp.rowconfigure(0, weight=1)

        # Info
        info_frame = ttk.LabelFrame(frame, text="TCP设备模拟", padding="10")
        info_frame.grid(row=0, column=0, sticky=(N, S, E, W), pady=10)
        frame.rowconfigure(0, weight=1)
        frame.columnconfigure(0, weight=1)

        ttk.Label(info_frame, text="此功能用于模拟设备连接和数据传输", font=('Arial', 11)).grid(row=0, column=0, columnspan=2, pady=10)

        ttk.Label(info_frame, text="TCP服务器:").grid(row=1, column=0, sticky=W, pady=5)
        self.entry_tcp_host = ttk.Entry(info_frame, width=20)
        self.entry_tcp_host.insert(0, "localhost")
        self.entry_tcp_host.grid(row=1, column=1, sticky=W, pady=5)

        ttk.Label(info_frame, text="端口:").grid(row=2, column=0, sticky=W, pady=5)
        self.entry_tcp_port = ttk.Entry(info_frame, width=20)
        self.entry_tcp_port.insert(0, "9000")
        self.entry_tcp_port.grid(row=2, column=1, sticky=W, pady=5)

        ttk.Label(info_frame, text="模拟时长(秒):").grid(row=3, column=0, sticky=W, pady=5)
        self.entry_duration = ttk.Entry(info_frame, width=20)
        self.entry_duration.insert(0, "60")
        self.entry_duration.grid(row=3, column=1, sticky=W, pady=5)

        ttk.Label(info_frame, text="发送间隔(秒):").grid(row=4, column=0, sticky=W, pady=5)
        self.entry_interval = ttk.Entry(info_frame, width=20)
        self.entry_interval.insert(0, "2")
        self.entry_interval.grid(row=4, column=1, sticky=W, pady=5)

        btn_frame = ttk.Frame(frame)
        btn_frame.grid(row=1, column=0, sticky=W, pady=10)

        self.btn_start_sim = ttk.Button(btn_frame, text="开始模拟", command=self.start_simulation)
        self.btn_start_sim.pack(side=LEFT, padx=5)

        self.btn_stop_sim = ttk.Button(btn_frame, text="停止", command=self.stop_simulation, state=DISABLED)
        self.btn_stop_sim.pack(side=LEFT, padx=5)

        # Output
        self.tcp_output = scrolledtext.ScrolledText(frame, height=15, width=80, font=('Consolas', 9))
        self.tcp_output.grid(row=2, column=0, sticky=(N, S, E, W), pady=10)
        frame.rowconfigure(2, weight=1)

        self.simulation_running = False
        self.simulation_thread = None

    def setup_log_tab(self):
        """Setup log tab"""
        frame = ttk.Frame(self.tab_log, padding="5")
        frame.grid(row=0, column=0, sticky=(N, S, E, W))
        self.tab_log.columnconfigure(0, weight=1)
        self.tab_log.rowconfigure(0, weight=1)

        self.log_text = scrolledtext.ScrolledText(frame, height=25, width=100, font=('Consolas', 9))
        self.log_text.grid(row=0, column=0, sticky=(N, S, E, W))

    # ===== Helper Methods =====

    def log(self, message, tag="info"):
        """Add log message"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.insert(END, f"[{timestamp}] {message}\n")
        self.log_text.see(END)
        self.tcp_output.insert(END, f"[{timestamp}] {message}\n")
        self.tcp_output.see(END)

    def clear_log(self):
        """Clear log"""
        self.log_text.delete(1.0, END)

    # ===== API Methods =====

    def login(self):
        """Login"""
        username = self.entry_username.get()
        password = self.entry_password.get()
        uuid = self.entry_uuid.get()

        if not username or not password:
            messagebox.showerror("错误", "请输入用户名和密码")
            return

        try:
            response = self.session.post(
                API_AUTH_LOGIN,
                json={"username": username, "password": password}
            )
            if response.status_code == 200:
                data = response.json()
                self.token = data.get("token")
                self.username = data.get("username")
                self.session.headers.update({"Authorization": f"Bearer {self.token}"})

                self.lbl_status.config(text=f"已登录: {data.get('nickname')}", foreground="green")
                self.log(f"登录成功: {username}, 角色: {data.get('roles')}")

                self.get_devices()
                self.get_latest()
                self.get_history()

                messagebox.showinfo("成功", f"登录成功!\n用户名: {data.get('nickname')}\n角色: {', '.join(data.get('roles', []))}")
            else:
                error_msg = response.json().get("message", "登录失败")
                messagebox.showerror("登录失败", error_msg)
                self.log(f"登录失败: {error_msg}", "error")
        except Exception as e:
            messagebox.showerror("错误", f"连接失败: {e}")
            self.log(f"连接错误: {e}", "error")

    def get_devices(self):
        """Get devices"""
        if not self.token:
            self.log("未登录，无法获取设备", "warning")
            return

        try:
            response = self.session.get(API_BATTERY_DEVICES)
            if response.status_code == 200:
                devices = response.json()
                self.log(f"获取设备成功: {len(devices)} 个设备")

                # Clear tree
                for item in self.devices_tree.get_children():
                    self.devices_tree.delete(item)

                # Add devices
                for device in devices:
                    status = "在线" if device.get('status') == 1 else "离线"
                    self.devices_tree.insert('', END, values=(
                        device.get('id'),
                        device.get('deviceUuid'),
                        device.get('deviceName'),
                        device.get('deviceType'),
                        status
                    ))
            else:
                self.log(f"获取设备失败: {response.status_code}", "error")
        except Exception as e:
            self.log(f"获取设备错误: {e}", "error")

    def get_latest(self):
        """Get latest data"""
        if not self.token:
            self.log("未登录，无法获取数据", "warning")
            return

        uuid = self.entry_uuid.get()
        url = f"{API_BATTERY_LATEST}/{uuid}" if uuid else API_BATTERY_LATEST

        try:
            response = self.session.get(url)
            if response.status_code == 200:
                data = response.json()
                if isinstance(data, list):
                    data = data[0] if data else {}

                if data.get("message"):
                    self.log("暂无数据，请确保设备已连接", "warning")
                    self.lbl_voltage.config(text="电压: -- V")
                    self.lbl_current.config(text="电流: -- A")
                    self.lbl_temp.config(text="温度: -- °C")
                    self.lbl_charge.config(text="电量: -- %")
                    self.lbl_power.config(text="功率: -- W")
                    self.lbl_alarm.config(text="报警: --")
                else:
                    self.log(f"获取最新数据成功: {data.get('deviceUuid')}")

                    voltage = data.get('voltage', '--')
                    current = data.get('current', '--')
                    temp = data.get('temperature', '--')
                    charge = data.get('charge', '--')
                    power = data.get('power', '--')
                    alarm = "正常" if data.get('alarmState') == 0 else "报警!"

                    self.lbl_voltage.config(text=f"电压: {voltage} V")
                    self.lbl_current.config(text=f"电流: {current} A")
                    self.lbl_temp.config(text=f"温度: {temp} °C")
                    self.lbl_charge.config(text=f"电量: {charge} %")
                    self.lbl_power.config(text=f"功率: {power} W")
                    self.lbl_alarm.config(text=f"报警: {alarm}", foreground="red" if alarm == "报警!" else "green")
            else:
                self.log(f"获取数据失败: {response.status_code}", "error")
        except Exception as e:
            self.log(f"获取数据错误: {e}", "error")

    def get_history(self):
        """Get history data"""
        if not self.token:
            self.log("未登录，无法获取历史数据", "warning")
            return

        uuid = self.entry_uuid.get()
        limit = int(self.spin_limit.get())

        if uuid:
            url = f"{API_BATTERY_RECENT}/{uuid}?limit={limit}"
        else:
            url = f"{API_BATTERY_RECENT}?limit={limit}"

        try:
            response = self.session.get(url)
            if response.status_code == 200:
                data = response.json()
                self.log(f"获取历史数据成功: {len(data)} 条记录")

                # Clear tree
                for item in self.history_tree.get_children():
                    self.history_tree.delete(item)

                # Add records
                for record in data:
                    time_str = record.get('createdAt', '')
                    if time_str:
                        try:
                            dt = datetime.fromisoformat(time_str.replace('Z', '+00:00'))
                            time_str = dt.strftime("%Y-%m-%d %H:%M:%S")
                        except:
                            pass

                    alarm = "是" if record.get('alarmState') == 1 else "否"
                    self.history_tree.insert('', END, values=(
                        time_str,
                        record.get('deviceUuid', ''),
                        f"{record.get('voltage', '')} V",
                        f"{record.get('current', '')} A",
                        f"{record.get('temperature', '')} °C",
                        f"{record.get('charge', '')} %",
                        f"{record.get('power', '')} W",
                        alarm
                    ))
            else:
                self.log(f"获取历史数据失败: {response.status_code}", "error")
        except Exception as e:
            self.log(f"获取历史数据错误: {e}", "error")

    def refresh_all(self):
        """Refresh all data"""
        self.get_devices()
        self.get_latest()
        self.get_history()
        self.log("刷新完成")

    # ===== Simulation =====

    def start_simulation(self):
        """Start TCP simulation"""
        if not self.token:
            messagebox.showwarning("警告", "请先登录")
            return

        uuid = self.entry_uuid.get()
        if not uuid:
            messagebox.showwarning("警告", "请输入设备UUID")
            return

        self.simulation_running = True
        self.btn_start_sim.config(state=DISABLED)
        self.btn_stop_sim.config(state=NORMAL)

        self.log("开始TCP模拟...")

        def run():
            import random
            import socket
            try:
                host = self.entry_tcp_host.get() or "localhost"
                port = int(self.entry_tcp_port.get() or 9000)
                duration = int(self.entry_duration.get() or 60)
                interval = int(self.entry_interval.get() or 2)

                self.log(f"模拟参数: UUID={uuid}, Host={host}, Port={port}")
                self.log(f"正在连接到 {host}:{port}...")

                client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                client_socket.connect((host, port))
                self.log("连接成功!")

                start_time = time.time()
                while self.simulation_running and (time.time() - start_time) < duration:
                    voltage = round(10 + random.random() * 5, 1)
                    current = round(random.random() * 5, 1)
                    temp = 20 + random.randint(0, 15)
                    charge = 50 + random.randint(0, 50)
                    power = round(voltage * current, 1)
                    fan = random.randint(0, 1)
                    relay = random.randint(0, 1)
                    alarm = 1 if temp > 30 or voltage > 14 else 0

                    data = f"{uuid}#{voltage}#{current}#{temp}#{charge}#{power}#{fan}#{relay}#{alarm}"
                    
                    client_socket.send((data + "\n").encode('utf-8'))
                    self.log(f"发送: {data}")
                    
                    try:
                        client_socket.settimeout(1.0)
                        response = client_socket.recv(1024).decode('utf-8').strip()
                        if response:
                            self.log(f"响应: {response}")
                    except:
                        pass

                    time.sleep(interval)

                client_socket.close()
                self.log("模拟完成!")
            except Exception as e:
                self.log(f"模拟错误: {e}", "error")
            finally:
                self.simulation_running = False
                self.btn_start_sim.config(state=NORMAL)
                self.btn_stop_sim.config(state=DISABLED)

        self.simulation_thread = threading.Thread(target=run, daemon=True)
        self.simulation_thread.start()

    def stop_simulation(self):
        """Stop simulation"""
        self.simulation_running = False
        self.log("停止模拟...")


def main():
    root = Tk()
    app = BMSGui(root)
    root.mainloop()


if __name__ == "__main__":
    main()
