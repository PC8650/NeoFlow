package com.nf.neoflow.service;

import com.nf.neoflow.component.FlowExecutor;
import com.nf.neoflow.dto.execute.ExecuteForm;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 执行服务
 * @author PC8650
 */
@AllArgsConstructor
@Service
public class ExecuteService {

    private final FlowExecutor flowExecutor;

    public void execute(ExecuteForm form) {
        flowExecutor.executor(form, false);
    }

}
