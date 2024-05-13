package com.nf.neoflow.controller;

import com.nf.neoflow.dto.process.*;
import com.nf.neoflow.dto.response.Result;
import com.nf.neoflow.models.Process;
import com.nf.neoflow.service.ProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Process 流程")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/process")
public class ProcessController {

    private final ProcessService processService;

    @Operation(description = "新建流程")
    @PostMapping("/create")
    public ResponseEntity<Result<Process>> create(@RequestBody ProcessCreateForm form) {
        return ResponseEntity.ok(Result.success(processService.create(form)));
    }

    @Operation(description = "查询流程列表")
    @GetMapping("/list")
    public ResponseEntity<Result<Page<Process>>> processList(@ModelAttribute ProcessQueryForm form) {
        return ResponseEntity.ok(Result.success(processService.processList(form)));
    }

    @Operation(description = "变更流程启用状态")
    @PostMapping("/changeActive")
    public ResponseEntity<Result<ProcessActiveStatusDto>> changeActive(@Valid @RequestBody ProcessChangeActiveForm form) {
        return ResponseEntity.ok(Result.success(processService.changeActive(form)));
    }

    @Operation(description = "变更流程启用版本")
    @PostMapping("/changeVersion")
    public ResponseEntity<Result<?>> changeActiveVersion(@Valid @RequestBody ProcessChangeVersionForm form) {
        processService.changeActiveVersion(form);
        return ResponseEntity.ok(Result.success());
    }

    @Operation(description = "变更流程启用版本")
    @GetMapping("/activeHistory")
    public ResponseEntity<Result<List<ActiveVersionHistoryDto>>> activeVersionHistory(@RequestParam String name) {
        return ResponseEntity.ok(Result.success(processService.activeVersionHistory(name)));
    }

    @Operation(description = "流程统计查询")
    @PostMapping("/statistics")
    public ResponseEntity<Result<List<ProcessQueryStatisticsDto>>> QueryProcessStatistics(@Valid @RequestBody ProcessQueryStatisticsForm form) {
        return ResponseEntity.ok(Result.success(processService.queryProcessForStatistics(form)));
    }

    @Operation(description = "查询所有流程名称")
    @GetMapping("/name")
    public ResponseEntity<Result<List<String>>> queryAllProcessName() {
        return ResponseEntity.ok(Result.success(processService.queryAllProcessName()));
    }

}
