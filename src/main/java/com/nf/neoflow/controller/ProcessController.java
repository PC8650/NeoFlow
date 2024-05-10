package com.nf.neoflow.controller;

import com.nf.neoflow.dto.process.*;
import com.nf.neoflow.dto.response.Result;
import com.nf.neoflow.models.Process;
import com.nf.neoflow.service.ProcessService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api("Process 流程")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/process")
public class ProcessController {

    private final ProcessService processService;

    @ApiOperation("新建流程")
    @PostMapping("/create")
    public ResponseEntity<Result<Process>> create(@RequestBody ProcessCreateForm form) {
        return ResponseEntity.ok(Result.success(processService.create(form)));
    }

    @ApiOperation("查询流程列表")
    @GetMapping("/list")
    public ResponseEntity<Result<Page<Process>>> processList(@ModelAttribute ProcessQueryForm form) {
        return ResponseEntity.ok(Result.success(processService.processList(form)));
    }

    @ApiOperation("变更流程启用状态")
    @PostMapping("/changeActive")
    public ResponseEntity<Result<ProcessActiveStatusDto>> changeActive(@Valid @RequestBody ProcessChangeActiveForm form) {
        return ResponseEntity.ok(Result.success(processService.changeActive(form)));
    }

    @ApiOperation("变更流程启用版本")
    @PostMapping("/changeVersion")
    public ResponseEntity<Result<?>> changeActiveVersion(@Valid @RequestBody ProcessChangeVersionForm form) {
        processService.changeActiveVersion(form);
        return ResponseEntity.ok(Result.success());
    }

    @ApiOperation("变更流程启用版本")
    @GetMapping("/activeHistory")
    public ResponseEntity<Result<List<ActiveVersionHistoryDto>>> activeVersionHistory(@RequestParam String name) {
        return ResponseEntity.ok(Result.success(processService.activeVersionHistory(name)));
    }

    @ApiOperation("流程统计查询")
    @PostMapping("/statistics")
    public ResponseEntity<Result<List<QueryProcessStatisticsDto>>> QueryProcessStatistics(@RequestBody QueryProcessStatisticsForm form) {
        return ResponseEntity.ok(Result.success(processService.queryProcessForStatistics(form)));
    }

}
