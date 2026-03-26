package com.example.bms.controller;

import com.example.bms.domain.AlarmHistory;
import com.example.bms.service.AlarmHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@Tag(name = "Alarm")
public class AlarmController {
    private final AlarmHistoryService svc;
    public AlarmController(AlarmHistoryService svc) { this.svc = svc; }

    @GetMapping("/api/alarms")
    @Operation(summary = "查询所有历史报警信息")
    public List<AlarmHistory> list() { return svc.listAll(); }

    public static class ProcessReq { @Min(0) @Max(1) public int status; }

    @PostMapping("/api/alarms/{id}/process")
    @Operation(summary = "更新历史报警处理状态")
    public java.util.Map<String, Object> updateProcess(
            @Parameter(name = "id", in = ParameterIn.PATH, required = true)
            @PathVariable("id") long id,
            @RequestBody ProcessReq req) {
        int rows = svc.updateProcessStatus(id, req.status);
        return java.util.Map.of("ok", rows > 0, "id", id, "status", req.status);
    }
}