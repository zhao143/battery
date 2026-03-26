package com.example.bms.domain;

import java.time.LocalDateTime;

public class ThresholdSetting {
    private Long id;
    private String deviceUuid;
    private Double vMax;
    private Double vMin;
    private Double iMax;
    private Integer tMax;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeviceUuid() { return deviceUuid; }
    public void setDeviceUuid(String deviceUuid) { this.deviceUuid = deviceUuid; }
    public Double getVMax() { return vMax; }
    public void setVMax(Double vMax) { this.vMax = vMax; }
    public Double getVMin() { return vMin; }
    public void setVMin(Double vMin) { this.vMin = vMin; }
    public Double getIMax() { return iMax; }
    public void setIMax(Double iMax) { this.iMax = iMax; }
    public Integer getTMax() { return tMax; }
    public void setTMax(Integer tMax) { this.tMax = tMax; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}