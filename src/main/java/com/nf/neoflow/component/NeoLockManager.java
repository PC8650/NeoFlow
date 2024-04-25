package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.enums.LockEnums;
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
 * 加锁、释放放锁操作，保证在得到锁的情况才释放锁
 * @author PC8650
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeoLockManager {

    private final NeoFlowConfig config;

    @Autowired
    @Lazy
    private CustomizationLock customizationLock;

    /**
     * 锁集合
     * 实际维护的是ConcurrentHashMap，KeySetView进行add，ConcurrentHashMap会put对应的key-value，value为一个缓存的共享值
     */
    private final ConcurrentHashMap.KeySetView<String, Boolean> LOCK_MAP = ConcurrentHashMap.newKeySet();

    /**
     * 获取锁
     * @param key 唯一key
     * @param lockEnum 锁类型
     */
    public Boolean getLock(String key, LockEnums lockEnum) {
        boolean getLock;
        String thread = Thread.currentThread().getName();
        String lockName = lockEnum.getName();
        key = lockName + ":" + key;
        if (config.getCustomizationLock()) {
            getLock = customizationLock.addAndGetLock(key, lockName);
        }else {
            getLock = LOCK_MAP.add(key);
        }

        if (!getLock) {
            log.info("{}，获取锁失败：{}-{}-{}", lockEnum.getMsg(), key, thread, getLock);
            throw new NeoProcessException(lockEnum.getErrorMsg());
        }

        log.info("{}，获取锁成功：{}-{}-{}", lockEnum.getMsg(), key, thread, getLock);
        return true;
    }

    /**
     * 释放锁
     * @param key 唯一key
     * @param getLock 是否获取锁
     * @param lockEnum 锁类型
     */
    public void releaseLock(String key, Boolean getLock, LockEnums lockEnum) {
        //获取到锁才能释放锁
        if (!getLock) {
            return;
        }

        boolean releaseLock;
        String thread = Thread.currentThread().getName();
        String lockName = lockEnum.getName();
        key = lockName + ":" + key;
        if (config.getCustomizationLock()) {
            releaseLock = customizationLock.releaseLock(key, lockName);
        }else {
            releaseLock = LOCK_MAP.remove(key);
        }
        log.info("{}，释放锁：{}-{}-{}", lockEnum.getMsg(), key, thread, releaseLock);
    }

}
