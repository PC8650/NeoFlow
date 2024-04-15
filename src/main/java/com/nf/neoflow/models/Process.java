package com.nf.neoflow.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nf.neoflow.constants.TimeFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Process 流程
 * @author PC8650
 */
@Data
@NoArgsConstructor
@Node
public class Process {

    @Id
    @GeneratedValue
    private Long id;

    @ApiModelProperty("流程名称")
    private String name;

    @ApiModelProperty("是否启用")
    private Boolean active = false;

    @ApiModelProperty("启用版本")
    private Integer activeVersion;

    @ApiModelProperty("版本启用历史")
    private List<Integer> activeHistory;

    @ApiModelProperty("创建者唯一标识")
    private String createBy;

    @ApiModelProperty("更新者唯一标识")
    private String updateBy;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;

    @ApiModelProperty("修改时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime updateTime;

    public Process(String name, String createBy) {
        this.name = name;
        this.createBy = createBy;
        this.updateBy = createBy;
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

}
