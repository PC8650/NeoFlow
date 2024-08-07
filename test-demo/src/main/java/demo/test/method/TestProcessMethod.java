package demo.test.method;

import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.annotation.ProcessMethod;
import org.nf.neoflow.annotation.ProcessOperator;
import org.nf.neoflow.dto.execute.ExecuteForm;

import java.util.UUID;

@ProcessOperator(name = "划款")
public class TestProcessMethod {

    @ProcessMethod(name = "begin")
    private ExecuteForm begin(ExecuteForm x) {
        System.out.println("begin");
        if (StringUtils.isBlank(x.getBusinessKey())) {
            x.setBusinessKey(UUID.randomUUID().toString());
        }
//        x.setProcessName("a");
//        x.setCondition(1);
        return x;
    }

    @ProcessMethod(name = "verify")
    private ExecuteForm verify(ExecuteForm x) {
        System.out.println("verify");
        x.setCondition(3);
//        if (x.getOperationType() == 2) {
//            x.setCondition(1);
//        }else if (x.getOperationType() == 3) {
//            x.setCondition(2);
//        }
//        int i = 1 / 0;
        return x;
    }

}
