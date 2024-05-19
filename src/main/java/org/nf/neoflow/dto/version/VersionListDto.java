package org.nf.neoflow.dto.version;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.nf.neoflow.constants.TimeFormat;

import java.time.LocalDateTime;

/**
 * 版本列表
 * @author PC8650
 */
@Data
public class VersionListDto {

    @Schema(name = "流程名称")
    private String processName;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "迭代自版本号")
    private Integer iterateFrom;

    @Schema(name = "允许退回发起的次数")
    private Integer cycle = 0;

    @Schema(name = "终止方法")
    private String terminateMethod;

    @Schema(name = "创建人标识")
    private String createBy;

    @Schema(name = "创建人名称")
    private String createByName;

    @Schema(name = "创建时间")
    @JsonFormat(pattern = TimeFormat.DATE_TIME)
    private LocalDateTime createTime;
}
