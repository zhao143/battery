package com.example.bms.controller;

import com.example.bms.domain.BatteryData;
import com.example.bms.domain.Device;
import com.example.bms.security.JwtUserDetails;
import com.example.bms.service.BatteryService;
import com.example.bms.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "电池数据")
@RequestMapping("/api/battery")
public class BatteryController {
    private final BatteryService batteryService;
    private final DeviceService deviceService;

    public BatteryController(BatteryService batteryService, DeviceService deviceService) {
        this.batteryService = batteryService;
        this.deviceService = deviceService;
    }

    private JwtUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserDetails) {
            return (JwtUserDetails) auth.getPrincipal();
        }
        return null;
    }

    @GetMapping("/devices")
    @Operation(summary = "获取用户设备列表")
    public ResponseEntity<?> getUserDevices() {
        JwtUserDetails user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }
        
        // admin可以看到所有设备
        if ("admin".equals(user.getUsername())) {
            return ResponseEntity.ok(deviceService.getAllDevices());
        }
        
        List<Device> devices = deviceService.getDevicesByUserId(user.getUserId());
        return ResponseEntity.ok(devices);
    }

    @PostMapping("/devices")
    @Operation(summary = "创建设备（仅管理员）")
    public ResponseEntity<?> createDevice(@RequestBody Map<String, String> request) {
        JwtUserDetails user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        if (!"admin".equals(user.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("message", "只有管理员可以创建设备"));
        }

        String uuid = request.get("deviceUuid");
        if (uuid == null || uuid.isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
        } else {
            Device existingDevice = deviceService.getDeviceByUuid(uuid);
            if (existingDevice != null) {
                return ResponseEntity.status(409).body(Map.of("message", "该UUID已存在"));
            }
        }

        Device device = new Device();
        device.setDeviceUuid(uuid);
        device.setDeviceName(request.getOrDefault("deviceName", "新建设备"));
        device.setDeviceType(request.getOrDefault("deviceType", "BMS"));
        device.setStatus(0);
        device.setCreatedAt(java.time.LocalDateTime.now());
        device.setUpdatedAt(java.time.LocalDateTime.now());

        deviceService.addDevice(device);

        return ResponseEntity.ok(Map.of("message", "设备创建成功", "deviceId", device.getId(), "deviceUuid", uuid));
    }

    @PostMapping("/devices/bind")
    @Operation(summary = "通过UUID绑定已有设备")
    public ResponseEntity<?> bindDevice(@RequestBody Map<String, String> request) {
        JwtUserDetails user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        String uuid = request.get("deviceUuid");
        if (uuid == null || uuid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "设备UUID不能为空"));
        }

        Device existingDevice = deviceService.getDeviceByUuid(uuid);
        if (existingDevice == null) {
            return ResponseEntity.status(404).body(Map.of("message", "设备不存在，请联系管理员创建设备"));
        }

        if (existingDevice.getUserId() != null && existingDevice.getUserId().equals(user.getUserId())) {
            return ResponseEntity.status(409).body(Map.of("message", "该设备已绑定到您的账户", "deviceId", existingDevice.getId()));
        }

        if (existingDevice.getUserId() != null) {
            return ResponseEntity.status(409).body(Map.of("message", "该设备已被其他用户绑定"));
        }

        deviceService.bindDeviceToUser(user.getUserId(), existingDevice.getId());
        return ResponseEntity.ok(Map.of("message", "设备绑定成功", "deviceId", existingDevice.getId(), "deviceUuid", uuid));
    }

    @PutMapping("/devices/{deviceUuid}")
    @Operation(summary = "更新设备信息（仅设备所有者）")
    public ResponseEntity<?> updateDevice(@PathVariable String deviceUuid, @RequestBody Map<String, String> request) {
        JwtUserDetails user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        Device device = deviceService.getDeviceByUuid(deviceUuid);
        if (device == null) {
            return ResponseEntity.status(404).body(Map.of("message", "设备不存在"));
        }

        if (!"admin".equals(user.getUsername()) &&
            (device.getUserId() == null || !device.getUserId().equals(user.getUserId()))) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限修改该设备"));
        }

        String deviceName = request.get("deviceName");
        if (deviceName != null && !deviceName.isEmpty()) {
            device.setDeviceName(deviceName);
        }
        device.setUpdatedAt(java.time.LocalDateTime.now());

        deviceService.updateDevice(device);

        return ResponseEntity.ok(Map.of("message", "设备更新成功"));
    }

    @GetMapping("/latest")
    @Operation(summary = "获取最新电池数据(所有设备最新)")
    public ResponseEntity<?> getLatest() {
        return ResponseEntity.ok(batteryService.latest());
    }

    @GetMapping("/latest/{deviceUuid}")
    @Operation(summary = "获取指定设备最新数据")
    public ResponseEntity<?> getLatestByDevice(@PathVariable String deviceUuid) {
        JwtUserDetails user = getCurrentUser();
        if (!hasAccessToDevice(user, deviceUuid)) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限访问该设备"));
        }
        BatteryData data = batteryService.latestByUuid(deviceUuid);
        if (data == null) {
            return ResponseEntity.ok(Map.of("message", "暂无数据"));
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近电池数据列表")
    public ResponseEntity<?> getRecent(@RequestParam(name = "limit", defaultValue = "100") int limit) {
        try {
            return ResponseEntity.ok(batteryService.recent(limit));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/recent/{deviceUuid}")
    @Operation(summary = "获取指定设备最近数据")
    public ResponseEntity<?> getRecentByDevice(@PathVariable String deviceUuid,
                                               @RequestParam(name = "limit", defaultValue = "100") int limit) {
        JwtUserDetails user = getCurrentUser();
        if (!hasAccessToDevice(user, deviceUuid)) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限访问该设备"));
        }
        try {
            return ResponseEntity.ok(batteryService.recentByUuid(deviceUuid, limit));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/stream")
    @Operation(summary = "SSE实时数据流")
    public ResponseEntity<?> stream() {
        System.out.println("SSE stream called, returning emitter");
        return ResponseEntity.ok(batteryService.createEmitter());
    }

    @GetMapping("/stream/{deviceUuid}")
    @Operation(summary = "SSE实时数据流(指定设备)")
    public ResponseEntity<?> streamByDevice(@PathVariable String deviceUuid) {
        JwtUserDetails user = getCurrentUser();
        System.out.println("SSE stream for device: " + deviceUuid + ", user: " + (user != null ? user.getUsername() : "null"));
        
        if (!hasAccessToDevice(user, deviceUuid)) {
            System.out.println("No access to device: " + deviceUuid);
            return ResponseEntity.status(403).body(Map.of("message", "无权限访问该设备"));
        }
        return ResponseEntity.ok(batteryService.createEmitterByUuid(deviceUuid));
    }

    private boolean hasAccessToDevice(JwtUserDetails userDetails, String deviceUuid) {
        if (userDetails == null) {
            return false;
        }
        // admin可以访问所有设备
        if ("admin".equals(userDetails.getUsername())) {
            return true;
        }
        List<Device> devices = deviceService.getDevicesByUserId(userDetails.getUserId());
        if (devices != null) {
            for (Device d : devices) {
                if (d.getDeviceUuid() != null && d.getDeviceUuid().equals(deviceUuid)) {
                    return true;
                }
            }
        }
        return false;
    }
}
