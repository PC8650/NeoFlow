package org.nf.neoflow.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nf.neoflow.enums.LockEnums;
import org.nf.neoflow.exception.NeoProcessException;
import org.nf.neoflow.interfaces.CustomizationLock;
import org.springframework.stereotype.Component;

/**
 * 锁管理
 * 加锁、释放放锁操作，保证在得到锁的情况才释放锁
 * @author PC8650
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeoLockManager {

    private final CustomizationLock customizationLock;

    /**
     * 获取锁
     * @param key 唯一key
     * @param lockEnum 锁类型
     */
    public Boolean getLock(String key, LockEnums lockEnum) {
        boolean getLock;
        String thread = Thread.currentThread().getName();
        String lockName = lockEnum.getName();

        getLock = customizationLock.addAndGetLock(key, lockName);

        if (!getLock) {
            log.info("{}，获取锁失败：{}-{}-{}-{}", lockEnum.getMsg(), lockEnum.getName(), key, thread, getLock);
            throw new NeoProcessException(lockEnum.getErrorMsg());
        }

        log.info("{}，获取锁成功：{}-{}-{}-{}", lockEnum.getMsg(), lockEnum.getName(), key, thread, getLock);
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
        if (!getLock) return;

        boolean releaseLock;
        String thread = Thread.currentThread().getName();
        String lockName = lockEnum.getName();

        releaseLock = customizationLock.releaseLock(key, lockName);
        log.info("{}，释放锁：{}-{}-{}-{}", lockEnum.getMsg(), lockEnum.getName(), key, thread, releaseLock);
    }

}
