package com.nf.neoflow.repository;

import com.nf.neoflow.dto.version.IterateTreeNode;
import com.nf.neoflow.dto.version.IterateTreeNodeGraphic;
import com.nf.neoflow.dto.version.VersionListDto;
import com.nf.neoflow.dto.version.VersionModelViewDto;
import com.nf.neoflow.models.Version;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 版本仓库映射
 * @author PC8650
 */
public interface VersionRepository extends Neo4jRepository<Version,Long> {

    @Query(value = """
        match (:Process{name:$0})-[:VERSION]->(v:Version)
        optional match (v)<-[:ITERATE]-(i:Version)
        with v, v.createTime as createTime, i.version as iterateFrom
        :#{orderBy(#pageable)}
        skip $offset limit $pageSize
        return $0 as processName, v.version as version, iterateFrom, v.cycle as cycle,
        v.createBy as createBy, v.createByName as createByName, createTime
    """,
    countQuery = """
        match (:Process{name:$0})-[:VERSION]->(v:Version)
        return count(v)
    """)
    Page<VersionListDto> queryVersionList(String processName, Long offset, Integer pageSize, Pageable pageable);

    /**
     * 查询流程版本视图
     * @param processName 版本名称
     * @param version 版本号
     * @return VersionModelViewDto
     */
    @Query("""
        match (p:Process{name:$0})
        match (p)-[:VERSION]->(v:Version{version:$1})-[:MODEL]->(s:ModelNode) where s is not null
        match path = (s)-[n:NEXT*1..]->(e:ModelNode) where e.location in [3,4]
        optional match (i:Version)-[:ITERATE]->(v)
        with p, i, v, path
        unwind nodes(path) as n
        with p, i , v, apoc.convert.toMap(n) as nm, path
        unwind relationships(path) as rels
        unwind rels as rel
        with p, i, v, nm, {operationCandidateInfo:apoc.convert.fromJsonList(nm.operationCandidate)} as oc, rel
        return p.name as processName, i.version as iterateFrom, v.version as version, v.cycle as cycle, v.terminatedMethod as terminatedMethod,
        collect(distinct apoc.map.merge(apoc.map.removeKeys(nm, ['operationCandidate']),oc)) as versionNodes,
        collect(distinct {startNode: startNode(rel).nodeUid, endNode: endNode(rel).nodeUid, condition: rel.condition}) as versionEdges
    """)
    VersionModelViewDto queryVersionView(String processName, Integer version);

    /**
     * 查询流程迭代树 非嵌套，只包含直系迭代
     * @param processName 流程名称
     * @return IterateTreeNode
     */
    @Query("""
        match (:Process{name:$0})-[:VERSION]->(v:Version)
        optional match (v)-[:ITERATE]->(i:Version)
        with v, i order by v.version, i.version
        with v, collect(i) as iterate
        return v.version as version, v.cycle as cycle, v.terminatedMethod as terminatedMethod,
        v.createBy as createBy, v.createByName as createByName, v.createTime as createTime,
        size(iterate) as iterateCount, [x in iterate | x.version] as iterateVersion,
        [x in iterate | {
                version: x.version,
                cycle: x.cycle,
                terminatedMethod: x.terminatedMethod,
                createBy: x.createBy,
                createByName: x.createByName,
                createTime: x.createTime
        }] as iterate
    """)
    List<IterateTreeNode> queryVersionIterateTree(String processName);

    /**
     * 查询流程迭代树 嵌套，包含非直系迭代
     * @param processName 流程名称
     * @return IterateTreeNode
     */
    @Query("""
        match (:Process{name:$0})-[:VERSION]->(v:Version)
        optional match (v)-[:ITERATE]->(i:Version)
        optional match (:Version)-[ir:ITERATE]->(v)
        with v, i, case when ir is null then true else false end as top
        order by v.version, i.version
        with v, collect(i) as iterate, top
        return v.version as version, v.cycle as cycle, v.terminatedMethod as terminatedMethod, top,
        v.createBy as createBy, v.createByName as createByName, v.createTime as createTime,
        size(iterate) as iterateCount, [x in iterate | x.version] as iterateVersion, [] as iterate
    """)
    List<IterateTreeNode> queryVersionIterateTreeNested(String processName);

