package com.example.bms.controller;

import com.example.bms.service.BatteryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Tag(name = "Stream")
public class StreamController {
    private final BatteryService batteryService;
    public StreamController(BatteryService batteryService) { this.batteryService = batteryService; }

    @GetMapping(value = "/api/battery/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "实时SSE电池数据流")
    public SseEmitter stream() { return batteryService.createEmitter(); }
}