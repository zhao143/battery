package com.example.bms;

import com.example.bms.domain.Device;
import com.example.bms.domain.SysRole;
import com.example.bms.domain.SysUser;
import com.example.bms.domain.SysUserRole;
import com.example.bms.domain.UserDevice;
import com.example.bms.mapper.DeviceMapper;
import com.example.bms.mapper.SysRoleMapper;
import com.example.bms.mapper.SysUserMapper;
import com.example.bms.mapper.SysUserRoleMapper;
import com.example.bms.mapper.UserDeviceMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@MapperScan("com.example.bms.mapper")
public class BmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BmsApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(SysUserMapper userMapper, SysRoleMapper roleMapper, 
                                      DeviceMapper deviceMapper, UserDeviceMapper userDeviceMapper,
                                      SysUserRoleMapper userRoleMapper,
                                      PasswordEncoder encoder) {
        return args -> {
            // 创建角色
            List<SysRole> roles = roleMapper.selectAll();
            if (roles.isEmpty()) {
                SysRole adminRole = new SysRole();
                adminRole.setRoleName("管理员");
                adminRole.setRoleKey("admin");
                adminRole.setDescription("系统管理员");
                adminRole.setCreatedAt(LocalDateTime.now());
                roleMapper.insert(adminRole);

                SysRole userRole = new SysRole();
                userRole.setRoleName("普通用户");
                userRole.setRoleKey("user");
                userRole.setDescription("普通用户");
                userRole.setCreatedAt(LocalDateTime.now());
                roleMapper.insert(userRole);
                System.out.println("初始化角色数据完成!");
            }

            // 重置密码
            List<SysUser> users = userMapper.selectAll();
            for (SysUser user : users) {
                user.setPassword(encoder.encode("password123"));
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.update(user);
            }
            System.out.println("所有用户密码已重置为: password123");

            // 分配用户角色
            List<SysUserRole> userRoles = userRoleMapper.selectAll();
            if (userRoles.isEmpty()) {
                // admin -> admin角色 (role_id=1)
                SysUserRole adminUserRole = new SysUserRole();
                adminUserRole.setUserId(1L);
                adminUserRole.setRoleId(1L);
                userRoleMapper.insert(adminUserRole);
                
                // user1 -> user角色 (role_id=2)
                SysUserRole user1UserRole = new SysUserRole();
                user1UserRole.setUserId(2L);
                user1UserRole.setRoleId(2L);
                userRoleMapper.insert(user1UserRole);
                
                // user2 -> user角色 (role_id=2)
                SysUserRole user2UserRole = new SysUserRole();
                user2UserRole.setUserId(3L);
                user2UserRole.setRoleId(2L);
                userRoleMapper.insert(user2UserRole);
                
                System.out.println("初始化用户角色关联完成!");
            }

            // 创建设备
            if (deviceMapper.selectAll().isEmpty()) {
                Device device1 = new Device();
                device1.setDeviceUuid("550e8400-e29b-41d4-a716-446655440001");
                device1.setDeviceName("用户1的设备");
                device1.setDeviceType("BMS");
                device1.setStatus(0);
                device1.setCreatedAt(LocalDateTime.now());
                device1.setUpdatedAt(LocalDateTime.now());
                deviceMapper.insert(device1);

                Device device2 = new Device();
                device2.setDeviceUuid("550e8400-e29b-41d4-a716-446655440002");
                device2.setDeviceName("用户2的设备");
                device2.setDeviceType("BMS");
                device2.setStatus(0);
                device2.setCreatedAt(LocalDateTime.now());
                device2.setUpdatedAt(LocalDateTime.now());
                deviceMapper.insert(device2);

                UserDevice ud1 = new UserDevice();
                ud1.setUserId(2L);
                ud1.setDeviceId(1L);
                ud1.setCreatedAt(LocalDateTime.now());
                userDeviceMapper.insert(ud1);

                UserDevice ud2 = new UserDevice();
                ud2.setUserId(3L);
                ud2.setDeviceId(2L);
                ud2.setCreatedAt(LocalDateTime.now());
                userDeviceMapper.insert(ud2);

                System.out.println("初始化设备数据完成!");
            }
        };
    }
}
