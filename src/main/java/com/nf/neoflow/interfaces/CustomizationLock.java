package com.nf.neoflow.interfaces;

/**
 * 自定义流程新建模型或执行时的加锁解锁方法
 * 需要实现接口，并重写服务方法，手动注入spring容器
 * @author PC8650
 */
public interface CustomizationLock {

    /**
     * 获取流程状态锁
     * 用于更改流程启用状态、更改流程启用版本
     * @param processName 流程名称
     * @return
     */
    Boolean addAndGetProcessStatusLock(String processName);

    /**
     * 释放流程状态锁
     * 用于更改流程启用状态、更改流程启用版本
     * @param processName 流程名称
     * @return
     */
    Boolean releaseProcessStatusLock(String processName);

    /**
     * 获取版本创建锁
     * @param processName 流程名称
     * @return
     */
    Boolean addAndGetVersionCreateLock(String processName);

    /**
     * 释放版本创建锁
     * @param processName 流程名称
     * @return
     */
    Boolean releaseVersionCreateLock(String processName);
}
