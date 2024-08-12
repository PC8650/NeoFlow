package org.nf.neoflow.service.replaceable;

import lombok.RequiredArgsConstructor;
import org.nf.neoflow.interfaces.CustomizationLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认锁服务
 * @author PC8650
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "neo.customization-lock", havingValue = "false", matchIfMissing = true)
public class DefaultLockService implements CustomizationLock {

    private final String MK = "%s:%s";

    /**
     * 锁集合
     * 实际维护的是ConcurrentHashMap，KeySetView进行add，ConcurrentHashMap会put对应的key-value，value为一个缓存的共享值
     */
    private final ConcurrentHashMap.KeySetView<String, Boolean> LOCK_MAP = ConcurrentHashMap.newKeySet();

    /**
     * 获取锁
     * @param key 唯一key
     * @param lockName {@link org.nf.neoflow.enums.LockEnums LockEnums} 锁类型 可以此设计对应的策略
     * @return 获取锁是否成功
     */
    @Override
    public Boolean addAndGetLock(String key, String lockName) {
        return LOCK_MAP.add(String.format(MK, lockName, key));
    }

    /**
     * 释放锁
     * @param key 唯一key
     * @param lockName {@link org.nf.neoflow.enums.LockEnums LockEnums} 锁类型 可以此设计对应的策略
     * @return 释放锁是否成功
     */
    @Override
    public Boolean releaseLock(String key, String lockName) {
        return LOCK_MAP.remove(String.format(MK, lockName, key));
    }

}
