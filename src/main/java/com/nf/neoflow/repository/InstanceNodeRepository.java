package com.nf.neoflow.repository;

import com.nf.neoflow.dto.execute.AutoNodeDto;
import com.nf.neoflow.dto.execute.NodeQueryDto;
import com.nf.neoflow.models.InstanceNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
     * @param version 版本
     * @param businessKey 业务key
     * @param nodeId 节点id
     * @return NodeQueryDto<InstanceNode>
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:INSTANCE]->(:Instance)-[:BUSINESS{key:$2,status:1}]->(f:InstanceNode)
        optional match path = (f)-[:NEXT*0..]->(b:InstanceNode)-[:NEXT]->(c:InstanceNode) where id(c) = $3
        return v.version as version,
        v.terminatedMethod as terminatedMethod,
        case when b is null then null
        when b.status = 1 then false
        else true end as before,
        case when c is null then null
        else apoc.convert.toJson(apoc.map.merge(properties(c),{id:id(c)})) end as nodeJson
    """)
    NodeQueryDto<InstanceNode> queryCurrentInstanceNode(String processName, Integer version, String businessKey, Long nodeId);

    /**
     * 查询当前实例节点
     * @param processName 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @param nodeId 节点id
     * @param num 当前节点位置
     * @return NodeQueryDto<InstanceNode>
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:INSTANCE]->(i:Instance)
        optional match (i)-[:BUSINESS{key:$2,status:1}]->(f:InstanceNode) where f is not null
        with v, f, $4-2 as length
        call apoc.path.expandConfig(f, {
            relationshipFilter: "NEXT>",
            minLevel: length,
            maxLevel: length,
            limit: 1
        }) yield path
        with v, last(nodes(path)) as b
        optional match (b)-[:NEXT]->(c:InstanceNode) where id(c) = $3
        return v.version as version,
        v.terminatedMethod as terminatedMethod,
        case when b is null then null
        when b.status = 1 then false
        else true end as before,
        case when c is null then null
        else apoc.convert.toJson(apoc.map.merge(properties(c),{id:id(c)})) end as nodeJson
    """)
    NodeQueryDto<InstanceNode> queryCurrentInstanceNodeTooLong(String processName, Integer version, String businessKey, Long nodeId, Integer num);

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
            set b.status = flowStatus, b.beginTime = localDateTime(cMap.beginTime), b.num = 1,
            c += cMap, c.autoTime = date(cMap.autoTime),
            c.beginTime = localDateTime(cMap.beginTime), c.endTime = localDateTime(cMap.endTime)
            return c, b',
            'optional match (i)-[b:BUSINESS{key:$businessKey,status:1}]->(f:InstanceNode) where f is not null
            optional match (f)-[:NEXT*0..]->(c:InstanceNode) where id(c) = $nodeId
            set c.operationBy = cMap.operationBy, c.endTime = localDateTime(cMap.endTime),
            c.status = cMap.status, c.operationRemark = cMap.operationRemark,
            c.during = cMap.during, c.processDuring = cMap.processDuring,
            b.status = flowStatus, b.endTime = localDateTime(cMap.endTime), b.during = cMap.processDuring,
            b.operationBy = case when cMap.autoTime is null then cMap.operationBy else b.operationBy end
            return c, b',
            {i:i, cMap:$cMap, flowStatus:$flowStatus, businessKey:$businessKey, nodeId:$nodeId}
        ) yield value
        
        //初始化下一节点
        with i, value.c as c, value.b as b where c is not null
        call apoc.do.when (
            $nMap is null,
            'return null as n',
            'create (c)-[nr:NEXT]->(n:InstanceNode)
            set nr.condition = condition,
            n+= nMap, n.autoTime = date(nMap.autoTime),
            n.beginTime = localDateTime(nMap.beginTime),
            b.cycle = case when n.location = 1 then coalesce(b.cycle, 0) + 1 else b.cycle end,
            b.operationCandidate = nMap.operationCandidate,
            b.num = b.num + 1, b.currentNodeId = id(n)
            return n',
            {c:c, b:b, nMap:$nMap, condition:$condition}
        ) yield value
        
        with value.n as n
        return id(n)
    """)
    Long updateFlowInstance(String processName, Integer version, Long nodeId,
                            String businessKey, Integer condition, Integer flowStatus,
                            Map<String, Object> cMap, Map<String, Object> nMap);

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
        with p, i, $num-1 as l
        optional match (i)-[b:BUSINESS{key:$businessKey,status:1}]->(f:InstanceNode) where f is not null
        call apoc.path.expandConfig(f, {
            relationshipFilter: "NEXT>",
            minLevel: l,
            maxLevel: l,
            limit: 1
        }) yield path
        with i, b, last(nodes(path)) as c, $cMap as cMap
        where id(c) = $nodeId
        set c.operationBy = cMap.operationBy, c.endTime = localDateTime(cMap.endTime),
        c.status = cMap.status, c.operationRemark = cMap.operationRemark,
        c.during = cMap.during, c.processDuring = cMap.processDuring,
        b.status = $flowStatus, b.endTime = localDateTime(cMap.endTime), b.during = cMap.processDuring,
        b.operationBy = case when cMap.autoTime is null then cMap.operationBy else b.operationBy end

        //初始化下一节点
        with i, c, b where c is not null
        call apoc.do.when (
            $nMap is null,
            'return null as n',
            'create (c)-[nr:NEXT]->(n:InstanceNode)
            set nr.condition = condition,
            n+= nMap, n.autoTime = date(nMap.autoTime),
            n.beginTime = localDateTime(nMap.beginTime),
            b.cycle = case when n.location = 1 then coalesce(b.cycle, 0) + 1 else b.cycle end,
            b.operationCandidate = nMap.operationCandidate,
            b.num = b.num + 1, b.currentNodeId = id(n)
            return n',
            {c:c, b:b, nMap:$nMap, condition:$condition}
        ) yield value
        
        with value.n as n
        return id(n)
    """)
    Long updateFlowInstanceTooLong(String processName, Integer version, Long nodeId,
                                   Integer num, String businessKey, Integer condition, Integer flowStatus,
                            Map<String, Object> cMap, Map<String, Object> nMap);

    /**
     * 退回发起人循环次数是否达上限
     * @param processName 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @return Boolean
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:INSTANCE]->(:Instance)-[b:BUSINESS{key:$2}]->(f:InstanceNode) where b is not null
        return coalesce(b.cycle, 0) < v.cycle
    """)
    Boolean canCycle(String processName, Integer version, String businessKey);

    /**
     * 查询实例发起节点
     * @param processName 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @return InstanceNode
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:INSTANCE]->(i:Instance)
        optional match (i)-[b:BUSINESS{key:$2}]->(f:InstanceNode) 
        with f where f is not null
        return f.modelNodeUid as modelNodeUid, f.name as name, f.identity as identity,
        1 as status, f.operationType as operationType, f.operationMethod as operationMethod,
        f.onlyPassExecute as onlyPassExecute, f.location as location,
        '['+f.operationBy+']' as operationCandidate
    """)
    InstanceNode queryInstanceInitiateNode(String processName, Integer version, String businessKey);

    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})-[:INSTANCE]->(i:Instance)
        optional match (i)-[b:BUSINESS{key:$2}]->(f:InstanceNode)
        with f where f is not null
        return f.beginTime
    """)
    LocalDateTime queryInstanceBeginTime(String processName, Integer version, String businessKey);

    /**
     * 扫描当天的自动节点
     * @param date 日期
     * @return List<AutoNodeDto>
     */
    @Query("""
        match (p:Process)-[:VERSION]->(v:Version)-[:INSTANCE]->(i:Instance),
        path = (i)-[b:BUSINESS{status:1}]->(:InstanceNode)-[*0..]->(n:InstanceNode{status:1})
        where n.autoTime = $0
        return p.name as processName, v.version as version, b.key as businessKey,
        size(nodes(path))-1 as num, id(n) as nodeId, n.beginTime as beginTime,
        n.modelNodeUid as modelNodeUid, n.operationMethod as operationMethod,
        n.location as location, n.defaultPassCondition as defaultPassCondition
    """)
    List<AutoNodeDto> queryAutoNodeToDay(LocalDate date);

    /**
     * 扫描当天及以前的自动节点
     * @param date 日期
     * @return List<AutoNodeDto>
     */
    @Query("""
        match (p:Process)-[:VERSION]->(v:Version)-[:INSTANCE]->(i:Instance),
        path = (i)-[b:BUSINESS{status:1}]->(:InstanceNode)-[*0..]->(n:InstanceNode{status:1})
        where n.autoTime = $0
        return p.name as processName, v.version as version, b.key as businessKey,
        size(nodes(path))-1 as num, id(n) as nodeId, n.beginTime as beginTime,
        n.modelNodeUid as modelNodeUid, n.operationMethod as operationMethod,
        n.location as location, n.defaultPassCondition as defaultPassCondition
    """)
    List<AutoNodeDto> queryAutoNodeToDayAndBefore(LocalDate date);
}
