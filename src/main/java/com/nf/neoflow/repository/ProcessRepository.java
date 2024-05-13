package com.nf.neoflow.repository;

import com.nf.neoflow.dto.process.ActiveVersionHistoryDto;
import com.nf.neoflow.dto.process.ProcessActiveStatusDto;
import com.nf.neoflow.dto.process.ProcessQueryStatisticsDto;
import com.nf.neoflow.models.Process;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程仓库映射
 * @author PC8650
 */
public interface ProcessRepository extends Neo4jRepository<Process,Long> {

    /**
     * 根据流程名称判断流程是否存在
     * @param name 流程名称
     * @return 存在返回true，否则返回false
     */
    Boolean existsProcessByName(String name);

    /**
     * 查询流程列表
     * @param name 流程名称
     * @param createBy 创建人唯一标识
     * @param pageable 分页对象
     * @return 流程列表
     */
    @Query(value = """
        match (p:Process)
        where ($name is null or $name = '' or p.name contains $name)
        and ($createBy is null or $createBy = '' or p.createBy = $createBy)
        with p, p.createTime as createTime
        :#{orderBy(#pageable)}
        skip :#{#pageable.offset} limit :#{#pageable.pageSize}
        return p.name as name, p.activeVersion as activeVersion,
        createTime, p.updateTime as updateTime
    """,
    countQuery = """
        match (p:Process)
        where ($name is null or $name = '' or p.name contains $name)
        and ($createBy is null or $createBy = '' or p.createBy = $createBy)
        return count(p)
    """)
    Page<Process> queryProcessList(String name, String createBy, Pageable pageable);

    /**
     * 变更流程启用状态
     * @param name 流程名称
     * @param active 启用状态
     * @param updateBy 更新人标识
     * @param updateTime 更新时间
     * @return ProcessActiveStatusDto 流程启用状态
     */
    @Query("""
        match (p:Process{name: $name})
        set p.active = $active, p.updateBy = $updateBy, p.updateTime = $updateTime
        with p.name as name, p.active as active, p.activeVersion as activeVersion
        return name, active, activeVersion,
        case when active then '流程已激活' else '流程已关闭，不能再新增实例，进行中的流程不受影响' end as remark
    """)
    ProcessActiveStatusDto changActive(String name, Boolean active, String updateBy, LocalDateTime updateTime);

    /**
     * 更改流程激活版本
     * @param name 流程名称
     * @param activeVersion 版本号
     * @param updateBy 更新人标识
     * @param updateByName 更新人名称
     * @param updateTime 更新时间
     * @return 新建ACTIVE关系数量
     */
    @Query("""
        //判断当前版本与更改版本是否不同
        with $name as name, $activeVersion as activeVersion,
        $updateBy as updateBy, $updateByName as updateByName,
        $updateTime as updateTime, $history as history
        
        match (p:Process{name: name, active: true})-[:VERSION]->(v:Version{version: activeVersion})
        where coalesce(p.activeVersion,'') <> activeVersion
        
        //不同，删除旧关系，更新节点信息
        optional match (p)-[a:ACTIVE]->() delete a
        merge (p)-[r:ACTIVE]->(v)
        on create set r.createBy = updateBy, r.createTime = updateTime,
        p.activeVersion = activeVersion, p.updateBy = updateBy, p.updateTime = updateTime,
        p.activeHistory = case when p.activeHistory is null then [history]
        else p.activeHistory + [history] end
        
        return count(r)
    """)
    Integer changeActiveVersion(String name, Integer activeVersion, String updateBy, String updateByName, LocalDateTime updateTime, String history);

    /**
     * 查询流程激活历史
     * @param name 流程名称
     * @return 流程激活历史
     */
    @Query("""
        match (p:Process{name: $name})
        where p.activeHistory is not null and size(p.activeHistory) > 0
        unwind  p.activeHistory as history
        with apoc.convert.fromJsonMap(history) as result
        with result
        return result.version as version,
        result.id as activeId, result.name as activeName,
        localDateTime(replace(result.time, ' ', 'T')) as activeTime
    """)
    List<ActiveVersionHistoryDto> activeVersionHistory(String name);

    /**
     * 流程统计查询
     * @param name 流程名称
     * @param version 版本号
     * @param beginStart 流程开始时间起始
     * @param beginEnd 流程开始时间结束
     * @param endStart 流程结束时间起始
     * @param endEnd 流程结束时间结束
     * @param pending 最低流程进行数
     * @param complete 最低流程完成数
     * @param rejected 最低流程拒绝数
     * @param terminated 最低流程终止数
     * @param total 最低流程总数
     * @return 流程统计结果
     */
    @Query("""
        match (p:Process) where ($name is null or $name = '' or p.name contains $name)
        optional match (p)-[:VERSION]->(v:Version)-[:INSTANCE]->(i:Instance) where ($version is null or v.version = $version)
        optional match (i)-[b:BUSINESS]->(:InstanceNode)
        where (
            (($beginStart is null or date(b.beginTime) >= $beginStart) and ($beginEnd is null or date(b.beginTime) <= $beginEnd))
            and
            (($endStart is null or date(b.endTime) >= $endStart) and ($endEnd is null or date(b.endTime) <= $endEnd))
        )
        with p, v,
        {
            version: v.version,
            pending: sum(case when b.status = 1 then 1 else 0 end),
            complete: sum(case when b.status = 2 then 1 else 0 end),
            rejected: sum(case when b.status = 3 then 1 else 0 end),
            terminated: sum(case when b.status = 3 then 1 else 0 end),
            total: count(b)
        } as vi
        with p, collect(vi) as version,
        sum(vi.pending) as pending, sum(vi.complete) as complete, sum(vi.rejected) as rejected, sum(vi.terminated) as terminated, sum(vi.total) as total
        where (
            ($pending is null or pending >= $pending) and ($complete is null or complete >= $complete) and
            ($rejected is null or rejected >= $rejected) and ($terminated is null or terminated >= $terminated) and
            ($total is null or total >= $total)
        )
        return p.name as name, pending, complete, rejected, terminated, total, version
        order by p.createTime
    """)
    List<ProcessQueryStatisticsDto> queryProcessForStatistics(String name, Integer version,
                                                              LocalDate beginStart, LocalDate beginEnd,
                                                              LocalDate endStart, LocalDate endEnd,
                                                              Long pending, Long complete, Long rejected,
                                                              Long terminated, Long total);

    /**
     * 查询所有流程名称
     * @return List<String>
     */
    @Query("""
        match (p:Process) return p.name order by id(p)
    """)
    List<String> queryAllProcessName();

}
