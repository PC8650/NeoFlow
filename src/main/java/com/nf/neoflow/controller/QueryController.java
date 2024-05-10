package com.nf.neoflow.controller;

import com.nf.neoflow.dto.query.QueryForOperatorDto;
import com.nf.neoflow.dto.query.QueryForOperatorForm;
import com.nf.neoflow.dto.response.Result;
import com.nf.neoflow.service.QueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("泛用查询")
@RestController
@AllArgsConstructor
@RequestMapping("${neo.prefix:/neo}/query")
public class QueryController {

    private final QueryService queryService;

    @ApiOperation("查询代表列表")
    @GetMapping("/list")
    public ResponseEntity<Result<Page<QueryForOperatorDto>>> queryForOperatorPending(@Valid @ModelAttribute QueryForOperatorForm form) {
        return ResponseEntity.ok(Result.success(queryService.queryForOperator(form)));
    }
}
