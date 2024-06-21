package org.nf.neoflow.utils;

import org.apache.commons.lang3.StringUtils;
import org.nf.neoflow.constants.NodeLocationType;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.dto.version.ModelNodeDto;
import org.nf.neoflow.dto.version.ProcessNodeEdge;
import org.nf.neoflow.exception.NeoProcessException;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 模型检查
 * @author PC8650
 */
public class ModelCheckUtils {

    /**
     * 校验要创建的model数据，入库准备
     * @param initiatorFlag 发起人标识
     * @param nodes 节点
     * @param edges 边
     * @param nodesSet nodes map集合
     * @param edgesSet edges map集合
     */
    public static void validateModel(int initiatorFlag,
                                     Set<ModelNodeDto> nodes, Set<ProcessNodeEdge> edges,
                                     Set<Map<String,Object>> nodesSet, Set<Map<String,Object>> edgesSet){
        Map<String, ModelNodeDto> nodeMap = new HashMap<>(nodes.size());
        validateNodes(initiatorFlag, nodes, nodeMap, nodesSet);
        validateEdges(edges, nodeMap, edgesSet);
    }

    /**
     * 校验节点，入库准备
     * @param initiatorFlag 发起人标识
     * @param nodes 节点
     * @param nodeMap 节点map
     * @param nodesSet 节点map集合，用于入库参数
     */
    private static void validateNodes(int initiatorFlag,
                                      Set<ModelNodeDto> nodes,
                                      Map<String, ModelNodeDto> nodeMap,
                                      Set<Map<String,Object>> nodesSet){
        int startCount = 0, completeCount = 0, terminateCount = 0;
        Map<String,Object> dtoMap;
        Set<String> nodeUidSet = new HashSet<>(nodes.size());
        for (ModelNodeDto node : nodes) {
            //自检基本属性
            node.check();

            //候选人
            if (node.getAutoInterval() == null && !Objects.equals(initiatorFlag, node.getOperationType())) {
                if (CollectionUtils.isEmpty(node.getOperationCandidateInfo())) {
                    throw new NeoProcessException("节点候选人不能为空");
                }
                for (UserBaseInfo userBaseInfo : node.getOperationCandidateInfo()) {
                    if (StringUtils.isBlank(userBaseInfo.getId()) || StringUtils.isBlank(userBaseInfo.getName())) {
                        throw new NeoProcessException("节点候选人信息缺失");
                    }
                }
            }

            //uid
            if (!nodeUidSet.add(node.getNodeUid())) {
                throw new NeoProcessException(String.format("节点uid重复：%s", node.getNodeUid()));
            }

            //特殊节点数量
            if (Objects.equals(node.getLocation(), NodeLocationType.INITIATE)) {
                startCount++;
            } else if (Objects.equals(node.getLocation(), NodeLocationType.COMPLETE)) {
                completeCount++;
            } else if (Objects.equals(node.getLocation(), NodeLocationType.TERMINATE)) {
                terminateCount++;
            }

            nodeMap.put(node.getNodeUid(), node);
            dtoMap = JacksonUtils.objToMap(node);
            if (!CollectionUtils.isEmpty(node.getOperationCandidateInfo())) {
                String candidate = JacksonUtils.toJson(node.getOperationCandidateInfo());
                dtoMap.put("operationCandidate", candidate);
                dtoMap.remove("operationCandidateInfo");
            }
            nodesSet.add(dtoMap);
        }

        if (startCount != 1 || completeCount != 1) {
            throw new NeoProcessException("开始节点/完成节点、终止节点有且只能存在一个");
        }
        if (terminateCount > 1) {
            throw new NeoProcessException("终止节点最多只能存在一个");
        }
    }

    /**
     * 校验边，入库准备
     * @param edges 边
     * @param nodeMap 节点map
     * @param edgesSet 边map集合，用于入库参数
     */
    private static void validateEdges(Set<ProcessNodeEdge> edges, Map<String, ModelNodeDto> nodeMap, Set<Map<String,Object>> edgesSet){
        ModelNodeDto startNode;
        ModelNodeDto endNode;
        String startNodeUid;
        String endNodeUid;
        Set<String> includeNodes = new HashSet<>(nodeMap.size());
        Set<String> conditionSet = new HashSet<>(edges.size());
        for (ProcessNodeEdge edge : edges) {
            //自检基本属性
            edge.check();

            startNodeUid = edge.getStartNode();
            endNodeUid = edge.getEndNode();
            //校验节点存在性
            if (!nodeMap.containsKey(startNodeUid) || !nodeMap.containsKey(endNodeUid)) {
                throw new NeoProcessException(String.format("边端点不存在 (%s)-->(%s)", startNodeUid, endNodeUid));
            }

            //校验条件重复
            String condition = String.format("(%s)-(%s)->", startNodeUid,edge.getCondition());
            if (!conditionSet.add(condition)) {
                throw new NeoProcessException(String.format("条件重复 %s", condition));
            }

            //校验结构
            startNode = nodeMap.get(startNodeUid);
            endNode = nodeMap.get(endNodeUid);
            if (Objects.equals(endNode.getLocation(), NodeLocationType.INITIATE)) {
                throw new NeoProcessException("不能指向开始节点");
            }
            if (Objects.equals(startNode.getLocation(), NodeLocationType.COMPLETE)
                    || Objects.equals(startNode.getLocation(), NodeLocationType.TERMINATE)) {
                throw new NeoProcessException("完成/终止 节点不能有后续");
            }

            includeNodes.add(startNodeUid);
            includeNodes.add(endNodeUid);

            Map<String,Object> edgeMap = JacksonUtils.objToMap(edge);
            edgesSet.add(edgeMap);
        }

        if (includeNodes.size() != nodeMap.size()) {
            throw new NeoProcessException("存在节点未包含在边中");
        }
    }