    /**
     * 查询流程迭代树 图形，返回节点和迭代关系(不含坐标)
     * @param processName 流程名称
     * @return IterateTreeNodeGraphic
     */
    @Query("""
        match (:Process{name:$0})-[vr:VERSION]->(v:Version)
        optional match (v)-[:ITERATE]->(i:Version)
        with v, i,
        case when i is not null then
        {
            startNode: toString(v.version),
            endNode: toString(i.version)
        } end as edge
        order by v.version, i.version
        with v, edge, count(i) as iterateCount, collect(i.version) as ic
        with edge,
        {
            version: v.version,
            cycle: v.cycle,
            terminatedMethod: v.terminatedMethod,
            createBy: v.createBy,
            createByName: v.createByName,
            createTime: v.createTime,
            iterateCount: iterateCount,
            iterateVersion: [x in ic | x]
        } as vm
        return collect(vm) as nodes, collect(edge) as edges
    """)
    IterateTreeNodeGraphic queryVersionIterateTreeGraphic(String processName);

    /**
     * 判断流程和迭代版本是否存在
     * @param processName 流程名称
     * @param IterateFrom 迭代版本
     * @return 提示
     */
    @Query("""
        match (p:Process{name:$0})
        optional match (p)-[:VERSION]->(v:Version{version:$1})
        return case when p is null then '流程['+$0+']不存在在'
        when $1 is not null and v is null then '迭代版本['+$0+'-'+$1+']不存在'
        else null end
    """)
    String existsProcessAndIterateFrom(String processName, Integer IterateFrom);

    /**
     * 创建流程模型版本
     * @param nodes 节点
     * @param edges 关系
     * @param processName 流程名称
     * @param cycle 退回次数
     * @param createBy 创建人标识
     * @param createByName 创建人名称
     * @param createTime 创建人名称
     * @return 版本号
     */
    @Query("""
    //创建新的版本节点
    match (p:Process{name:$processName})
    optional match (p)-[:VERSION]->(ver:Version)
    with p, coalesce(max(ver.version), 0)+1 as nextVer, $IterateFrom as IterateFrom
    call apoc.do.when (
        IterateFrom is null,
        'create (p)-[:VERSION]->(newVer:Version) return newVer',
        'match (p)-[:VERSION]->(i:Version{version:$IterateFrom})
         where i is not null
         create (i)-[:ITERATE]->(newVer:Version)
         create (p)-[:VERSION]->(newVer)
         return newVer',
        {p:p, IterateFrom:IterateFrom, nextVer:nextVer}
    ) yield value
    with value.newVer as newVer, nextVer, p
    set newVer.version = nextVer, newVer.cycle = $cycle, newVer.terminatedMethod = $terminatedMethod,
    newVer.createTime = $createTime, newVer.createBy = $createBy, newVer.createByName = $createByName
    //创建实例节点
    with newVer, nextVer
    create (newVer)-[:INSTANCE]->(:Instance{name:'instance'})
    //创建模型节点
    with newVer, nextVer
    unwind $nodes as node
    create (n:ModelNode)
    set n += node
    with n, newVer, nextVer
    call apoc.do.when(
        n.location = 1,
        'create (newVer)-[:MODEL]->(n) return n',
        'return n',
        {newVer:newVer, n:n}
    ) yield value as newNode
    with collect(n) as newNodes, nextVer
    //创建NEXT条件关系
    unwind $edges as edge
    with newNodes, nextVer, edge,
    [x in newNodes where x.nodeUid = edge.startNode][0] as startNode,
    [x in newNodes where x.nodeUid = edge.endNode][0] as endNode
    merge (startNode)-[c:NEXT]->(endNode)
    on create set c.condition = edge.condition,
    c.startLocation = edge.startLocation, c.endLocation = edge.endLocation
    return distinct nextVer
    """)
    Integer createVersion(Set<Map<String,Object>> nodes, Set<Map<String,Object>> edges,
                          String processName, Integer IterateFrom, Integer cycle, String terminatedMethod,
                          String createBy, String createByName, LocalDateTime createTime);
}
