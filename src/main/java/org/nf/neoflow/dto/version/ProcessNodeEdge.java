package org.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.exception.NeoProcessException;

import java.util.List;

/**
 * 流程节点边
 * @author PC8650
 */
@Data
@JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
public class ProcessNodeEdge {

    @Schema(name = "开始节点标识")
    String startNode;

    @Schema(name = "结束节点标识")
    String endNode;

    @Schema(name = "条件")
    Integer condition;

    @Schema(name = "起始坐标", nullable = true)
    List<Double> startLocation;

    @Schema(name = "结束坐标", nullable = true)
    List<Double> endLocation;

    public void check() {
        if (StringUtils.isBlank(startNode) || StringUtils.isBlank(endNode)) {
            throw new NeoProcessException("边端点不能为空");
        }
        if (condition == null) {
            throw new NeoProcessException("边条件不能为空");
        }
    }
}
