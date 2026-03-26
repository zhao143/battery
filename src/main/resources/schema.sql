-- ============================================================
-- 电池管理系统 (BMS) 数据库架构
-- 数据库名称: bms_uuid
-- 描述: 用于管理电池设备、用户、阈值设置和报警历史的数据库
-- ============================================================

CREATE DATABASE IF NOT EXISTS bms_uuid;
USE bms_uuid;

-- ============================================================
-- 用户表 (sys_user)
-- 存储系统用户信息，包括管理员和普通用户
-- 关系: 一个用户可以有多个角色 (通过sys_user_role表关联)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名，唯一且不能为空',
    password VARCHAR(255) NOT NULL COMMENT '密码，经过BCrypt加密存储',
    nickname VARCHAR(100) COMMENT '用户昵称，用于显示',
    email VARCHAR(100) COMMENT '用户邮箱，可用于找回密码等功能',
    phone VARCHAR(20) COMMENT '用户电话',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态: 1=启用, 0=禁用',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ============================================================
-- 角色表 (sys_role)
-- 定义系统中的角色类型
-- 关系: 一个角色可以分配给多个用户 (通过sys_user_role表关联)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID，主键自增',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称，唯一',
    role_key VARCHAR(50) NOT NULL COMMENT '角色标识，如: admin, user',
    description VARCHAR(255) COMMENT '角色描述',
    created_at DATETIME NOT NULL COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ============================================================
-- 权限表 (sys_permission)
-- 定义系统中的细粒度权限
-- 关系: 一个权限可以分配给多个角色 (通过sys_role_permission表关联)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID，主键自增',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称，如: 查看所有用户',
    permission_key VARCHAR(100) NOT NULL COMMENT '权限标识，如: user:list',
    description VARCHAR(255) COMMENT '权限描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- ============================================================
-- 用户角色关联表 (sys_user_role)
-- 用户和角色的多对多关联表
-- 关系: 一个用户可以拥有多个角色，一个角色可以分配给多个用户
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID，外键关联sys_user表',
    role_id BIGINT NOT NULL COMMENT '角色ID，外键关联sys_role表',
    UNIQUE KEY uk_user_role (user_id, role_id) COMMENT '确保用户-角色组合唯一',
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ============================================================
-- 角色权限关联表 (sys_role_permission)
-- 角色和权限的多对多关联表
-- 关系: 一个角色可以拥有多个权限，一个权限可以分配给多个角色
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID，主键自增',
    role_id BIGINT NOT NULL COMMENT '角色ID，外键关联sys_role表',
    permission_id BIGINT NOT NULL COMMENT '权限ID，外键关联sys_permission表',
    UNIQUE KEY uk_role_permission (role_id, permission_id) COMMENT '确保角色-权限组合唯一',
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================================
-- 设备表 (device)
-- 存储电池管理设备信息
-- 关系: 一个用户可以拥有多个设备，通过device.user_id直接关联
--       同时也可以通过user_device表进行多对多关联（预留）
-- ============================================================
CREATE TABLE IF NOT EXISTS device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID，主键自增',
    device_uuid VARCHAR(36) NOT NULL UNIQUE COMMENT '设备UUID，设备的唯一标识符',
    device_name VARCHAR(100) COMMENT '设备名称，如: 1号电池柜',
    device_type VARCHAR(50) COMMENT '设备类型，如: BMS, 锂电池, 铅酸电池',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '设备状态: 1=在线, 0=离线',
    user_id BIGINT COMMENT '所属用户ID，外键关联sys_user表，NULL表示未分配',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE SET NULL,
    INDEX idx_device_user (user_id) COMMENT '用户ID索引，加速按用户查询设备'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- ============================================================
