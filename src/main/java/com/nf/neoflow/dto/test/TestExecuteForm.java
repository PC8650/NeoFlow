package com.nf.neoflow.dto.test;

import com.nf.neoflow.constants.InstanceOperationType;
import com.nf.neoflow.dto.execute.ExecuteForm;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.utils.JacksonUtils;

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
