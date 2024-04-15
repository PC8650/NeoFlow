package com.nf.neoflow.controller;

import com.nf.neoflow.dto.process.*;
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
    public ResponseEntity<Process> create(@RequestBody ProcessCreateForm form) {
        return ResponseEntity.ok(processService.create(form));
    }

    @ApiOperation("查询流程列表")
    @GetMapping("/list")
    public ResponseEntity<Page<Process>> processList(@ModelAttribute ProcessQueryForm form) {
        return ResponseEntity.ok(processService.processList(form));
    }

    @ApiOperation("变更流程启用状态")
    @PostMapping("/changeActive")
    public ResponseEntity<ProcessActiveStatusDto> changeActive(@Valid @RequestBody ProcessChangeActiveForm form) {
        return ResponseEntity.ok(processService.changeActive(form));
    }

    @ApiOperation("变更流程启用版本")
    @PostMapping("/changeVersion")
    public ResponseEntity<Integer> changeActiveVersion(@Valid @RequestBody ProcessChangeVersionForm form) {
        return ResponseEntity.ok(processService.changeActiveVersion(form));
    }

    @ApiOperation("变更流程启用版本")
    @GetMapping("/activeHistory")
    @ApiImplicitParam(name = "name", value = "流程名称", required = true, dataType = "String", paramType = "query")
    public ResponseEntity<List<ActiveVersionHistoryDto>> activeVersionHistory(@RequestParam String name) {
        return ResponseEntity.ok(processService.activeVersionHistory(name));
    }
}
