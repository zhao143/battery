package com.example.bms.controller;

import com.example.bms.service.BatteryService;
import com.example.bms.tcp.TcpServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
@Tag(name = "Control")
public class ControlController {
    private final BatteryService batteryService;
    private final TcpServer tcpServer;

    public ControlController(BatteryService batteryService, TcpServer tcpServer) {
        this.batteryService = batteryService;
        this.tcpServer = tcpServer;
    }

    @GetMapping("/api/control")
    @Operation(summary = "获取期望的风扇与继电器状态")
    public Map<String, Integer> getControl() {
        var latest = batteryService.latest();
        int fan = latest != null ? latest.getFanState() : batteryService.getDesiredFanState();
        int relay = latest != null ? latest.getRelayState() : batteryService.getDesiredRelayState();
        return Map.of("fan", fan, "relay", relay);
    }

    public static class ControlReq {
        @Min(0) @Max(1) public Integer fan;
        @Min(0) @Max(1) public Integer relay;
    }

    @PostMapping("/api/control")
    @Operation(summary = "设置期望的风扇与继电器状态，并广播到TCP客户端")
    public Map<String, Integer> setControl(@RequestBody ControlReq req) {
        batteryService.setControlStates(req.fan, req.relay);
        int fan = batteryService.getDesiredFanState();
        int relay = batteryService.getDesiredRelayState();
        tcpServer.broadcastControlUpdate(fan, relay);
        batteryService.addControlSnapshot(req.fan, req.relay);
        return Map.of("fan", fan, "relay", relay);
    }

    public static class CmdReq { public String cmd; }

    @PostMapping("/api/tcp/send")
    @Operation(summary = "发送自定义调试命令到所有TCP客户端")
    public Map<String, String> sendCmd(@RequestBody CmdReq req) {
        String cmd = req != null && req.cmd != null ? req.cmd.trim() : "";
        if (!cmd.isEmpty()) tcpServer.broadcastRaw(cmd);
        return Map.of("sent", cmd);
    }
}