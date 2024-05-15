package com.nf.neoflow.repository;

import com.nf.neoflow.dto.query.OperationHistoryDto;
import com.nf.neoflow.dto.query.OperatorOfPendingDto;
import com.nf.neoflow.dto.query.QueryForOperatorDto;
import com.nf.neoflow.dto.query.QueryOfNodeIdentityDto;
import com.nf.neoflow.models.Instance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;

/**
 * 流程实例的泛用查询
 * @author PC8650
 */
public interface QueryRepository extends Neo4jRepository<Instance, Long> {

    /**
     * 询当前用户在各流程的待办数量
     * @param name 流程名称
     * @param version 版本
     * @param query lucene查询语句
     * @return List<OperatorOfPendingDto>
     */
    @Query("""
        call db.index.fulltext.queryRelationships('BUSINESS_fullText_oc',$2) yield relationship
        with relationship as b where b.status = 1
        match (v:Version)-[:INSTANCE]->(:Instance)-[b]->(:InstanceNode) where ($1 is null or v.version = $1)
        optional match (p)-[:VERSION]->(v) where ($0 is null or $0 = '' or p.name contains $0)
        with p, v, count(b) as vc where p is not null
        return p.name as name, sum(vc) as count, collect({version: v.version, count: vc}) as version
    """)
    List<OperatorOfPendingDto> queryForOperatorOfPending(String name, Integer version, String query);

    /**
     * 发起列表
     * @param name 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @param query 用户json
     * @param instanceStatus 流程实例状态
     * @param pageable 分页对象
     * @return Page<QueryForOperatorDto>
     */
    @Query(value = """
        match (i:Instance)-[b:BUSINESS]->(n:InstanceNode{operationBy: $3})
        where n is not null and ($2 is null or b.key = $2) and ($4 is null or b.status = $4)
        optional match (v:Version)-[:INSTANCE]->(i) where ($1 is null or v.version = $1)
        optional match (p)-[:VERSION]->(v) where ($0 is null or $0 = '' or p.name contains $0)
        with p, v, b where p is not null
        return p.name as name, v.version as version,
        b.beginTime as initiateTime, b.endTime as updateTime, b.status as status,
        b.key as businessKey, b.num as num, b.currentNodeId as nodeId
        :#{orderBy(#pageable)}
        skip :#{#pageable.offset} limit :#{#pageable.pageSize}
    """,
    countQuery = """
        match (i:Instance)-[b:BUSINESS]->(n:InstanceNode{operationBy: $3})
        where n is not null and ($2 is null or b.key = $2) and ($4 is null or b.status = $4)
        optional match (v:Version)-[:INSTANCE]->(i) where ($1 is null or v.version = $1)
        optional match (p)-[:VERSION]->(v) where ($0 is null or $0 = '' or p.name contains $0)
        with n where p is not null
        return count(n)
    """)
    Page<QueryForOperatorDto> queryForOperatorInitiate(String name, Integer version, String businessKey, String query, Integer instanceStatus, Pageable pageable);

    /**
     * 待办列表
     * @param name 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @param query lucene查询语句
     * @param pageable 分页对象
     * @return Page<QueryForOperatorDto>
     */
    @Query(value = """
        call db.index.fulltext.queryRelationships('BUSINESS_fullText_oc',$3) yield relationship
        with relationship as b where ($2 is null or b.key = $2) and b.status = 1
        match (v:Version)-[:INSTANCE]->(:Instance)-[b]->(:InstanceNode) where ($1 is null or v.version = $1)
        optional match (p)-[:VERSION]->(v) where ($0 is null or $0 = '' or p.name contains $0)
        with p, v, b where p is not null
        return p.name as name, v.version as version,
        b.beginTime as initiateTime, b.endTime as updateTime,
        b.key as businessKey, b.num as num, b.currentNodeId as nodeId
        :#{orderBy(#pageable)}
        skip :#{#pageable.offset} limit :#{#pageable.pageSize}
    """,
    countQuery = """
        call db.index.fulltext.queryRelationships('BUSINESS_fullText_oc',$3) yield relationship
        with relationship as b where ($2 is null or b.key = $2) and b.status = 1
        match (v:Version)-[:INSTANCE]->(:Instance)-[b]->(:InstanceNode) where ($1 is null or v.version = $1)
        optional match (p)-[:VERSION]->(v) where ($0 is null or $0 = '' or p.name contains $0)
        with b where p is not null
        return count(b)
    """)
    Page<QueryForOperatorDto> queryForOperatorPending(String name, Integer version, String businessKey, String query, Pageable pageable);

