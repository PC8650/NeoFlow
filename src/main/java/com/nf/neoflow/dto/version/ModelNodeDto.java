package com.nf.neoflow.dto.version;

import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.exception.NeoProcessException;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 流程模型节点
 * @author PC8650
 */
@Data
public class ModelNodeDto {

    @ApiModelProperty("创建模型时前端生成的uid")
    private String nodeUid;

    @ApiModelProperty("节点名称")
    private String name;

    @ApiModelProperty("节点标识，可用于需对特殊节点做处理的业务")
    private String identity;

    @ApiModelProperty("节点操作类型，根据业务自定义")
    private Integer operationType;

    @ApiModelProperty("指定节点操作候选人，配合operationType自定义")
    private List<UserBaseInfo> operationCandidateInfo;

    @ApiModelProperty("节点操作方法，对应的@ProcessMethod")
    private String operationMethod;

    @ApiModelProperty("自动执行间隔，只精确到日期（x 天后，x <= 0 立即自动执行），有值将忽略操作类型和候选人")
    private Integer autoInterval;

    @ApiModelProperty("节点位置：1-开始，2-中间，3-完成，4-终止")
    private Integer location;

    @ApiModelProperty("x坐标")
    private Double x;

    @ApiModelProperty("y坐标")
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
        if (autoInterval == null) {
            if (CollectionUtils.isEmpty(operationCandidateInfo)) {
                throw new NeoProcessException("节点候选人不能为空");
            }
            for (UserBaseInfo userBaseInfo : operationCandidateInfo) {
                if (StringUtils.isBlank(userBaseInfo.getId()) || StringUtils.isBlank(userBaseInfo.getName())) {
                    throw new NeoProcessException("节点候选人信息缺失");
                }
            }
        }
        if (x == null || y == null) {
            throw new NeoProcessException("屏幕坐标不能为空");
        }
    }

}
