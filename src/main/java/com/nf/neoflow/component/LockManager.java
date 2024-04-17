package com.nf.neoflow.component;

import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.enums.LockEnums;
import com.nf.neoflow.exception.NeoProcessException;
import com.nf.neoflow.interfaces.CustomizationLock;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 锁管理
 * 加锁、释放放锁操作，保证在得到锁的情况才释放锁
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


    private Map<String,ConcurrentHashMap.KeySetView<String, Boolean>> LOCK_MAP;

    @PostConstruct
    public void lockInit() {
        if (!config.getCustomizationLock()) {
            List<String> locks = LockEnums.allLockNames();
            LOCK_MAP = new HashMap<>(locks.size());
            for (String lock : locks) {
                LOCK_MAP.put(lock, ConcurrentHashMap.newKeySet());
            }
        }
    }

    /**
     * 获取锁
     * @param key 唯一key
     * @param lockEnum 锁类型
     */
    public Boolean getLock(String key, LockEnums lockEnum) {
        boolean getLock;
        String thread = Thread.currentThread().getName();
        if (config.getCustomizationLock()) {
            getLock = customizationLock.addAndGetLock(key);
        }else {
            getLock = LOCK_MAP.get(lockEnum.getName()).add(key);
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
        if (config.getCustomizationLock()) {
            releaseLock = customizationLock.releaseLock(key);
        }else {
            releaseLock = LOCK_MAP.get(lockEnum.getName()).remove(key);
        }
        log.info("{}，释放锁：{}-{}-{}", lockEnum.getMsg(), key, thread, releaseLock);
    }

}
