package org.nf.neoflow.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nf.neoflow.constants.TimeFormat;
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

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "允许退回发起的次数")
    private Integer cycle = 0;

    @Schema(name = "终止方法", description = "对应的@ProcessMethod")
    private String terminatedMethod;

    @Schema(name = "创建人标识")
    private String createBy;

    @Schema(name = "创建人名称")
    private String createByName;;

    @Schema(name = "创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;

}
