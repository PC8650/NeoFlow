package org.nf.neoflow.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nf.neoflow.component.BaseUserChoose;
import org.nf.neoflow.component.NeoCacheManager;
import org.nf.neoflow.component.NeoLockManager;
import org.nf.neoflow.dto.process.*;
import org.nf.neoflow.dto.user.UserBaseInfo;
import org.nf.neoflow.enums.CacheEnums;
import org.nf.neoflow.enums.LockEnums;
import org.nf.neoflow.exception.NeoProcessException;
import org.nf.neoflow.models.Process;
import org.nf.neoflow.repository.ProcessRepository;
import org.nf.neoflow.utils.JacksonUtils;
import org.nf.neoflow.utils.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程服务
 * @author PC8650
 */
@Slf4j
@Service
@AllArgsConstructor
public class ProcessService {

    private final ProcessRepository processRepository;
    private final BaseUserChoose userChoose;
    private final NeoLockManager lockManager;
    private final NeoCacheManager cacheManager;

    /**
     * 创建流程
     * @param form 创建流程表单
     * @return Process
     */
    public Process create(ProcessCreateForm form) {
        UserBaseInfo userBaseInfo = userChoose.user(form.getCreateBy());
        if (userBaseInfo != null) {
            form.setCreateBy(userBaseInfo.getId());
        }

        if (processRepository.existsProcessByName(form.getName())) {
            throw NeoProcessException.duplicateName(form.getName());
        }
        return processRepository.save(new Process(form.getName(), form.getCreateBy()));
    }

    /**
     * 查询流程列表
     * @param form 查询流程表单
     * @return Page<Process>
     */
    public Page<Process> processList(ProcessQueryForm form) {
        Pageable pageable = PageUtils.initPageable(form.getPageNumber()-1, form.getPageSize(), "createTime", form.getDesc());
        return processRepository.queryProcessList(form.getName(), form.getCreateBy(), pageable);
    }

    /**
     * 变更流程启用状态
     * @param form 请求表单
     * @return ProcessActiveStatusDto 流程启用状态
     */
    public ProcessActiveStatusDto changeActive(ProcessChangeActiveForm form) {
        log.info("变更流程启用状态{} -->  {}", form.getName(), form.getActive());
        boolean getLock = false;
        LockEnums lockEnum = LockEnums.PROCESS_STATUS;
        try {
            getLock = lockManager.getLock(form.getName(), lockEnum);
            UserBaseInfo userBaseInfo = userChoose.user(form.getUpdateBy());
            if (userBaseInfo != null) {
                form.setUpdateBy(userBaseInfo.getId());
            }
            return processRepository.changActive(form.getName(), form.getActive(), form.getUpdateBy(), LocalDateTime.now());
        } finally {
            lockManager.releaseLock(form.getName(), getLock, lockEnum);
        }

    }

    /**
     * 变更流程启用版本
     * @param form 请求表单
     */
    public void changeActiveVersion(ProcessChangeVersionForm form) {
        log.info("变更流程启用版本{} -->  {}", form.getName(), form.getActiveVersion());
        boolean getLock = false;
        LockEnums lockEnum = LockEnums.PROCESS_STATUS;
        try {
            getLock = lockManager.getLock(form.getName(), lockEnum);
            UserBaseInfo userBaseInfo = userChoose.user(form.getUpdateBy(), form.getUpdateByName());
            if (userBaseInfo != null) {
                form.setUpdateBy(userBaseInfo.getId());
                form.setUpdateByName(userBaseInfo.getName());
            }
            LocalDateTime time = LocalDateTime.now();
            ActiveVersionHistoryDto dto = new ActiveVersionHistoryDto(
                    form.getActiveVersion(), form.getUpdateBy(), form.getUpdateByName(), time
            );
            String history = JacksonUtils.toJson(dto);
            int i = processRepository.changeActiveVersion(form.getName(), form.getActiveVersion(), form.getUpdateBy(), form.getUpdateByName(), time, history);
            log.info("已变更{}启用版本", i);
            cacheManager.deleteCache(CacheEnums.P_A_V_H.getType(), form.getName());
        } finally {
            lockManager.releaseLock(form.getName(), getLock, lockEnum);
        }
    }

    /**
     * 查询流程启用历史
     * @param name 流程名称
     * @return List<ActiveVersionHistoryDto>
     */
    public List<ActiveVersionHistoryDto> activeVersionHistory(String name) {
        NeoCacheManager.CacheValue<List> value = cacheManager.getCache(CacheEnums.P_A_V_H.getType(), name, List.class);
        if (value.filter() || value.value() != null) {
            return value.value();
        }

        List<ActiveVersionHistoryDto> activeVersionHistoryDtos = processRepository.activeVersionHistory(name);

        cacheManager.setCache(CacheEnums.P_A_V_H.getType(), name, activeVersionHistoryDtos);
        return activeVersionHistoryDtos;
    }

    /**
     * 流程统计查询
     * @param form 表单
     * @return List<ProcessQueryStatisticsDto>
     */
    public List<ProcessQueryStatisticsDto> queryProcessForStatistics(ProcessQueryStatisticsForm form) {
        return processRepository.queryProcessForStatistics(form.getName(), form.getVersion(),
                form.getBeginStart(), form.getBeginEnd(), form.getEndStart(), form.getEndEnd(),
                form.getPending(), form.getComplete(), form.getRejected(), form.getTerminated(), form.getTotal());
    }

    /**
     * 查询所有流程名称
     * @return 流程名称
     */
    public List<String> queryAllProcessName() {
        String cacheType = CacheEnums.A_P_N.getType();
        String key = "all";
        List<String> names;
        NeoCacheManager.CacheValue<List> cache = cacheManager.getCache(cacheType, key, List.class);
        if (!cache.filter() && cache.value() != null) {
            names = cache.value();
        }else {
            names = processRepository.queryAllProcessName();
            cacheManager.setCache(cacheType, key, names);
        }
        return names;
    }

}
