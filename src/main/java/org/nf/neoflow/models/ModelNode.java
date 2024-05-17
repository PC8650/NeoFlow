package org.nf.neoflow.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.List;
import java.util.Map;

/**
 * ModelNode 流程模型节点
 * @author PC8650
 */
@Data
@Node
public class ModelNode {

    @Id
    @GeneratedValue
    private Long id;

    @Schema(name = "创建模型时前端生成的uid")
    private String nodeUid;

    @Schema(name = "节点名称")
    private String name;

    @Schema(name = "节点标识", description = "可用于需对特殊节点做处理的业务")
    private String identity;

    @Schema(name = "节点操作类型", description = "根据业务自定义")
    private Integer operationType;

    @Schema(name = "指定节点操作候选人", description = "配合operationType自定义")
    private String operationCandidate;

    @Schema(name = "接收传递候选人信息")
    private List<Map<String,Object>> operationCandidateInfo;

    @Schema(name = "节点操作方法", description = "对应的@ProcessMethod")
    private String operationMethod;

    @Schema(name = "是否只通过才执行方法")
    private Boolean onlyPassExecute;

    @Schema(name = "自动执行间隔",description = "只精确到日期（x 天后，x <= 0 立即自动执行），有值将忽略操作类型和候选人")
    private Integer autoInterval;

    @Schema(name = "通过时默认的跳转条件", description = "跳转条件缺失时默认选择改值，配合自动节点")
    private Integer defaultPassCondition;

    @Schema(name = "节点位置", description = "1-开始，2-中间，3-完成，4-终止")
    private Integer location;

    @Schema(name = "x坐标")
    private Float x;

    @Schema(name = "y坐标")
    private Float y;

    /**
     * 复制忽略属性
     * @return String[]
     */
    public String[] ignoreCopyPropertyList() {
        return new String[]{
                "id","nodeUid",
                "operationCandidate",
                "operationCandidateInfo",
                "autoInterval",
                "x","y"
        };
    }

}
