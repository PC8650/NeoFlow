package org.nf.neoflow.interfaces;

import org.nf.neoflow.component.NeoLockManager;
import org.nf.neoflow.enums.LockEnums;

/**
 * 自定义流程新建模型或执行时的加锁解锁方法
 * 需要实现接口，重写方法，手动注入spring容器
 * 由 {@link NeoLockManager NeoLockManager} 统一处理，保证在得到锁的情况才释放锁
 * @author PC8650
 */
public interface CustomizationLock {

    /**
     * 获取锁
     * @param key 唯一key
     * @param lockName {@link LockEnums LockEnums} 可以此设计对应的策略
     * @return Boolean
     */
    Boolean addAndGetLock(String key, String lockName);

    /**
     * 释放锁
     * @param key 唯一key
     * @param lockName {@link LockEnums LockEnums} 可以此设计对应的策略
     * @return Boolean
     */
    Boolean releaseLock(String key, String lockName);
}
