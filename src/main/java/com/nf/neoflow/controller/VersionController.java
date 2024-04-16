package com.nf.neoflow.controller;

import com.nf.neoflow.dto.version.*;
import com.nf.neoflow.service.VersionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Api("Version 版本")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/version")
public class VersionController {

    private final VersionService versionService;

    @ApiOperation("查询流程版本列表")
    @GetMapping("/list")
    public ResponseEntity<Page<VersionListDto>> queryList(@Valid @ModelAttribute VersionListQueryForm form) {
        return ResponseEntity.ok(versionService.versionList(form));
    }

    @ApiOperation("查询流程版本视图")
    @GetMapping("/view")
    public ResponseEntity<VersionModelViewDto> queryList(@Valid @ModelAttribute VersionViewQueryForm form) {
        return ResponseEntity.ok(versionService.versionView(form));
    }

    @ApiOperation("查询流程版本迭代")
    @GetMapping("/iterate")
    public ResponseEntity<?> iterate(@Valid @ModelAttribute IterateTreeQueryForm form) {
        return ResponseEntity.ok(versionService.versionIterateTree(form));
    }

    @ApiOperation("创建流程版本模型")
    @PostMapping("/create")
    public ResponseEntity<String> createVersion(@Valid @RequestBody VersionModelCreateForm form) {
        versionService.createVersion(form);
        return ResponseEntity.ok(null);
    }
}
