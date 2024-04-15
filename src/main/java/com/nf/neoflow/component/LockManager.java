package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.exception.NeoProcessException;
import com.nf.neoflow.interfaces.CustomizationLock;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 锁管理
 * @author PC8650
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockManager {

    private final NeoFlowConfig config;

    @Autowired
    @Lazy
    private CustomizationLock customizationLock;

    /**
     * 流程启用版本锁
     * 用于更改流程启用状态、更改流程启用版本
     */
    private ConcurrentHashMap.KeySetView<String, Boolean> ACTIVE_VERSION = null;

    /**
     * 流程模型创建锁
     * 用于创建流程模型
     */
    private ConcurrentHashMap.KeySetView<String, Boolean> VERSION_CREATE_LOCK = null;

    @PostConstruct
    public void lockInit() {
        if (!config.getCustomizationLock()) {
            VERSION_CREATE_LOCK = ConcurrentHashMap.newKeySet();
            ACTIVE_VERSION = ConcurrentHashMap.newKeySet();
        }
    }

    /**
     * 获取流程模型创建锁
     * @param processName 流程名称
     */
    public Boolean getActiveVersionLock(String processName) {
        boolean getLock;
        String thread = Thread.currentThread().getName();
        if (config.getCustomizationLock()) {
            getLock = customizationLock.addAndGetActiveVersionLock(processName);
        }else {
            getLock = ACTIVE_VERSION.add(processName);
        }
        log.info("获取流程启用版本锁：{}-{}-{}", processName, thread, getLock);

        getLockResult(getLock, "流程状态变更中，请稍后重试");
        return true;
    }

    /**
     * 释放流程模型创建锁
     * @param processName 流程名称
     * @param getLock 是否获取锁
     */
    public void releaseActiveVersionLock(String processName, Boolean getLock) {
        //获取到锁才能释放锁
        if (!getLock) {
            return;
        }

        boolean releaseLock;
        String thread = Thread.currentThread().getName();
        if (config.getCustomizationLock()) {
            releaseLock = customizationLock.releaseActiveVersionLock(processName);
        }else {
            releaseLock = ACTIVE_VERSION.remove(processName);
        }
        log.info("释放流程启用版本锁：{}-{}-{}", processName, thread, releaseLock);
    }


    /**
     * 获取流程模型创建锁
     * @param processName 流程名称
     */
    public Boolean getVersionCreateLock(String processName) {
        boolean getLock;
        String thread = Thread.currentThread().getName();
        if (config.getCustomizationLock()) {
            getLock = customizationLock.addAndGetVersionCreateLock(processName);
        }else {
            getLock = VERSION_CREATE_LOCK.add(processName);
        }
        log.info("获取流程模型创建锁：{}-{}-{}", processName, thread, getLock);

        getLockResult(getLock, "流程模型创建中，请稍后重试");
        return true;
    }

    /**
     * 释放流程模型创建锁
     * @param processName 流程名称
     * @param getLock 是否获取锁
     */
    public void releaseVersionCreateLock(String processName, Boolean getLock) {
        //获取到锁才能释放锁
        if (!getLock) {
            return;
        }

        boolean releaseLock;
        String thread = Thread.currentThread().getName();
        if (config.getCustomizationLock()) {
            releaseLock = customizationLock.releaseVersionCreateLock(processName);
        }else {
            releaseLock = VERSION_CREATE_LOCK.remove(processName);
        }
        log.info("释放流程模型创建锁：{}-{}-{}", processName, thread, releaseLock);
    }

    /**
     * 获取锁结果
     * @param flag 获取锁结果
     */
    private void getLockResult(Boolean flag, String msg) {
        if (!flag) {
            throw new NeoProcessException(msg);
        }
    }



}
