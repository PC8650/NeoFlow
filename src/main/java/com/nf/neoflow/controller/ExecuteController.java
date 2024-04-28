package com.nf.neoflow.controller;

import com.nf.neoflow.dto.execute.ExecuteForm;
import com.nf.neoflow.dto.response.Result;
import com.nf.neoflow.service.ExecuteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("执行流程")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}")
public class ExecuteController {

    private final ExecuteService executeService;

    @ApiOperation("执行流程")
    @PostMapping("/execute")
    public ResponseEntity<Result<?>> execute(@Valid @RequestBody ExecuteForm form) {
        executeService.execute(form);
        return ResponseEntity.ok(Result.success());
    }

}
