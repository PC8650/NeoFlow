package org.nf.neoflow.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nf.neoflow.constants.TimeFormat;
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

    @Schema(name = "流程名称")
    private String name;

    @Schema(name = "是否启用")
    private Boolean active = false;

    @Schema(name = "启用版本")
    private Integer activeVersion;

    @Schema(name = "版本启用历史")
    private List<Integer> activeHistory;

    @Schema(name = "创建者唯一标识")
    private String createBy;

    @Schema(name = "更新者唯一标识")
    private String updateBy;

    @Schema(name = "创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;

    @Schema(name = "修改时间")
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
