package com.nf.neoflow.repository;

import com.nf.neoflow.dto.execute.NodeQueryDto;
import com.nf.neoflow.models.InstanceNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.Map;

/**
 * 实例节点仓库映射
 * @author PC8650
 */
public interface InstanceNodeRepository extends Neo4jRepository<InstanceNode, Long> {

    /**
     * 查询实例是否存在
     * 发起流程去重
     * @param businessKey 业务key
     * @return Boolean
     */
    @Query("""
        return exists(()-[:BUSINESS{key:$0}]->())
    """)
    Boolean instanceIsExists(String businessKey);

    /**
     * 查询当前实例节点
     * @param processName 流程名称
     * @param businessKey 业务key
     * @param nodeId 节点id
     * @return NodeQueryDto<InstanceNode>
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version)-[:INSTANCE]->(:Instance)-[:BUSINESS{key:$1,status:1}]->(f:InstanceNode)
        optional match path = (f)-[:NEXT*0..]->(b:InstanceNode)-[:NEXT]->(c:InstanceNode) where id(c) = $2
        return v.version as version,
        case when b is null then null
        when b.status = 1 then false
        else true end as before,
        case when id(nodes(path)[$3-1]) = $2 then true
        else false end as match,
        apoc.convert.toJson(apoc.map.merge(properties(c),{id:id(c)})) as nodeJson
    """)
    NodeQueryDto<InstanceNode> queryCurrentInstanceNode(String processName, String businessKey, Long nodeId, Integer num);

    /**
     * 更新流程实例
     * @param processName 流程名称
     * @param version 版本
     * @param nodeId 当前实例节点id
     * @param businessKey 业务key
     * @param condition 跳转条件
     * @param flowStatus 流程状态
     * @param cMap 当前实例节点
     * @param nMap 下一实例节点
     * @return 下一实例节点
     */
    @Query("""
        match (p:Process{name:$processName})
        optional match (p)-[:VERSION]->(v:Version{version:$version})-[:INSTANCE]->(i:Instance) where i is not null
        //更新当前节点和流程状态
        with p, i
        call apoc.do.when (
            $nodeId is null,
            'create (i)-[b:BUSINESS{key: businessKey}]->(c:InstanceNode)
            set b.status = flowStatus, c += cMap
            return c',
            'optional match (i)-[b:BUSINESS{key:$businessKey,status:1}]->(f:InstanceNode) where f is not null
            optional match (f)-[:NEXT*0..]->(c:InstanceNode) where id(c) = $nodeId
            set c.operationBy = cMap.operationBy, c.endTime = cMap.endTime,
            c.status = cMap.status, c.during = cMap.during,
            b.status = flowStatus
            return c',
            {i:i, cMap:$cMap, flowStatus:$flowStatus, businessKey:$businessKey, nodeId:$nodeId}
        ) yield value
        
        //初始化下一节点
        with i, value.c as c where c is not null
        call apoc.do.when (
            $nMap is null,
            'return null as n',
            'create (c)-[nr:NEXT]->(n:InstanceNode)
            set n+= nMap, nr.condition = condition
            return n',
            {c:c, nMap:$nMap, condition:$condition}
        ) yield value
        
        with value.n as n
        return apoc.convert.toJson(apoc.map.merge(properties(n),{id:id(n)}))
    """)
    String updateFlowInstance(String processName, Integer version, Long nodeId,
                            String businessKey, Integer condition, Integer flowStatus,
                            Map<String, Object> cMap, Map<String, Object> nMap);

}
