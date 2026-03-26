package com.example.bms.controller;

import com.example.bms.domain.Device;
import com.example.bms.domain.SysRole;
import com.example.bms.domain.SysUser;
import com.example.bms.mapper.SysRoleMapper;
import com.example.bms.mapper.SysUserMapper;
import com.example.bms.security.JwtUserDetails;
import com.example.bms.service.DeviceService;
import com.example.bms.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "管理")
@RequestMapping("/api/admin")
public class AdminController {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final DeviceService deviceService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AdminController(SysUserMapper userMapper, SysRoleMapper roleMapper, DeviceService deviceService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deviceService = deviceService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    private JwtUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtUserDetails) {
            return (JwtUserDetails) auth.getPrincipal();
        }
        return null;
    }

    private boolean isAdmin() {
        JwtUserDetails user = getCurrentUser();
        System.out.println("=== isAdmin check: user=" + (user != null ? user.getUsername() : "null"));
        return user != null && "admin".equals(user.getUsername());
    }

    @GetMapping("/users")
    @Operation(summary = "获取所有用户")
    public ResponseEntity<?> getAllUsers() {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        List<SysUser> users = userMapper.selectAll();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users")
    @Operation(summary = "创建用户")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        String username = request.get("username");
        String password = request.get("password");
        String nickname = request.get("nickname");
        
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }
        
        SysUser existing = userMapper.selectByUsername(username);
        if (existing != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }
        
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setStatus(1);
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        userMapper.insert(user);

        List<SysRole> userRoles = roleMapper.selectByRoleKey("user");
        if (userRoles != null && !userRoles.isEmpty()) {
            userMapper.insertUserRole(user.getId(), userRoles.get(0).getId());
        }

        return ResponseEntity.ok(Map.of("message", "用户创建成功", "userId", user.getId()));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "更新用户")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
        }
        if (request.containsKey("nickname")) {
            user.setNickname(request.get("nickname"));
        }
        if (request.containsKey("password") && !request.get("password").isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.get("password")));
        }
        if (request.containsKey("status")) {
            user.setStatus(Integer.parseInt(request.get("status")));
        }
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userMapper.update(user);
        return ResponseEntity.ok(Map.of("message", "用户更新成功"));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "删除用户")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        if (id == 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "不能删除超级管理员"));
        }
        userMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "用户删除成功"));
    }

    @GetMapping("/devices")
    @Operation(summary = "获取所有设备")
    public ResponseEntity<?> getAllDevices() {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @PostMapping("/devices")
    @Operation(summary = "创建设备")
    public ResponseEntity<?> createDevice(@RequestBody Map<String, String> request) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        Device device = new Device();
        String uuid = request.get("deviceUuid");
        if (uuid == null || uuid.isEmpty()) {
            uuid = java.util.UUID.randomUUID().toString();
        }
        device.setDeviceUuid(uuid);
        device.setDeviceName(request.getOrDefault("deviceName", "新建设备"));
        device.setDeviceType(request.getOrDefault("deviceType", "BMS"));
        device.setStatus(0);
        deviceService.addDevice(device);
        return ResponseEntity.ok(Map.of("message", "设备创建成功", "deviceId", device.getId(), "deviceUuid", uuid));
    }

    @PutMapping("/devices/{id}")
    @Operation(summary = "更新设备")
    public ResponseEntity<?> updateDevice(@PathVariable Long id, @RequestBody Map<String, String> request) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        Device device = deviceService.getDeviceById(id);
        if (device == null) {
            return ResponseEntity.status(404).body(Map.of("message", "设备不存在"));
        }
        if (request.containsKey("deviceName")) {
            device.setDeviceName(request.get("deviceName"));
        }
        if (request.containsKey("deviceType")) {
            device.setDeviceType(request.get("deviceType"));
        }
        deviceService.updateDevice(device);
        return ResponseEntity.ok(Map.of("message", "设备更新成功"));
    }

    @DeleteMapping("/devices/{id}")
    @Operation(summary = "删除设备")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id) {
        System.out.println("=== DELETE device called, id=" + id);
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        deviceService.deleteDevice(id);
        return ResponseEntity.ok(Map.of("message", "设备删除成功"));
    }

    @PostMapping("/devices/{deviceId}/bind/{userId}")
    @Operation(summary = "绑定设备到用户")
    public ResponseEntity<?> bindDevice(@PathVariable Long deviceId, @PathVariable Long userId) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        deviceService.bindDeviceToUser(deviceId, userId);
        return ResponseEntity.ok(Map.of("message", "设备绑定成功"));
    }

    @PostMapping("/devices/{deviceId}/unbind/{userId}")
    @Operation(summary = "解绑设备")
    public ResponseEntity<?> unbindDevice(@PathVariable Long deviceId, @PathVariable Long userId) {
        if (!isAdmin()) {
            return ResponseEntity.status(403).body(Map.of("message", "无权限"));
        }
        deviceService.unbindDeviceFromUser(userId, deviceId);
        return ResponseEntity.ok(Map.of("message", "设备解绑成功"));
    }
}