    /**
     * 已办列表
     * @param name 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @param query 用户json
     * @param nodeStatus 已办节点状态
     * @param pageable 分页对象
     * @return Page<QueryForOperatorDto>
     */
    @Query(value = """
        match path = (i:Instance)-[b:BUSINESS]->(:InstanceNode)-[:NEXT*]->(n:InstanceNode{operationBy: $3})
        where n is not null and ($2 is null or b.key = $2) and ($4 is null or n.status = $4)
        match (v:Version)-[:INSTANCE]->(i) where ($1 is null or v.version = $1)
        optional match (p)-[:VERSION]->(v) where ($0 is null or $0 = '' or p.name contains $0)
        with p, v, b, size(nodes(path))-1 as nodeNum, n where p is not null
        with p, v, b, max(n.endTime) as doneTime,
        collect({num: nodeNum, nodeId: id(n), nodeName: n.name, status: n.status, doneTime: n.endTime}) as doneNodes
        :#{orderBy(#pageable)}
        skip :#{#pageable.offset} limit :#{#pageable.pageSize}
        return doneNodes,
        p.name as name, v.version as version,
        b.beginTime as initiateTime, b.endTime as updateTime,
        b.key as businessKey, b.num as num, b.currentNodeId as nodeId
    """,
    countQuery = """
        match path = (i:Instance)-[b:BUSINESS]->(:InstanceNode)-[:NEXT*]->(n:InstanceNode{operationBy: $3})
        where n is not null and ($2 is null or b.key = $2) and ($4 is null or n.status = $4)
        match (v:Version)-[:INSTANCE]->(i) where ($1 is null or v.version = $1)
        optional match (p)-[:VERSION]->(v) where ($0 is null or $0 = '' or p.name contains $0)
        with b where p is not null
        return count(distinct b)
    """)
    Page<QueryForOperatorDto> queryForOperatorDone(String name, Integer version, String businessKey, String query, Integer nodeStatus, Pageable pageable);

    /**
     * 通过节点 名称/身份 查询节点状态
     * @param name 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @param nodeName 节点名称
     * @param nodeIdentity 节点身份
     * @param query lucene查询语句
     * @param pageable 分页对象
     * @return Page<QueryOfNodeIdentityDto>
     */
    @Query(value = """
        call db.index.fulltext.queryNodes('InstanceNode_fullText_oc',$query) yield node
        with node as n where n.status =1
        and ($nodeName is null or n.name = $nodeName) and ($nodeIdentity is null or n.identity = $nodeIdentity)
        match path = (i:Instance)-[b:BUSINESS]->(:InstanceNode)-[:NEXT*]->(n) where ($businessKey is null or b.key = $businessKey)
        optional match (v:Version)-[:INSTANCE]->(i) where ($version is null or v.version = $version)
        optional match (p:Process)-[:VERSION]->(v)where ($name is null or p.name = $name)
        with p, v, b, b.endTime as endTime, size(nodes(path))-1 as num, id(n) as nodeId, n.status as status where p is not null
        return p.name as name, v.version as version, b.key as businessKey, num, nodeId, status
       :#{orderBy(#pageable)}
        skip :#{#pageable.offset} limit :#{#pageable.pageSize}
    """,
    countQuery = """
        call db.index.fulltext.queryNodes('InstanceNode_fullText_oc',$query) yield node
        with node as n where n.status =1
        and ($nodeName is null or n.name = $nodeName) and ($nodeIdentity is null or n.identity = $nodeIdentity)
        match path = (i:Instance)-[b:BUSINESS]->(:InstanceNode)-[:NEXT*]->(n) where ($businessKey is null or b.key = $businessKey)
        optional match (v:Version)-[:INSTANCE]->(i) where ($version is null or v.version = $version)
        optional match (p:Process)-[:VERSION]->(v)where ($name is null or p.name = $name)
        with n where p is not null
        return count(n)
    """)
    Page<QueryOfNodeIdentityDto> queryOfNodeIdentityPending(String name, Integer version, String businessKey,
                                                     String nodeName, String nodeIdentity,
                                                     String query, Pageable pageable);

