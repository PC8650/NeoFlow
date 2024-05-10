package com.nf.neoflow.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户基础信息
 * @author PC8650
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserBaseInfo {

    @ApiModelProperty("唯一标识")
    private String id;

    @ApiModelProperty("名称")
    private String name;
}
