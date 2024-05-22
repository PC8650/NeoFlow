package org.nf.neoflow.dto.execute;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 批量执行结果
 * @author PC8650
 */
public record BatchResultDto (
        @Schema(name = "总数")
        Integer size,

        @Schema(name = "成功数")
        Integer success,

        @Schema(name = "失败数")
        Integer fail,

        @Schema(name = "成功列表")
        List<String> successList,

        @Schema(name = "失败列表")
        List<String> failList
) {
}
