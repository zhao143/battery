package com.example.bms.controller;

import com.example.bms.domain.ThresholdSetting;
import com.example.bms.security.JwtUserDetails;
import com.example.bms.service.ThresholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@Tag(name = "Threshold")
public class ThresholdController {
    private final ThresholdService svc;
    public ThresholdController(ThresholdService svc) { this.svc = svc; }

    @GetMapping("/api/threshold")
    @Operation(summary = "查询最新阈值设置")
    public ThresholdSetting latest(@RequestParam(required = false) String deviceUuid) {
        if (deviceUuid != null && !deviceUuid.isEmpty()) {
            return svc.getByDeviceUuid(deviceUuid);
        }
        return svc.latest();
    }

    public static class VReq {
        public double value;
        public String deviceUuid;
    }
    public static class TReq {
        public int value;
        public String deviceUuid;
    }

    @PostMapping("/api/threshold/vmax")
    @Operation(summary = "设置最高电压")
    public Map<String, Object> vmax(@RequestBody VReq req) {
        ThresholdSetting t = svc.setVMax(req.deviceUuid, req.value);
        return Map.of("ok", true, "threshold", t != null ? t : "null");
    }

    @PostMapping("/api/threshold/vmin")
    @Operation(summary = "设置最低电压")
    public Map<String, Object> vmin(@RequestBody VReq req) {
        ThresholdSetting t = svc.setVMin(req.deviceUuid, req.value);
        return Map.of("ok", true, "threshold", t != null ? t : "null");
    }

    @PostMapping("/api/threshold/imax")
    @Operation(summary = "设置最大电流")
    public Map<String, Object> imax(@RequestBody VReq req) {
        ThresholdSetting t = svc.setIMax(req.deviceUuid, req.value);
        return Map.of("ok", true, "threshold", t != null ? t : "null");
    }

    @PostMapping("/api/threshold/tmax")
    @Operation(summary = "设置温度阈值")
    public Map<String, Object> tmax(@RequestBody TReq req) {
        ThresholdSetting t = svc.setTMax(req.deviceUuid, req.value);
        return Map.of("ok", true, "threshold", t != null ? t : "null");
    }

    public static class AllReq {
        public String deviceUuid;
        public Double vMax;
        public Double vMin;
        public Double iMax;
        public Integer tMax;
    }

    @PostMapping("/api/threshold/saveAll")
    @Operation(summary = "一键保存所有阈值")
    public Map<String, Object> saveAll(@RequestBody AllReq req) {
        ThresholdSetting t = svc.saveAll(req.deviceUuid, req.vMax, req.vMin, req.iMax, req.tMax);
        return Map.of("ok", true, "threshold", t != null ? t : "null");
    }
}