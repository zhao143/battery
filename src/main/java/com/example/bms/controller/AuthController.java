package com.example.bms.controller;

import com.example.bms.domain.SysRole;
import com.example.bms.domain.SysUser;
import com.example.bms.mapper.SysRoleMapper;
import com.example.bms.mapper.SysUserMapper;
import com.example.bms.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "认证")
@RequestMapping("/api/auth")
public class AuthController {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(SysUserMapper userMapper, SysRoleMapper roleMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        SysUser user = userMapper.selectByUsername(username);
        if (user == null || user.getStatus() != 1) {
            return ResponseEntity.status(401).body(Map.of("message", "用户不存在或已禁用"));
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "密码错误"));
        }

        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        System.out.println("=== Login ===");
        System.out.println("User: " + username + " (id=" + user.getId() + ")");
        System.out.println("Roles: " + roles);

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("userId", user.getId());
        result.put("roles", roles.stream().map(SysRole::getRoleKey).toList());

        System.out.println("Returning roles: " + roles.stream().map(SysRole::getRoleKey).toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ResponseEntity<?> register(@RequestBody Map<String, String> registerRequest) {
        String username = registerRequest.get("username");
        String password = registerRequest.get("password");
        String nickname = registerRequest.getOrDefault("nickname", username);
        String email = registerRequest.get("email");
        String phone = registerRequest.get("phone");

        if (username == null || username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名不能为空"));
        }
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码不能为空"));
        }
        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码长度不能少于6位"));
        }
        if (email != null && !email.isEmpty()) {
            SysUser byEmail = userMapper.selectByEmail(email);
            if (byEmail != null) {
                return ResponseEntity.status(409).body(Map.of("message", "邮箱已被使用"));
            }
        }
        if (phone != null && !phone.isEmpty()) {
            SysUser byPhone = userMapper.selectByPhone(phone);
            if (byPhone != null) {
                return ResponseEntity.status(409).body(Map.of("message", "电话已被使用"));
            }
        }

        SysUser existingUser = userMapper.selectByUsername(username);
        if (existingUser != null) {
            return ResponseEntity.status(409).body(Map.of("message", "用户名已存在"));
        }

        SysUser newUser = new SysUser();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setNickname(nickname);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setStatus(1);
        newUser.setCreatedAt(java.time.LocalDateTime.now());
        newUser.setUpdatedAt(java.time.LocalDateTime.now());

        userMapper.insert(newUser);

        List<SysRole> userRoles = roleMapper.selectByRoleKey("user");
        if (userRoles != null && !userRoles.isEmpty()) {
            userMapper.insertUserRole(newUser.getId(), userRoles.get(0).getId());
        }

        return ResponseEntity.ok(Map.of("message", "注册成功", "userId", newUser.getId()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("message", "token无效"));
        }
        String username = jwtUtil.getUsernameFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
        }
        List<SysRole> roles = roleMapper.selectByUserId(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail());
        result.put("phone", user.getPhone());
        result.put("userId", user.getId());
        result.put("roles", roles.stream().map(SysRole::getRoleKey).toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/me")
    @Operation(summary = "修改当前用户信息")
    public ResponseEntity<?> updateCurrentUser(@RequestHeader("Authorization") String authHeader,
                                               @RequestBody Map<String, String> updateRequest) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("message", "token无效"));
        }
        String username = jwtUtil.getUsernameFromToken(token);
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("message", "用户不存在"));
        }

        String nickname = updateRequest.get("nickname");
        String email = updateRequest.get("email");
        String phone = updateRequest.get("phone");

        if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
            SysUser byEmail = userMapper.selectByEmail(email);
            if (byEmail != null && !byEmail.getId().equals(user.getId())) {
                return ResponseEntity.status(409).body(Map.of("message", "邮箱已被其他用户使用"));
            }
        }
        if (phone != null && !phone.isEmpty() && !phone.equals(user.getPhone())) {
            SysUser byPhone = userMapper.selectByPhone(phone);
            if (byPhone != null && !byPhone.getId().equals(user.getId())) {
                return ResponseEntity.status(409).body(Map.of("message", "电话已被其他用户使用"));
            }
        }

        if (nickname != null) user.setNickname(nickname);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        user.setUpdatedAt(java.time.LocalDateTime.now());

        userMapper.update(user);

        return ResponseEntity.ok(Map.of("message", "更新成功"));
    }
}