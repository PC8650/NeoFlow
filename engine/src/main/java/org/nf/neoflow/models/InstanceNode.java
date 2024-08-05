package org.nf.neoflow.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * InstanceNode 流程版本实例节点
 * @author PC8650
 */
@Data
@Node
public class InstanceNode {

    @Id
    @GeneratedValue
    private Long id;

    @Schema(name = "对应的模型节点uid")
    private String modelNodeUid;

    @Schema(name = "节点名称")
    private String name;

    @Schema(name = "节点标识", description = "可用于需对特殊节点做处理的业务")
    private String identity;

    @Schema(name = "节点状态", description = "1-待办，2-同意，3-拒绝，4-转发")
    private Integer status;

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

    @Schema(name = "自动执行日期", description = "有值将忽略操作类型和候选人")
    private LocalDate autoTime;

    @Schema(name = "通过时默认的跳转条件", description = "跳转条件缺失时默认选择改值，配合自动节点")
    private Integer defaultPassCondition;

    @Schema(name = "节点位置", description = "1-开始，2-中间，3-完成，4-终止")
    private Integer location;

    @Schema(name = "实际操作人")
    private String operationBy;

    @Schema(name = "操作备注")
    private String operationRemark;

    @Schema(name = "节点开始时间")
    private LocalDateTime beginTime;

    @Schema(name = "节点结束时间")
    private LocalDateTime endTime;

    @Schema(name = "节点持续时间")
    private String during;

    @Schema(name = "流程持续时间")
    private String processDuring;

    @Schema(name = "版本移植")
    private String graft;

    @Schema(name = "流程业务变量数据",
            description = """
                序列化的业务数据，用于保留会随流程节点变动，但又需要在变动 前/后 留痕的数据(保留变动 前/后 可由实际需求决定)。
                建议只保留需要因变动留痕的关键数据。
                自动节点需要配合节点方法才能传递。
            """)
    private String variableData;

    /**
     * 复制忽略属性
     * @return String[]
     */
    public String[] ignoreCopyPropertyListOfForward() {
        return new String[]{
                "id","status",
                "graft",
                "autoTime",
                "operationBy",
                "operationType",
                "operationCandidate",
                "operationCandidateInfo",
                "beginTime", "endTime", "during"
        };
    }

}
