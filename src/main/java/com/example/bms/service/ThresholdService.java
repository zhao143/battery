package com.example.bms.service;

import com.example.bms.domain.ThresholdSetting;
import com.example.bms.mapper.ThresholdSettingMapper;
import com.example.bms.tcp.TcpBroadcastService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ThresholdService {
    private final ThresholdSettingMapper mapper;
    private final TcpBroadcastService broadcaster;

    public ThresholdService(ThresholdSettingMapper mapper, TcpBroadcastService broadcaster) {
        this.mapper = mapper;
        this.broadcaster = broadcaster;
    }

    public ThresholdSetting latest() { return mapper.selectLatest(); }

    public ThresholdSetting getByDeviceUuid(String deviceUuid) {
        return mapper.selectByDeviceUuid(deviceUuid);
    }

    @Transactional
    public ThresholdSetting setVMax(String deviceUuid, double v) {
        ThresholdSetting existing = mapper.selectByDeviceUuid(deviceUuid);
        if (existing != null && existing.getId() != null) {
            mapper.updateVMax(existing.getId(), v);
        } else {
            ThresholdSetting t = new ThresholdSetting();
            t.setDeviceUuid(deviceUuid);
            t.setVMax(v);
            t.setUpdatedAt(LocalDateTime.now());
            mapper.insertWithVMax(t);
        }
        broadcaster.broadcastRaw("SET_Vol_h" + fmt(v) + "#");
        return mapper.selectByDeviceUuid(deviceUuid);
    }

    @Transactional
    public ThresholdSetting setVMin(String deviceUuid, double v) {
        ThresholdSetting existing = mapper.selectByDeviceUuid(deviceUuid);
        if (existing != null && existing.getId() != null) {
            mapper.updateVMin(existing.getId(), v);
        } else {
            ThresholdSetting t = new ThresholdSetting();
            t.setDeviceUuid(deviceUuid);
            t.setVMin(v);
            t.setUpdatedAt(LocalDateTime.now());
            mapper.insertWithVMin(t);
        }
        broadcaster.broadcastRaw("SET_Vol_l" + fmt(v) + "#");
        return mapper.selectByDeviceUuid(deviceUuid);
    }

    @Transactional
    public ThresholdSetting setIMax(String deviceUuid, double v) {
        ThresholdSetting existing = mapper.selectByDeviceUuid(deviceUuid);
        if (existing != null && existing.getId() != null) {
            mapper.updateIMax(existing.getId(), v);
        } else {
            ThresholdSetting t = new ThresholdSetting();
            t.setDeviceUuid(deviceUuid);
            t.setIMax(v);
            t.setUpdatedAt(LocalDateTime.now());
            mapper.insertWithIMax(t);
        }
        broadcaster.broadcastRaw("SET_Cur_h" + fmt(v) + "#");
        return mapper.selectByDeviceUuid(deviceUuid);
    }

    @Transactional
    public ThresholdSetting setTMax(String deviceUuid, int v) {
        ThresholdSetting existing = mapper.selectByDeviceUuid(deviceUuid);
        if (existing != null && existing.getId() != null) {
            mapper.updateTMax(existing.getId(), v);
        } else {
            ThresholdSetting t = new ThresholdSetting();
            t.setDeviceUuid(deviceUuid);
            t.setTMax(v);
            t.setUpdatedAt(LocalDateTime.now());
            mapper.insertWithTMax(t);
        }
        broadcaster.broadcastRaw("SET_temp_h" + v + "#");
        return mapper.selectByDeviceUuid(deviceUuid);
    }

    @Transactional
    public ThresholdSetting saveAll(String deviceUuid, Double vMax, Double vMin, Double iMax, Integer tMax) {
        ThresholdSetting existing = mapper.selectByDeviceUuid(deviceUuid);
        if (existing != null && existing.getId() != null) {
            mapper.updateAll(existing.getId(), vMax, vMin, iMax, tMax);
        } else {
            ThresholdSetting t = new ThresholdSetting();
            t.setDeviceUuid(deviceUuid);
            t.setVMax(vMax);
            t.setVMin(vMin);
            t.setIMax(iMax);
            t.setTMax(tMax);
            t.setUpdatedAt(LocalDateTime.now());
            mapper.insertAll(t);
        }
        if (vMax != null) broadcaster.broadcastRaw("SET_Vol_h" + fmt(vMax) + "#");
        if (vMin != null) broadcaster.broadcastRaw("SET_Vol_l" + fmt(vMin) + "#");
        if (iMax != null) broadcaster.broadcastRaw("SET_Cur_h" + fmt(iMax) + "#");
        if (tMax != null) broadcaster.broadcastRaw("SET_temp_h" + tMax + "#");
        return mapper.selectByDeviceUuid(deviceUuid);
    }

    private String fmt(double v) {
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }
}