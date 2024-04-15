package com.nf.neoflow.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.dto.version.ModelNodeDto;
import com.nf.neoflow.dto.version.ProcessNodeEdge;
import com.nf.neoflow.dto.version.VersionModelCreateForm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestCreateVersion {
    public static void main(String[] args) throws Exception {
        Set<ModelNodeDto> nodes = new HashSet<>();
        Set<ProcessNodeEdge> edges = new HashSet<>();

        // 添加发起节点
        ModelNodeDto startNode = new ModelNodeDto();
        startNode.setNodeUid("1");
        startNode.setName("发起");
        startNode.setIdentity("Start");
        startNode.setOperationType(0);
        startNode.setOperationMethod("begin");
        startNode.setAutoInterval(0);
        startNode.setLocation(1);
        nodes.add(startNode);

        ProcessNodeEdge startEdge = new ProcessNodeEdge();
        startEdge.setStartNode("1");
        startEdge.setEndNode("2");
        startEdge.setCondition(1);
        edges.add(startEdge);

        // 生成中间节点和边
        for (int i = 2; i <= 101; i++) {
            ModelNodeDto middleNode = new ModelNodeDto();
            middleNode.setNodeUid(String.valueOf(i));
            middleNode.setName("中间节点" + (i - 1));
            middleNode.setIdentity("Middle" + (i - 1));
            middleNode.setOperationType(2);

            List<UserBaseInfo> candidates = new ArrayList<>();
            UserBaseInfo user = new UserBaseInfo();
            user.setId("2");
            user.setName("张三");
            candidates.add(user);

            middleNode.setOperationCandidateInfo(candidates);
            middleNode.setOperationMethod("verify");
            middleNode.setLocation(2);
            nodes.add(middleNode);

            // 连接到下一个节点或终止节点
            if (i < 101) {
                ProcessNodeEdge edge = new ProcessNodeEdge();
                edge.setStartNode(String.valueOf(i));
                edge.setEndNode(String.valueOf(i + 1));
                edge.setCondition(1);
                edges.add(edge);
            }
            ProcessNodeEdge rejectEdge = new ProcessNodeEdge();
            rejectEdge.setStartNode(String.valueOf(i));
            rejectEdge.setEndNode("103");
            rejectEdge.setCondition(2);
            edges.add(rejectEdge);
        }

        // 添加完成节点和终止节点
        ModelNodeDto completeNode = new ModelNodeDto();
        completeNode.setNodeUid("102");
        completeNode.setName("完成");
        completeNode.setIdentity("Complete");
        completeNode.setOperationType(0);
        completeNode.setOperationMethod("");
        completeNode.setAutoInterval(0);
        completeNode.setLocation(3);
        nodes.add(completeNode);

        ProcessNodeEdge completeEdge = new ProcessNodeEdge();
        completeEdge.setStartNode("101");
        completeEdge.setEndNode("102");
        completeEdge.setCondition(1);
        edges.add(completeEdge);


        ModelNodeDto terminateNode = new ModelNodeDto();
        terminateNode.setNodeUid("103");
        terminateNode.setName("终止");
        terminateNode.setIdentity("Terminate");
        terminateNode.setOperationType(0);
        terminateNode.setOperationMethod("");
        terminateNode.setAutoInterval(0);
        terminateNode.setLocation(4);
        nodes.add(terminateNode);

        // 使用Jackson序列化为JSON
        VersionModelCreateForm form = new VersionModelCreateForm();
        form.setNodes(nodes);
        form.setEdges(edges);
        form.setProcessName("划款");
        form.setCycle(1);
        form.setCreateBy("2");
        form.setCreateByName("张三");
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(form));
    }
}