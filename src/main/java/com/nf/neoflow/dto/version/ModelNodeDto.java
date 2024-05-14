package com.nf.neoflow.dto.version;

import com.nf.neoflow.constants.NodeLocationType;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.exception.NeoProcessException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 流程模型节点
 * @author PC8650
 */
@Data
public class ModelNodeDto {

    @Schema(name = "创建模型时前端生成的uid")
    private String nodeUid;

    @Schema(name = "节点名称")
    private String name;

    @Schema(name = "节点标识", description = "可用于需对特殊节点做处理的业务")
    private String identity;

    @Schema(name = "节点操作类型", description = "根据业务自定义")
    private Integer operationType;

    @Schema(name = "指定节点操作候选人", description = "配合operationType自定义")
    private List<UserBaseInfo> operationCandidateInfo;

    @Schema(name = "节点操作方法", description = "对应的@ProcessMethod")
    private String operationMethod;

    @Schema(name = "是否只通过才执行方法", nullable = true, defaultValue = "true")
    private Boolean onlyPassExecute = true;

    @Schema(name = "自动执行间隔", description = "只精确到日期（x 天后，x <= 0 立即自动执行），有值将忽略操作类型和候选人")
    private Integer autoInterval;

    @Schema(name = "默认通过时的跳转条件", description = "跳转条件缺失时默认选择改值，配合自动节点")
    private Integer defaultPassCondition;

    @Schema(name = "节点位置", description = "1-开始，2-中间，3-完成，4-终止")
    private Integer location;

    @Schema(name = "x坐标")
    private Double x;

    @Schema(name = "y坐标")
    private Double y;


    public void check() {
        if (StringUtils.isBlank(nodeUid)) {
            throw new NeoProcessException("节点uid不能为空");
        }
        if (StringUtils.isBlank(name)) {
            throw new NeoProcessException("节点名称不能为空");
        }
        if (operationType == null) {
            throw new NeoProcessException("节点操作类型不能为空");
        }
        if (location == null) {
            throw new NeoProcessException("节点位置不能为空");
        }
        if (autoInterval != null && NodeLocationType.MIDDLE.equals(location) && defaultPassCondition == null) {
            throw new NeoProcessException("中间自动节点默认通过跳转条件不能为空");
        }
        if (Objects.equals(location, NodeLocationType.INITIATE) && !Objects.equals(autoInterval, 0)) {
            throw new NeoProcessException("发起节点只能设置为[立即自动执行]");
        }
        if (x == null || y == null) {
            throw new NeoProcessException("节点坐标不能为空");
        }
    }

}
