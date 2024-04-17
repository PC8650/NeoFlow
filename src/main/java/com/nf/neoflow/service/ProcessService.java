package com.nf.neoflow.service;

import com.nf.neoflow.component.BaseUserChoose;
import com.nf.neoflow.component.LockManager;
import com.nf.neoflow.dto.process.*;
import com.nf.neoflow.dto.user.UserBaseInfo;
import com.nf.neoflow.enums.LockEnums;
import com.nf.neoflow.exception.NeoProcessException;
import com.nf.neoflow.models.Process;
import com.nf.neoflow.repository.ProcessRepository;
import com.nf.neoflow.utils.JacksonUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessService {

    private final ProcessRepository processRepository;
    private final BaseUserChoose userChoose;
    private final LockManager lockManager;

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
        Sort sort;
        if (form.getDesc()) {
            sort = Sort.by(Sort.Direction.DESC,"createTime");
        }else {
            sort = Sort.by(Sort.Direction.ASC,"createTime");
        }
        Pageable pageable = PageRequest.of(form.getPageNumber()-1, form.getPageSize(), sort);
        return processRepository.queryProcessList(form.getName(), form.getCreateBy(), pageable.getOffset(), pageable.getPageSize(), pageable);
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
     * @return 变更记录数
     */
    public Integer changeActiveVersion(ProcessChangeVersionForm form) {
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
            return processRepository.changeActiveVersion(form.getName(), form.getActiveVersion(), form.getUpdateBy(), form.getUpdateByName(), time, history);
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
        return processRepository.activeVersionHistory(name);
    }

}
