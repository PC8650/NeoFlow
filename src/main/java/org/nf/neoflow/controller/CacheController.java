package org.nf.neoflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.nf.neoflow.component.NeoCacheManager;
import org.nf.neoflow.dto.response.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "缓存")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/cache")
public class CacheController {

    private final NeoCacheManager cacheManager;


    @Operation(description = "缓存统计")
    @GetMapping("/statistics")
    public ResponseEntity<Result<?>> statistics() {
        return ResponseEntity.ok(Result.success(cacheManager.cacheStatistics()));
    }

}
