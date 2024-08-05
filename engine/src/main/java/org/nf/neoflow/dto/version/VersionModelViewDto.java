package org.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nf.neoflow.models.ModelNode;

import java.util.Map;
import java.util.Set;

/**
 * 版本模型视图
 * @author PC8650
 */
@Data
@JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
public class VersionModelViewDto {

    @Schema(name = "流程名称")
    private String processName;

    @Schema(name = "迭代自版本号")
    private Integer iterateFrom;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "允许退回发起的次数")
    private Integer cycle;

    @Schema(name = "终止方法", description = "对应的@ProcessMethod")
    private String terminatedMethod;

    @Schema(name = "组件模型")
    private Map<String,Object> componentModel;

    @Schema(name = "版本节点")
    private Set<ModelNode> versionNodes;

    @Schema(name = "版本边")
    private Set<ProcessNodeEdge> versionEdges;

}
