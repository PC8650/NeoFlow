package org.nf.neoflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.dto.query.*;
import org.nf.neoflow.dto.response.Result;
import org.nf.neoflow.service.QueryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "泛用查询")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/query")
public class QueryController {

    private final QueryService queryService;

    @Operation(description = "查询当前用户在各流程的待办数量")
    @GetMapping("/pending")
    public ResponseEntity<Result<List<OperatorOfPendingDto>>> queryForOperatorOfPending(@Valid @ModelAttribute OperatorOfPendingForm form){
        return ResponseEntity.ok(Result.success(queryService.queryForOperatorOfPending(form)));
    }

    @Operation(description = "查询当前用户 发起/待办/已办 列表")
    @GetMapping("/list")
    public ResponseEntity<Result<Page<QueryForOperatorDto>>> queryForOperator(@Valid @ModelAttribute QueryForOperatorForm form) {
        return ResponseEntity.ok(Result.success(queryService.queryForOperator(form)));
    }

    @Operation(description = "通过节点 名称/身份 查询节点状态")
    @GetMapping("/identity")
    public ResponseEntity<Result<Page<QueryOfNodeIdentityDto>>> queryOfNodeIdentity(@Valid @ModelAttribute QueryOfNodeIdentityForm form) {
        if (StringUtils.isBlank(form.getNodeName()) && StringUtils.isBlank(form.getNodeIdentity())) {
            throw new IllegalArgumentException("节点名称/节点身份 不能全为空");
        }

        return ResponseEntity.ok(Result.success(queryService.queryOfNodeIdentity(form)));
    }

    @Operation(description = "查询流程实例操作历史")
    @GetMapping("/history")
    public ResponseEntity<Result<OperationHistoryDto>> queryInstanceOperationHistory(@Valid @ModelAttribute OperationHistoryForm form) {
        return ResponseEntity.ok(Result.success(queryService.queryInstanceOperationHistory(form)));
    }

}
