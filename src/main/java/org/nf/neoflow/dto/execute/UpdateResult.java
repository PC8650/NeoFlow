package org.nf.neoflow.dto.execute;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nf.neoflow.models.InstanceNode;

/**
 * 更新结果
 * @author PC8650
 */
public record UpdateResult(
        @Schema(name = "此次执行表单") ExecuteForm form,
        @Schema(name = "下一实例节点") InstanceNode next,
        @Schema(name = "是否立即自动执行下一节点") Boolean autoRightNow,
        @Schema(name = "是否获取锁") Boolean getLock
){
}
