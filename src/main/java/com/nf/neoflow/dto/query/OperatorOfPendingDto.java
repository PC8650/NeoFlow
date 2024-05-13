package com.nf.neoflow.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 当前用户在各流程的待办数量
 * @author PC8650
 */
@Data
public class OperatorOfPendingDto {

    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "待办数量")
    private Integer count;

    @Schema(name = "版本信息")
    List<Map<String, Object>> version;

}
