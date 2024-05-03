package com.nf.neoflow.repository;

import com.nf.neoflow.dto.process.ActiveVersionHistoryDto;
import com.nf.neoflow.dto.process.ProcessActiveStatusDto;
import com.nf.neoflow.models.Process;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

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
     * @param offset 分页偏移量
     * @param pageSize 分页大小
     * @param pageable 分页对象
     * @return 流程列表
     */
    @Query(value = """
        match (p:Process)
        where ($name is null or $name = '' or p.name contains $name)
        and ($createBy is null or $createBy = '' or p.createBy = $createBy)
        with p, p.createTime as createTime
        :#{orderBy(#pageable)}
        skip $offset limit $pageSize
        return p.name as name, p.activeVersion as activeVersion,
        createTime, p.updateTime as updateTime
    """,
    countQuery = """
        match (p:Process)
        where ($name is null or $name = '' or p.name contains $name)
        and ($createBy is null or $createBy = '' or p.createBy = $createBy)
        return count(p)
    """)
    Page<Process> queryProcessList(String name, String createBy, Long offset, Integer pageSize, Pageable pageable);

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
        case when active then '流程已激活' else '流程已关闭，将不能新增实例，进行中的流程不受影响' end as remark
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
}
