package com.nf.neoflow.service;

import com.nf.neoflow.component.FlowExecutor;
import com.nf.neoflow.dto.execute.AutoNodeDto;
import com.nf.neoflow.dto.execute.BatchResultDto;
import com.nf.neoflow.dto.execute.ExecuteForm;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
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
     * 批量执行
     * @param forms 表单
     */
    public BatchResultDto executeBatch(Set<ExecuteForm> forms) {
        return flowExecutor.batchToExecutor(forms);
    }

    /**
     * 执行自动节点
     * @param date 日期
     */
    public void executeAutoNode(LocalDate date){
        flowExecutor.autoScan(date);

    }

}
