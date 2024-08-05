package org.nf.neoflow.dto.test;

import org.nf.neoflow.utils.FlowBuilderUtil;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.utils.NodeTemplateUtils;

import java.util.List;

public class TestFlowBuilder {
    public static void main(String[] args) {

        String verify = "verify";


        UserBaseInfo user = new UserBaseInfo();
        user.setId("2");
        user.setName("张三");
        List<UserBaseInfo> candidates = List.of(user);


        FlowBuilderUtil.Builder builder = FlowBuilderUtil.of("划款")
                //新建一条路径
                .newPath()
                .addNode(1, true, 2,
                        NodeTemplateUtils.middleInit("中间节点1-1", "2")
                                .setIdentity("middle1-1").setOperationType(2)
                                .setOperationMethod(verify).setOperationCandidateInfo(candidates))
                .addNode(1,
                        NodeTemplateUtils.middleInit("中间节点2-1", "3")
                                .setIdentity("middle2-1").setOperationType(0)
                                .setOperationMethod(verify).setOperationCandidateInfo(candidates))
                .complete(1)

                //新建一条路径
                .newPath(1)
                .addNode(3, true, 2,
                        NodeTemplateUtils.middleInit("中间节点1-2", "4")
                                .setIdentity("middle1-2").setOperationType(2)
                                .setOperationMethod(verify).setOperationCandidateInfo(candidates))
                .addNode(1,
                        NodeTemplateUtils.middleInit("中间节点2-2", "5")
                                .setIdentity("middle2-2").setOperationType(2)
                                .setOperationMethod(verify).setOperationCandidateInfo(candidates))
                .complete(1)

                //新建一条路径
                .newPath(1,3)
                .addNode(3,
                        NodeTemplateUtils.middleInit("中间节点1-3", "6")
                                .setIdentity("middle1-3").setOperationType(3)
                                .setAutoInterval(1).setDefaultPassCondition(1))
                .complete(1)

                .startNode(
                        NodeTemplateUtils.startInit("发起", "1")
                                .setIdentity("start").setOperationType(0)
                                .setOperationMethod("begin"))
                .completeNode(
                        NodeTemplateUtils.completeInit("完成", "7")
                                .setIdentity("complete").setOperationType(0))
                .terminateNode(
                        NodeTemplateUtils.terminateInit("终止", "8")
                                .setIdentity("terminate").setOperationType(0))
                .initiatorFlag(0).createBy(user);

        System.out.println(builder.build());
    }

}
