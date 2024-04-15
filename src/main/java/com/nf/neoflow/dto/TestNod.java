package com.nf.neoflow.dto;

import com.nf.neoflow.annotation.ProcessMethod;
import com.nf.neoflow.annotation.ProcessOperator;

/**
 * 测试用nod，开发自测防止启动报错
 */
@ProcessOperator(name = "nod")
public class TestNod {

    @ProcessMethod(name = "me")
    private InstanceNodeForm m(InstanceNodeForm x) {
        System.out.println("m ");
        return x;
    }

    private InstanceNodeForm a(InstanceNodeForm x) {
        System.out.println("a ");
        return x;
    }
}