-- 用户设备关联表 (user_device)
-- 用户和设备的多对多关联表（备用方案）
-- 当需要支持一个用户管理多个设备，且一个设备可被多个用户管理时使用
-- 目前主要通过device.user_id直接关联
-- ============================================================
CREATE TABLE IF NOT EXISTS user_device (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID，主键自增',
    user_id BIGINT NOT NULL COMMENT '用户ID，外键关联sys_user表',
    device_id BIGINT NOT NULL COMMENT '设备ID，外键关联device表',
    created_at DATETIME NOT NULL COMMENT '绑定时间',
    UNIQUE KEY uk_user_device (user_id, device_id) COMMENT '确保用户-设备组合唯一',
    CONSTRAINT fk_ud_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_ud_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设备关联表';

-- ============================================================
-- 电池数据表 (battery_data)
-- 存储电池的实时监测数据
-- 关系: 通过device_uuid关联到具体的设备
-- 说明: 数据由TCP端点采集并实时插入
-- ============================================================
CREATE TABLE IF NOT EXISTS battery_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '数据记录ID，主键自增',
    device_uuid VARCHAR(36) NOT NULL COMMENT '设备UUID，关联到device表的device_uuid',
    voltage DOUBLE NOT NULL COMMENT '电压，单位: 伏特(V)',
    current DOUBLE NOT NULL COMMENT '电流，单位: 安培(A)',
    temperature INT NOT NULL COMMENT '温度，单位: 摄氏度(°C)',
    charge INT NOT NULL COMMENT '电量百分比，0-100',
    power DOUBLE NOT NULL COMMENT '功率，单位: 瓦特(W)',
    fan_state INT NOT NULL COMMENT '风扇状态: 1=开启, 0=关闭',
    relay_state INT NOT NULL COMMENT '继电器状态: 1=开启, 0=关闭',
    alarm_state INT NOT NULL COMMENT '报警状态: 1=报警, 0=正常',
    created_at DATETIME NOT NULL COMMENT '数据采集时间',
    INDEX idx_device_uuid (device_uuid) COMMENT '设备UUID索引',
    INDEX idx_created_at (created_at) COMMENT '时间索引，用于查询最近数据',
    INDEX idx_device_time (device_uuid, created_at) COMMENT '复合索引，加速设备+时间查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电池实时数据表';

-- ============================================================
-- 阈值设置表 (threshold_setting)
-- 存储各设备的报警阈值设置
-- 关系: 通过device_uuid关联到具体的设备
-- 说明: 每个设备只有一条阈值记录，使用UPSERT模式更新
-- ============================================================
CREATE TABLE IF NOT EXISTS threshold_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设置记录ID，主键自增',
    device_uuid VARCHAR(36) COMMENT '设备UUID，关联到device表的device_uuid',
    v_max DOUBLE COMMENT '最高电压阈值，单位: 伏特(V)',
    v_min DOUBLE COMMENT '最低电压阈值，单位: 伏特(V)',
    i_max DOUBLE COMMENT '最大电流阈值，单位: 安培(A)',
    t_max INT COMMENT '最高温度阈值，单位: 摄氏度(°C)',
    updated_at DATETIME NOT NULL COMMENT '最后更新时间',
    INDEX idx_device_uuid (device_uuid) COMMENT '设备UUID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阈值设置表';

-- ============================================================
-- 报警历史表 (alarm_history)
-- 存储历史报警记录
-- 关系: 通过device_uuid关联到具体的设备
-- 说明: 当电池数据触发报警条件时，记录报警详情
-- ============================================================
CREATE TABLE IF NOT EXISTS alarm_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报警记录ID，主键自增',
    device_uuid VARCHAR(36) NOT NULL COMMENT '设备UUID，关联到device表的device_uuid',
    voltage DOUBLE NOT NULL COMMENT '报警时电压',
    current DOUBLE NOT NULL COMMENT '报警时电流',
    temperature INT NOT NULL COMMENT '报警时温度',
    charge INT NOT NULL COMMENT '报警时电量',
    power DOUBLE NOT NULL COMMENT '报警时功率',
    fan_state INT NOT NULL COMMENT '报警时风扇状态',
    relay_state INT NOT NULL COMMENT '报警时继电器状态',
    alarm_state INT NOT NULL COMMENT '报警状态: 1=报警, 0=正常',
    process_status INT NOT NULL DEFAULT 0 COMMENT '处理状态: 0=未处理, 1=已处理',
    v_max DOUBLE COMMENT '当时设置的最高电压阈值',
    v_min DOUBLE COMMENT '当时设置的最低电压阈值',
    i_max DOUBLE COMMENT '当时设置的最大电流阈值',
    t_max INT COMMENT '当时设置的最高温度阈值',
    created_at DATETIME NOT NULL COMMENT '报警发生时间',
    INDEX idx_device_uuid (device_uuid) COMMENT '设备UUID索引',
    INDEX idx_created_at (created_at) COMMENT '时间索引',
    INDEX idx_process_status (process_status) COMMENT '处理状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警历史表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 初始化用户 (密码都是 password123)
INSERT IGNORE INTO sys_user (username, password, nickname, email, status, created_at, updated_at) VALUES
('admin', '$2a$10$gplpauQ1FPllJxdhqrykfOtmi0Y6mmu/9JWmqudgGRRVK5/oMoiri', '管理员', 'admin@bms.com', 1, NOW(), NOW()),
('user1', '$2a$10$gplpauQ1FPllJxdhqrykfOtmi0Y6mmu/9JWmqudgGRRVK5/oMoiri', '用户1', 'user1@bms.com', 1, NOW(), NOW()),
('user2', '$2a$10$gplpauQ1FPllJxdhqrykfOtmi0Y6mmu/9JWmqudgGRRVK5/oMoiri', '用户2', 'user2@bms.com', 1, NOW(), NOW());

-- 初始化角色
INSERT IGNORE INTO sys_role (role_name, role_key, description, created_at) VALUES
('管理员', 'admin', '系统管理员，拥有所有权限', NOW()),
('普通用户', 'user', '普通用户，只能操作自己的设备', NOW());

-- 初始化权限
INSERT IGNORE INTO sys_permission (permission_name, permission_key, description) VALUES
('查看所有用户', 'user:list', '查看系统中所有用户信息'),
('管理用户', 'user:manage', '创建、编辑、删除用户'),
('查看所有设备', 'device:list', '查看系统中所有设备'),
('查看自己设备', 'device:self', '查看自己绑定的设备'),
('查看数据', 'data:view', '查看电池监测数据'),
('管理设备', 'device:manage', '管理设备信息，包括添加、编辑、删除设备');

-- 分配用户角色
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin用户拥有管理员角色
(2, 2),  -- user1用户拥有普通用户角色
(3, 2);  -- user2用户拥有普通用户角色

-- 分配角色权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
-- 管理员拥有所有权限
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6),
-- 普通用户只能查看自己设备和数据
(2, 4), (2, 5);
