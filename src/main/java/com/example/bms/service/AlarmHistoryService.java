package com.example.bms.service;

import com.example.bms.domain.AlarmHistory;
import com.example.bms.domain.BatteryData;
import com.example.bms.domain.ThresholdSetting;
import com.example.bms.mapper.AlarmHistoryMapper;
import com.example.bms.mapper.ThresholdSettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlarmHistoryService {
    private final AlarmHistoryMapper mapper;
    private final ThresholdSettingMapper thresholdMapper;

    public AlarmHistoryService(AlarmHistoryMapper mapper, ThresholdSettingMapper thresholdMapper) {
        this.mapper = mapper;
        this.thresholdMapper = thresholdMapper;
    }

    public void recordIfAlarm(BatteryData data) {
        if (data == null) return;
        if (data.getAlarmState() == 0) return;
        ThresholdSetting t = thresholdMapper.selectLatest();
        AlarmHistory h = new AlarmHistory();
        h.setDeviceUuid(data.getDeviceUuid());
        h.setVoltage(data.getVoltage());
        h.setCurrent(data.getCurrent());
        h.setTemperature(data.getTemperature());
        h.setCharge(data.getCharge());
        h.setPower(data.getPower());
        h.setFanState(data.getFanState());
        h.setRelayState(data.getRelayState());
        h.setAlarmState(data.getAlarmState());
        h.setProcessStatus(0);
        if (t != null) {
            h.setVMax(t.getVMax());
            h.setVMin(t.getVMin());
            h.setIMax(t.getIMax());
            h.setTMax(t.getTMax());
        }
        h.setCreatedAt(LocalDateTime.now());
        mapper.insert(h);
    }

    public List<AlarmHistory> listAll() { return mapper.selectAll(); }

    public int updateProcessStatus(long id, int status) { return mapper.updateProcessStatus(id, status); }
}