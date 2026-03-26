package com.example.bms.service;

import com.example.bms.domain.BatteryData;
import com.example.bms.domain.BatteryDataPoint;
import com.example.bms.mapper.BatteryDataMapper;
import com.example.bms.mapper.DataTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BatteryService {
    private final BatteryDataMapper batteryDataMapper;
    private final DataTypeMapper dataTypeMapper;
    private final AlarmHistoryService alarmHistoryService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicInteger desiredFanState = new AtomicInteger(0);
    private final AtomicInteger desiredRelayState = new AtomicInteger(0);

    private static final Map<String, Long> TYPE_CODE_TO_ID = new ConcurrentHashMap<>();
    private static final Map<Long, String> TYPE_ID_TO_CODE = new ConcurrentHashMap<>();

    private static final String[] DATA_TYPES = {"voltage", "current", "temperature", "charge", "power", "fan_state", "relay_state", "alarm_state"};

    public BatteryService(BatteryDataMapper batteryDataMapper, DataTypeMapper dataTypeMapper, AlarmHistoryService alarmHistoryService) {
        this.batteryDataMapper = batteryDataMapper;
        this.dataTypeMapper = dataTypeMapper;
        this.alarmHistoryService = alarmHistoryService;
        initTypeMappings();
    }

    private void initTypeMappings() {
        for (String typeCode : DATA_TYPES) {
            Long id = dataTypeMapper.findIdByCode(typeCode);
            if (id != null) {
                TYPE_CODE_TO_ID.put(typeCode, id);
                TYPE_ID_TO_CODE.put(id, typeCode);
            }
        }
    }

    public Long getDataTypeId(String typeCode) {
        Long id = TYPE_CODE_TO_ID.get(typeCode);
        if (id == null) {
            id = dataTypeMapper.findIdByCode(typeCode);
            if (id != null) {
                TYPE_CODE_TO_ID.put(typeCode, id);
                TYPE_ID_TO_CODE.put(id, typeCode);
            }
        }
        return id;
    }

    public String getDataTypeCode(Long id) {
        String code = TYPE_ID_TO_CODE.get(id);
        if (code == null) {
            code = dataTypeMapper.findCodeById(id);
            if (code != null) {
                TYPE_ID_TO_CODE.put(id, code);
                TYPE_CODE_TO_ID.put(code, id);
            }
        }
        return code;
    }

    public void saveData(BatteryData data) {
        LocalDateTime now = data.getCreatedAt() != null ? data.getCreatedAt() : LocalDateTime.now();

        saveDataPoint(data.getDeviceUuid(), "voltage", String.valueOf(data.getVoltage()), now);
        saveDataPoint(data.getDeviceUuid(), "current", String.valueOf(data.getCurrent()), now);
        saveDataPoint(data.getDeviceUuid(), "temperature", String.valueOf(data.getTemperature()), now);
        saveDataPoint(data.getDeviceUuid(), "charge", String.valueOf(data.getCharge()), now);
        saveDataPoint(data.getDeviceUuid(), "power", String.valueOf(data.getPower()), now);
        saveDataPoint(data.getDeviceUuid(), "fan_state", String.valueOf(data.getFanState()), now);
        saveDataPoint(data.getDeviceUuid(), "relay_state", String.valueOf(data.getRelayState()), now);
        saveDataPoint(data.getDeviceUuid(), "alarm_state", String.valueOf(data.getAlarmState()), now);

        alarmHistoryService.recordIfAlarm(data);
        desiredFanState.set(data.getFanState());
        desiredRelayState.set(data.getRelayState());
        pushToEmitters(data);
    }

    private void saveDataPoint(String deviceUuid, String typeCode, String value, LocalDateTime createdAt) {
        Long typeId = getDataTypeId(typeCode);
        if (typeId != null) {
            BatteryDataPoint point = new BatteryDataPoint();
            point.setDeviceUuid(deviceUuid);
            point.setDataTypeId(typeId);
            point.setDataValue(value);
            point.setCreatedAt(createdAt);
            batteryDataMapper.insert(point);
        }
    }

    public BatteryData latestByUuid(String deviceUuid) {
        BatteryData data = new BatteryData();
        data.setDeviceUuid(deviceUuid);

        Map<String, String> values = getLatestValues(deviceUuid);
        if (values == null || values.isEmpty()) {
            return null;
        }

        data.setVoltage(parseDouble(values.get("voltage")));
        data.setCurrent(parseDouble(values.get("current")));
        data.setTemperature(parseInt(values.get("temperature")));
        data.setCharge(parseInt(values.get("charge")));
        data.setPower(parseDouble(values.get("power")));
        data.setFanState(parseInt(values.get("fan_state")));
        data.setRelayState(parseInt(values.get("relay_state")));
        data.setAlarmState(parseInt(values.get("alarm_state")));

        return data;
    }

    private Map<String, String> getLatestValues(String deviceUuid) {
        Map<String, String> result = new HashMap<>();
        for (String typeCode : DATA_TYPES) {
            Long typeId = getDataTypeId(typeCode);
            if (typeId != null) {
                BatteryDataPoint point = batteryDataMapper.selectLatestByUuidAndType(deviceUuid, typeId);
                if (point != null) {
                    result.put(typeCode, point.getDataValue());
                }
            }
        }
        return result;
    }

    public List<BatteryData> recentByUuid(String deviceUuid, int limit) {
        List<BatteryData> result = new ArrayList<>();

        Map<String, List<BatteryDataPoint>> allPoints = new HashMap<>();
        for (String typeCode : DATA_TYPES) {
            Long typeId = getDataTypeId(typeCode);
            if (typeId != null) {
                List<BatteryDataPoint> points = batteryDataMapper.selectRecentByUuidAndType(deviceUuid, typeId, limit);
                allPoints.put(typeCode, points);
            }
        }

        if (allPoints.values().stream().allMatch(List::isEmpty)) {
            return result;
        }

        int maxSize = allPoints.values().stream().mapToInt(List::size).max().orElse(0);
        for (int i = 0; i < maxSize; i++) {
            BatteryData data = new BatteryData();
            data.setDeviceUuid(deviceUuid);
            result.add(data);
        }

        for (Map.Entry<String, List<BatteryDataPoint>> entry : allPoints.entrySet()) {
            String typeCode = entry.getKey();
            List<BatteryDataPoint> points = entry.getValue();
            for (int i = 0; i < points.size(); i++) {
                BatteryData data = result.get(i);
                String value = points.get(i).getDataValue();
                switch (typeCode) {
                    case "voltage" -> data.setVoltage(parseDouble(value));
                    case "current" -> data.setCurrent(parseDouble(value));
                    case "temperature" -> data.setTemperature(parseInt(value));
                    case "charge" -> data.setCharge(parseInt(value));
                    case "power" -> data.setPower(parseDouble(value));
                    case "fan_state" -> data.setFanState(parseInt(value));
                    case "relay_state" -> data.setRelayState(parseInt(value));
                    case "alarm_state" -> data.setAlarmState(parseInt(value));
                }
                if (data.getCreatedAt() == null) {
                    data.setCreatedAt(points.get(i).getCreatedAt());
                }
            }
        }

        return result;
    }

    public BatteryData latest() {
        return null;
    }

    public List<BatteryData> recent(int limit) {
        return new ArrayList<>();
    }

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
        return emitter;
    }

    public SseEmitter createEmitterByUuid(String deviceUuid) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        BatteryData data = latestByUuid(deviceUuid);
        if (data != null) {
            try { emitter.send(data); } catch (IOException ignored) {}
        }
        return emitter;
    }

    private void pushToEmitters(BatteryData data) {
        for (SseEmitter emitter : emitters) {
            try { emitter.send(data); } catch (Exception e) { try { emitter.completeWithError(e); } catch (Exception ignored) {} emitters.remove(emitter); }
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.isEmpty()) return 0;
        try { return Double.parseDouble(value); } catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String value) {
        if (value == null || value.isEmpty()) return 0;
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 0; }
    }
}