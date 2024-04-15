package com.nf.neoflow.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户基础信息
 * @author PC8650
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserBaseInfo {

    @ApiModelProperty("唯一标识")
    private String id;

    @ApiModelProperty("名称")
    private String name;
}
