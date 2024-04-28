package com.nf.neoflow.interfaces;

import com.nf.neoflow.component.NeoCacheManager;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 自定义缓存接口
 * 需要实现接口，重写方法，手动注入spring容器。
 * 缓存过期策略需要根据 {@link com.nf.neoflow.enums.CacheEnums CacheType} 在方法内部决定
 * @author PC8650
 */
public interface CustomizationCache {

    /**
     * 设置缓存
     * @param cacheType {@link com.nf.neoflow.enums.CacheEnums CacheType}缓存类型
     * @param cacheKey 缓存类型+分隔符+业务key
     * @param value 缓存值
     */
    void setCache(String cacheType, String cacheKey, Object value);

    /**
     * 获取缓存
     * @param cacheType {@link com.nf.neoflow.enums.CacheEnums CacheType}缓存类型
     * @param cacheKey 缓存类型+分隔符+业务key
     * @return   缓存值
     */
    <T> NeoCacheManager.CacheValue<T> getCache(String cacheType, String cacheKey, Class<T> clazz);

    /**
     * 删除缓存
     * @param cacheType {@link com.nf.neoflow.enums.CacheEnums CacheType}缓存类型
     * @param cacheKey 缓存类型+分隔符+key
     */
    void deleteCache(String cacheType, String cacheKey);

    /**
     * 删除缓存
     * @param cacheType {@link com.nf.neoflow.enums.CacheEnums CacheType}缓存类型
     * @param cacheKeys cacheKey列表 缓存类型+分隔符+key
     */
    void deleteCache(String cacheType, List<String> cacheKeys);

    /**
     * 删除缓存
     * @param caches key-cacheType {@link com.nf.neoflow.enums.CacheEnums CacheType}，value-cacheKey列表 缓存类型+分隔符+key
     */
    void deleteCache(Map<String, List<String>> caches);

    /**
     * 删除缓存
     * @param cacheType {@link com.nf.neoflow.enums.CacheEnums CacheType}缓存类型
     */
    void deleteCache(String... cacheType);


    /**
     * 获取所有缓存统计信息
     * @return 所有缓存统计信息
     */
    Object cacheStatistics();
}
