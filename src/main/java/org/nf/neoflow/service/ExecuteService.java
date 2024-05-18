package org.nf.neoflow.service;

import lombok.AllArgsConstructor;
import org.nf.neoflow.component.FlowExecutor;
import org.nf.neoflow.dto.execute.BatchResultDto;
import org.nf.neoflow.dto.execute.ExecuteForm;
import org.nf.neoflow.dto.execute.GraftForm;
import org.nf.neoflow.dto.execute.GraftResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

/**
 * 执行服务
 * @author PC8650
 */
@AllArgsConstructor
@Service
public class ExecuteService {

    private final FlowExecutor flowExecutor;

    /**
     * 执行流程
     * @param form 表单
     */
    public void execute(ExecuteForm form) {
        flowExecutor.executor(form);
    }

    /**
     * 批量执行流程
     * @param forms 表单
     */
    public BatchResultDto executeBatch(Set<ExecuteForm> forms) {
        return flowExecutor.batchOperation(forms, ExecuteForm.class);
    }

    /**
     * 流程实例移植版本
     * @param form 表单
     * @return GraftResult
     */
    public GraftResult instanceVersionGraft(GraftForm form) {
        flowExecutor.instanceVersionGraft(form);
        return new GraftResult(form.getProcessName(), form.getGraftVersion(), form.getNum(), form.getNodeId(), form.getBusinessKey());
    }

    /**
     * 批量移植流程实例版本
     * @param forms 表单
     */
    public BatchResultDto graftBatch(Set<GraftForm> forms) {
        return flowExecutor.batchOperation(forms, GraftForm.class);
    }

    /**
     * 执行自动节点
     * @param date 日期
     */
    public void executeAutoNode(LocalDate date){
        flowExecutor.autoScan(date);

    }

}
