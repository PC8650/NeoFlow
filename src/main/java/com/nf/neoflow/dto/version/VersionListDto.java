package com.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版本列表
 * @author PC8650
 */
@Data
public class VersionListDto {

    @ApiModelProperty("流程名称")
    private String processName;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("迭代自版本号")
    private Integer iterateFrom;

    @ApiModelProperty("允许退回发起的次数")
    private Integer cycle = 0;

    @ApiModelProperty("创建人标识")
    private String createBy;

    @ApiModelProperty("创建人名称")
    private String createByName;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;
}
