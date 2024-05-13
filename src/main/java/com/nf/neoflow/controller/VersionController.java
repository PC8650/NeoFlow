package com.nf.neoflow.controller;

import com.nf.neoflow.dto.response.Result;
import com.nf.neoflow.dto.version.*;
import com.nf.neoflow.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Version 版本")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/version")
public class VersionController {

    private final VersionService versionService;

    @Operation(description = "查询流程版本列表")
    @GetMapping("/list")
    public ResponseEntity<Result<Page<VersionListDto>>> queryList(@Valid @ModelAttribute VersionListQueryForm form) {
        return ResponseEntity.ok(Result.success(versionService.versionList(form)));
    }

    @Operation(description = "查询流程版本视图")
    @GetMapping("/view")
    public ResponseEntity<Result<VersionModelViewDto>> queryList(@Valid @ModelAttribute VersionViewQueryForm form) {
        return ResponseEntity.ok(Result.success(versionService.versionView(form)));
    }

    @Operation(description = "查询流程版本迭代")
    @GetMapping("/iterate")
    public ResponseEntity<Result<?>> iterate(@Valid @ModelAttribute IterateTreeQueryForm form) {
        return ResponseEntity.ok(Result.success(versionService.versionIterateTree(form)));
    }

    @Operation(description = "创建流程版本模型")
    @PostMapping("/create")
    public ResponseEntity<Result<?>> createVersion(@Valid @RequestBody VersionModelCreateForm form) {
        versionService.createVersion(form);
        return ResponseEntity.ok(Result.success());
    }
}