    /**
     * 校验要创建的model数据，构建接口json
     * @param initiatorFlag 发起人标识
     * @param nodes 节点
     * @param edges 边
     */
    public static void validateModel(int initiatorFlag,
                                     Set<ModelNodeDto> nodes, Set<ProcessNodeEdge> edges){
        Map<String, ModelNodeDto> nodeMap = new HashMap<>(nodes.size());
        validateNodes(initiatorFlag, nodes, nodeMap);
        validateEdges(edges, nodeMap);
    }

    /**
     * 校验节点，构建接口json
     * @param initiatorFlag 发起人标识
     * @param nodes 节点
     * @param nodeMap 节点map
     */
    private static void validateNodes(int initiatorFlag,
                                      Set<ModelNodeDto> nodes,
                                      Map<String, ModelNodeDto> nodeMap){
        int startCount = 0, completeCount = 0, terminateCount = 0;
        Map<String,Object> dtoMap;
        Set<String> nodeUidSet = new HashSet<>(nodes.size());
        for (ModelNodeDto node : nodes) {
            //自检基本属性
            node.check();

            //候选人
            if (node.getAutoInterval() == null && !Objects.equals(initiatorFlag, node.getOperationType())) {
                if (CollectionUtils.isEmpty(node.getOperationCandidateInfo())) {
                    throw new NeoProcessException("节点候选人不能为空");
                }
                for (UserBaseInfo userBaseInfo : node.getOperationCandidateInfo()) {
                    if (StringUtils.isBlank(userBaseInfo.getId()) || StringUtils.isBlank(userBaseInfo.getName())) {
                        throw new NeoProcessException("节点候选人信息缺失");
                    }
                }
            }

            //uid
            if (!nodeUidSet.add(node.getNodeUid())) {
                throw new NeoProcessException(String.format("节点uid重复：%s", node.getNodeUid()));
            }

            //特殊节点数量
            if (Objects.equals(node.getLocation(), NodeLocationType.INITIATE)) {
                startCount++;
            } else if (Objects.equals(node.getLocation(), NodeLocationType.COMPLETE)) {
                completeCount++;
            } else if (Objects.equals(node.getLocation(), NodeLocationType.TERMINATE)) {
                terminateCount++;
            }

            nodeMap.put(node.getNodeUid(), node);
            dtoMap = JacksonUtils.objToMap(node);
            if (!CollectionUtils.isEmpty(node.getOperationCandidateInfo())) {
                String candidate = JacksonUtils.toJson(node.getOperationCandidateInfo());
                dtoMap.put("operationCandidate", candidate);
                dtoMap.remove("operationCandidateInfo");
            }
        }

        if (startCount != 1 || completeCount != 1) {
            throw new NeoProcessException("开始节点/完成节点、终止节点有且只能存在一个");
        }
        if (terminateCount > 1) {
            throw new NeoProcessException("终止节点最多只能存在一个");
        }
    }

    /**
     * 校验边，构建接口json
     * @param edges 边
     * @param nodeMap 节点map
     */
    private static void validateEdges(Set<ProcessNodeEdge> edges, Map<String, ModelNodeDto> nodeMap){
        ModelNodeDto startNode;
        ModelNodeDto endNode;
        String startNodeUid;
        String endNodeUid;
        Set<String> includeNodes = new HashSet<>(nodeMap.size());
        Set<String> conditionSet = new HashSet<>(edges.size());
        for (ProcessNodeEdge edge : edges) {
            //自检基本属性
            edge.check();

            startNodeUid = edge.getStartNode();
            endNodeUid = edge.getEndNode();
            //校验节点存在性
            if (!nodeMap.containsKey(startNodeUid) || !nodeMap.containsKey(endNodeUid)) {
                throw new NeoProcessException(String.format("边端点不存在 (%s)-->(%s)", startNodeUid, endNodeUid));
            }

            //校验条件重复
            String condition = String.format("(%s)-(%s)->", startNodeUid,edge.getCondition());
            if (!conditionSet.add(condition)) {
                throw new NeoProcessException(String.format("条件重复 %s", condition));
            }

            //校验结构
            startNode = nodeMap.get(startNodeUid);
            endNode = nodeMap.get(endNodeUid);
            if (Objects.equals(endNode.getLocation(), NodeLocationType.INITIATE)) {
                throw new NeoProcessException("不能指向开始节点");
            }
            if (Objects.equals(startNode.getLocation(), NodeLocationType.COMPLETE)
                    || Objects.equals(startNode.getLocation(), NodeLocationType.TERMINATE)) {
                throw new NeoProcessException("完成/终止 节点不能有后续");
            }

            includeNodes.add(startNodeUid);
            includeNodes.add(endNodeUid);
        }

        if (includeNodes.size() != nodeMap.size()) {
            throw new NeoProcessException("存在节点未包含在边中");
        }
    }

}
