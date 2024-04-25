package com.nf.neoflow.repository;

import com.nf.neoflow.dto.execute.NodeQueryDto;
import com.nf.neoflow.models.ModelNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * 模型节点仓库映射
 * @author PC8650
 */
public interface ModelNodeRepository extends Neo4jRepository<ModelNode, Long> {

    /**
     * 查询流程当前激活版本模型的第一个节点
     * @param processName 流程名称
     * @return ModelNodeDto
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:ACTIVE]->(v:Version)-[:MODEL]->(f:ModelNode) where f is not null
        return v.version as version,
        apoc.convert.toJson(apoc.map.merge(properties(f),{id:id(f)})) as nodeJson
    """)
    NodeQueryDto<ModelNode> queryActiveVersionModelFirstNode(String processName);

    /**
     * 查询下一模型节点
     * @param processName 流程名称
     * @param version 版本
     * @param nodeUid 当前模型节点uid
     * @param condition 跳转条件
     * @return ModelNode
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:MODEL]->(f:ModelNode) where f is not null
        with f, [(f)-[:NEXT*0..]->(c:ModelNode{nodeUid:$2}) | c][0] as c
        optional match (c)-[:NEXT{condition:$3}]->(n:ModelNode)
        return n
    """)
    ModelNode queryNextModelNode(String processName, Integer version, String nodeUid, Integer condition);

    /**
     * 查询中间节点是否可拒绝
     * @param processName 流程名称
     * @param version 版本
     * @param nodeUid 节点uid
     * @return ModelNode
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:MODEL]->(f:ModelNode) where f is not null
        with f, [(f)-[:NEXT*]->(c:ModelNode{nodeUid:$2}) | c][0] as c
        optional match (c)-[:NEXT]->(t:ModelNode{location:4})
        return t
    """)
    ModelNode MiddleNodeCanReject(String processName, Integer version, String nodeUid);

    /**
     * 查询模型终止节点
     * @param processName 流程名称
     * @param version 版本
     * @return ModelNode
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:MODEL]->(f:ModelNode) where f is not null
        with f, [(f)-[:NEXT*]->(t:ModelNode{location:4}) | t][0] as t
        return t
    """)
    ModelNode queryModelTerminateNode(String processName, Integer version);
}
