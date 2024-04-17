package com.nf.neoflow.interfaces;

/**
 * 自定义流程新建模型或执行时的加锁解锁方法
 * 需要实现接口，并重写服务方法，手动注入spring容器
 * 再由 {@link com.nf.neoflow.component.LockManager LockManager} 统一处理，保证在得到锁的情况才释放锁
 * @author PC8650
 */
public interface CustomizationLock {

    /**
     * 获取锁
     * @param key 唯一key
     * @return Boolean
     */
    Boolean addAndGetLock(String key);

    /**
     * 释放锁
     * @param key 唯一key
     * @return Boolean
     */
    Boolean releaseLock(String key);
}
