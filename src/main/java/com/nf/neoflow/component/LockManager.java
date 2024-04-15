package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.exception.NeoProcessException;
import com.nf.neoflow.interfaces.CustomizationLock;
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
     * 流程模型创建锁
     */
    private final ConcurrentHashMap.KeySetView<String, Boolean> VERSION_CREATE_LOCK = ConcurrentHashMap.newKeySet();

    /**
     * 获取流程模型创建锁
     * @param processName 流程名称
     */
    public void getVersionCreateLock(String processName) {
        boolean getLock;
        String thread = Thread.currentThread().getName();
        if (config.getCustomizationLock()) {
            getLock = customizationLock.addAndGetVersionCreateLock(processName);
        }else {
            getLock = VERSION_CREATE_LOCK.add(processName);
        }
        log.info("获取流程模型创建锁：{}-{}-{}", processName, thread, getLock);
        if (!getLock) {
            throw new NeoProcessException("流程模型创建中，请稍后重试");
        }
    }

    /**
     * 释放流程模型创建锁
     * @param processName 流程名称
     */
    public void releaseVersionCreateLock(String processName) {
        boolean releaseLock;
        String thread = Thread.currentThread().getName();
        if (config.getCustomizationLock()) {
            releaseLock = customizationLock.releaseVersionCreateLock(processName);
        }else {
            releaseLock = VERSION_CREATE_LOCK.remove(processName);
        }
        log.info("释放流程模型创建锁：{}-{}-{}", processName, thread, releaseLock);
    }
}
