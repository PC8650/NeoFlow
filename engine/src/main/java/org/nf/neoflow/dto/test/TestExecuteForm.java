package org.nf.neoflow.dto.test;

import org.nf.neoflow.constants.InstanceOperationType;
import org.nf.neoflow.dto.execute.ExecuteForm;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.utils.JacksonUtils;

public class TestExecuteForm {

    public static void main(String[] args) {
        UserBaseInfo userBaseInfo = new UserBaseInfo();
        userBaseInfo.setId("2");
        userBaseInfo.setName("张三");
        ExecuteForm form = new ExecuteForm();
        form.setOperationType(InstanceOperationType.INITIATE)
                .setProcessName("划款")
                .setOperator(userBaseInfo);


        System.out.println(JacksonUtils.toJson(form));
    }

}
