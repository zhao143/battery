package com.example.bms.domain;

import java.time.LocalDateTime;

public class BatteryDataPoint {
    private Long id;
    private String deviceUuid;
    private Long dataTypeId;
    private String dataValue;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceUuid() { return deviceUuid; }
    public void setDeviceUuid(String deviceUuid) { this.deviceUuid = deviceUuid; }
    public Long getDataTypeId() { return dataTypeId; }
    public void setDataTypeId(Long dataTypeId) { this.dataTypeId = dataTypeId; }
    public String getDataValue() { return dataValue; }
    public void setDataValue(String dataValue) { this.dataValue = dataValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}