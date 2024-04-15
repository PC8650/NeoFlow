package com.nf.neoflow.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

/**
 * Version 流程版本
 * @author PC8650
 */
@Data
@Node
public class Version {

    @Id
    @GeneratedValue
    private Long id;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("允许退回发起的次数")
    private Integer cycle = 0;

    @ApiModelProperty("创建人标识")
    private String createBy;

    @ApiModelProperty("创建人名称")
    private String createByName;;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;

}
