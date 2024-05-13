package com.nf.neoflow.controller;

import com.nf.neoflow.dto.execute.ExecuteForm;
import com.nf.neoflow.dto.response.Result;
import com.nf.neoflow.service.ExecuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "执行流程")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}")
public class ExecuteController {

    private final ExecuteService executeService;

    @Operation(description = "执行流程")
    @PostMapping("/execute")
    public ResponseEntity<Result<?>> execute(@Valid @RequestBody ExecuteForm form) {
        executeService.execute(form);
        return ResponseEntity.ok(Result.success());
    }

    @Operation(description = "执行自动节点")
    @GetMapping("/auto_execute")
    public ResponseEntity<Result<?>> autoExecute(@RequestParam(required = false) LocalDate date) {
        executeService.executeAutoNode(date);
        return ResponseEntity.ok(Result.success());
    }

}
