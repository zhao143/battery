package com.example.bms.service;

import com.example.bms.domain.Device;
import com.example.bms.domain.UserDevice;
import com.example.bms.mapper.DeviceMapper;
import com.example.bms.mapper.UserDeviceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceService {
    private final DeviceMapper deviceMapper;
    private final UserDeviceMapper userDeviceMapper;

    public DeviceService(DeviceMapper deviceMapper, UserDeviceMapper userDeviceMapper) {
        this.deviceMapper = deviceMapper;
        this.userDeviceMapper = userDeviceMapper;
    }

    public List<Device> getAllDevices() {
        List<Device> devices = deviceMapper.selectAll();
        for (Device device : devices) {
            List<UserDevice> userDevices = userDeviceMapper.selectByDeviceId(device.getId());
            List<Long> userIds = userDevices.stream()
                .map(UserDevice::getUserId)
                .collect(Collectors.toList());
            device.setBoundUsers(userIds);
        }
        return devices;
    }

    public List<Device> getDevicesByUserId(Long userId) {
        return deviceMapper.selectByUserId(userId);
    }

    public Device getDeviceByUuid(String uuid) {
        return deviceMapper.selectByUuid(uuid);
    }

    public Device getDeviceById(Long id) {
        return deviceMapper.selectById(id);
    }

    public Device addDevice(Device device) {
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        if (device.getStatus() == null) {
            device.setStatus(0);
        }
        deviceMapper.insert(device);
        return device;
    }

    public void updateDevice(Device device) {
        device.setUpdatedAt(LocalDateTime.now());
        deviceMapper.update(device);
    }

    public void deleteDevice(Long id) {
        deviceMapper.deleteById(id);
    }

    public void bindDeviceToUser(Long userId, Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device != null) {
            device.setUserId(userId);
            device.setUpdatedAt(LocalDateTime.now());
            deviceMapper.updateUserId(deviceId, userId);
        }
        UserDevice ud = new UserDevice();
        ud.setUserId(userId);
        ud.setDeviceId(deviceId);
        ud.setCreatedAt(LocalDateTime.now());
        userDeviceMapper.insert(ud);
    }

    public void unbindDeviceFromUser(Long userId, Long deviceId) {
        userDeviceMapper.delete(userId, deviceId);
    }
}
