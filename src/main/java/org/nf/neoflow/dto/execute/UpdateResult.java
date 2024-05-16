package org.nf.neoflow.dto.execute;

import org.nf.neoflow.models.InstanceNode;

/**
 * 更新结果
 * @author PC8650
 */
public record UpdateResult(ExecuteForm form, InstanceNode next, Boolean autoRigNow, Boolean getLock){

}
