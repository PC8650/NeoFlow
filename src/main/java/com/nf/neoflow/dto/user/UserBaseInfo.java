package com.nf.neoflow.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nf.neoflow.exception.NeoUserException;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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
