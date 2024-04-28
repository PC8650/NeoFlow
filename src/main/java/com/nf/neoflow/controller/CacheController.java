package com.nf.neoflow.controller;

import com.nf.neoflow.component.NeoCacheManager;
import com.nf.neoflow.dto.response.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("缓存")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/cache")
public class CacheController {

    private final NeoCacheManager cacheManager;


    @ApiOperation("缓存统计")
    @GetMapping("/statistics")
    public ResponseEntity<Result<?>> statistics() {
        return ResponseEntity.ok(Result.success(cacheManager.cacheStatistics()));
    }

}
