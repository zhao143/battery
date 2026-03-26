package com.example.bms.service;

import com.example.bms.domain.BatteryData;
import com.example.bms.mapper.BatteryDataMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BatteryService {
    private final BatteryDataMapper mapper;
    private final AlarmHistoryService alarmHistoryService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicInteger desiredFanState = new AtomicInteger(0);
    private final AtomicInteger desiredRelayState = new AtomicInteger(0);

    public BatteryService(BatteryDataMapper mapper, AlarmHistoryService alarmHistoryService) {
        this.mapper = mapper;
        this.alarmHistoryService = alarmHistoryService;
    }

    public void saveData(BatteryData data) {
        if (data.getCreatedAt() == null) {
            data.setCreatedAt(LocalDateTime.now());
        }
        mapper.insert(data);
        alarmHistoryService.recordIfAlarm(data);
        desiredFanState.set(data.getFanState());
        desiredRelayState.set(data.getRelayState());
        pushToEmitters(data);
    }

    public BatteryData latest() { return mapper.selectLatest(); }
    public BatteryData latestByUuid(String deviceUuid) { return mapper.selectLatestByUuid(deviceUuid); }
    public List<BatteryData> recent(int limit) { return mapper.selectRecent(limit); }
    public List<BatteryData> recentByUuid(String deviceUuid, int limit) { return mapper.selectRecentByUuid(deviceUuid, limit); }

    public int getDesiredFanState() { return desiredFanState.get(); }
    public int getDesiredFanState(String deviceUuid) { return desiredFanState.get(); }
    public int getDesiredRelayState() { return desiredRelayState.get(); }
    public int getDesiredRelayState(String deviceUuid) { return desiredRelayState.get(); }
    public void setControlStates(Integer fan, Integer relay) {
        if (fan != null) desiredFanState.set(fan);
        if (relay != null) desiredRelayState.set(relay);
    }

    public void addControlSnapshot(Integer fan, Integer relay) {
        BatteryData latest = latest();
        BatteryData snapshot = new BatteryData();
        if (latest != null) {
            snapshot.setDeviceUuid(latest.getDeviceUuid());
            snapshot.setVoltage(latest.getVoltage());
            snapshot.setCurrent(latest.getCurrent());
            snapshot.setTemperature(latest.getTemperature());
            snapshot.setCharge(latest.getCharge());
            snapshot.setPower(latest.getPower());
            snapshot.setFanState(latest.getFanState());
            snapshot.setRelayState(latest.getRelayState());
            snapshot.setAlarmState(latest.getAlarmState());
        } else {
            snapshot.setVoltage(0);
            snapshot.setCurrent(0);
            snapshot.setTemperature(0);
            snapshot.setCharge(0);
            snapshot.setPower(0);
            snapshot.setFanState(0);
            snapshot.setRelayState(0);
            snapshot.setAlarmState(0);
        }
        if (fan != null) snapshot.setFanState(fan);
        if (relay != null) snapshot.setRelayState(relay);
        saveData(snapshot);
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        BatteryData latest = latest();
        if (latest != null) {
            try { emitter.send(latest); } catch (IOException ignored) {}
        }
        return emitter;
    }

    public SseEmitter createEmitterByUuid(String deviceUuid) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        BatteryData latest = latestByUuid(deviceUuid);
        if (latest != null) {
            try { emitter.send(latest); } catch (IOException ignored) {}
        }
        return emitter;
    }

    private void pushToEmitters(BatteryData data) {
        for (SseEmitter emitter : emitters) {
            try { emitter.send(data); } catch (Exception e) { try { emitter.completeWithError(e); } catch (Exception ignored) {} emitters.remove(emitter); }
        }
    }
}
