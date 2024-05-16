package org.nf.neoflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.nf.neoflow.dto.execute.BatchResultDto;
import org.nf.neoflow.dto.execute.ExecuteForm;
import org.nf.neoflow.dto.response.Result;
import org.nf.neoflow.service.ExecuteService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Set;

@Tag(name = "执行流程")
@Validated
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

    @Operation(description = "批量执行流程")
    @PostMapping("/batch_execute")
    public ResponseEntity<Result<BatchResultDto>> batchExecute(@Valid @RequestBody Set<ExecuteForm> forms) {
        return ResponseEntity.ok(Result.success(executeService.executeBatch(forms)));
    }

    @Operation(description = "执行自动节点")
    @GetMapping("/auto_execute")
    public ResponseEntity<Result<?>> autoExecute(@RequestParam(required = false) LocalDate date) {
        executeService.executeAutoNode(date);
        return ResponseEntity.ok(Result.success());
    }

}
