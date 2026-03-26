package com.example.bms.domain;

import java.time.LocalDateTime;

public class AlarmHistory {
    private Long id;
    private String deviceUuid;
    private double voltage;
    private double current;
    private int temperature;
    private int charge;
    private double power;
    private int fanState;
    private int relayState;
    private int alarmState;
    private int processStatus;
    private Double vMax;
    private Double vMin;
    private Double iMax;
    private Integer tMax;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceUuid() { return deviceUuid; }
    public void setDeviceUuid(String deviceUuid) { this.deviceUuid = deviceUuid; }
    public double getVoltage() { return voltage; }
    public void setVoltage(double voltage) { this.voltage = voltage; }
    public double getCurrent() { return current; }
    public void setCurrent(double current) { this.current = current; }
    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }
    public int getCharge() { return charge; }
    public void setCharge(int charge) { this.charge = charge; }
    public double getPower() { return power; }
    public void setPower(double power) { this.power = power; }
    public int getFanState() { return fanState; }
    public void setFanState(int fanState) { this.fanState = fanState; }
    public int getRelayState() { return relayState; }
    public void setRelayState(int relayState) { this.relayState = relayState; }
    public int getAlarmState() { return alarmState; }
    public void setAlarmState(int alarmState) { this.alarmState = alarmState; }
    public int getProcessStatus() { return processStatus; }
    public void setProcessStatus(int processStatus) { this.processStatus = processStatus; }
    public Double getVMax() { return vMax; }
    public void setVMax(Double vMax) { this.vMax = vMax; }
    public Double getVMin() { return vMin; }
    public void setVMin(Double vMin) { this.vMin = vMin; }
    public Double getIMax() { return iMax; }
    public void setIMax(Double iMax) { this.iMax = iMax; }
    public Integer getTMax() { return tMax; }
    public void setTMax(Integer tMax) { this.tMax = tMax; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}