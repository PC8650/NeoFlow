package org.nf.neoflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nf.neoflow.component.BusinessOperatorManager;
import org.nf.neoflow.dto.execute.ExecuteForm;
import org.nf.neoflow.dto.response.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程引擎远程调用controller
 * 由流程引擎远程调用，触发配置的业务方法
 */
@Tag(name = "流程引擎远程调用业务执行")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/neo/remote")
public class RemoteOperateController {

    private final BusinessOperatorManager manager;

    @Operation(description = "执行业务方法")
    @PostMapping("/execute")
    public Result<ExecuteForm> execute(@RequestBody ExecuteForm form) {
        return Result.success(manager.operate(form));
    }
}