    /**
     * 通过节点 名称/身份 查询节点状态
     * @param name 流程名称
     * @param version 版本
     * @param businessKey 业务key
     * @param nodeName 节点名称
     * @param nodeIdentity 节点身份
     * @param nodeStatus 节点状态
     * @param query lucene查询语句
     * @param pageable 分页对象
     * @return Page<QueryOfNodeIdentityDto>
     */
    @Query(value = """
        call db.index.fulltext.queryNodes('InstanceNode_fullText_oc',$query) yield node
        with node as n where n.status > 1 and ($nodeStatus is null or n.status = $nodeStatus)
        and ($nodeName is null or n.name = $nodeName) and ($nodeIdentity is null or n.identity = $nodeIdentity)
        match path = (i:Instance)-[b:BUSINESS]->(:InstanceNode)-[:NEXT*]->(n) where ($businessKey is null or b.key = $businessKey)
        optional match (v:Version)-[:INSTANCE]->(i) where ($version is null or v.version = $version)
        optional match (p:Process)-[:VERSION]->(v)where ($name is null or p.name = $name)
        with p, v, b, n.endTime as endTime, size(nodes(path))-1 as num, id(n) as nodeId, n.status as status where p is not null
        return p.name as name, v.version as version, b.key as businessKey, num, nodeId, status
       :#{orderBy(#pageable)}
        skip :#{#pageable.offset} limit :#{#pageable.pageSize}
    """,
    countQuery = """
        call db.index.fulltext.queryNodes('InstanceNode_fullText_oc',$query) yield node
        with node as n where n.status > 1 and ($nodeStatus is null or n.status = $nodeStatus)
        and ($nodeName is null or n.name = $nodeName) and ($nodeIdentity is null or n.identity = $nodeIdentity)
        match path = (i:Instance)-[b:BUSINESS]->(:InstanceNode)-[:NEXT*]->(n) where ($businessKey is null or b.key = $businessKey)
        optional match (v:Version)-[:INSTANCE]->(i) where ($version is null or v.version = $version)
        optional match (p:Process)-[:VERSION]->(v)where ($name is null or p.name = $name)
        with n where p is not null
        return count(n)
    """)
    Page<QueryOfNodeIdentityDto> queryOfNodeIdentityDone(String name, Integer version, String businessKey,
                                                            String nodeName, String nodeIdentity, Integer nodeStatus,
                                                            String query, Pageable pageable);

    /**
     * 查询流程实例操作历史
     * @param businessKey 业务key
     * @param num 截至到 第 num 个节点
     * @return List<OperationHistoryDto>
     */
    @Query("""
        match (:Instance)-[b:BUSINESS{key:$0}]->(i:InstanceNode)
        with i, case when $1 is null then b.num else $1 end as length
        call apoc.path.expandConfig(i, {
            relationshipFilter: "NEXT>",
            minLevel: length-1,
            maxLevel: length-1,
            limit: 1
        }) yield path
        with nodes(path) as nodes
        unwind nodes as n
        with n
        return n.name as nodeName,
        n.status as operationResult,
        apoc.convert.fromJsonList(n.operationCandidate) as candidate,
        apoc.convert.fromJsonMap(n.operationBy) as operator,
        n.operationRemark as operationRemark,
        n.beginTime as beginTime, n.endTime as endTime,
        n.during as during
    """)
    List<OperationHistoryDto> queryInstanceOperationHistory(String businessKey, Integer num);

}
