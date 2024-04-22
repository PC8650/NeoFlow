package com.nf.neoflow.dto.test;

import com.nf.neoflow.annotation.ProcessMethod;
import com.nf.neoflow.annotation.ProcessOperator;
import com.nf.neoflow.dto.execute.ExecuteForm;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * 测试用nod，开发自测防止启动报错
 */
@ProcessOperator(name = "划款")
public class TestNod {

    @ProcessMethod(name = "begin")
    private ExecuteForm begin(ExecuteForm x) {
        System.out.println("begin");
        if (StringUtils.isBlank(x.getBusinessKey())) {
            x.setBusinessKey(UUID.randomUUID().toString());
        }
//        x.setProcessName("a");
        x.setCondition(1);
        return x;
    }

    @ProcessMethod(name = "verify")
    private ExecuteForm verify(ExecuteForm x) {
        System.out.println("verify");
        if (x.getOperationType() == 2) {
            x.setCondition(1);
        }else if (x.getOperationType() == 3) {
            x.setCondition(2);
        }
        return x;
    }


}
