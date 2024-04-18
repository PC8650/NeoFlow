package com.nf.neoflow.component;


import com.github.benmanes.caffeine.cache.Caffeine;
import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.constants.CacheType;
import com.nf.neoflow.interfaces.CustomizationCache;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author PC8650
 */
@Component
@RequiredArgsConstructor
public class NeoCacheManager {

    private final NeoFlowConfig config;

    @Autowired
    @Lazy
    private CustomizationCache customizationCache;

    private CaffeineCacheManager cacheManager;

    private NullFlag nullFlag;

    @PostConstruct
    public void initCacheManager () {
        if (!config.getEnableCache()) {
            return;
        }
        if (config.getCacheNull()) {
            nullFlag = new NullFlag();
        }

        cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CacheType.getAllType());
        cacheManager.setCaffeine(Caffeine.newBuilder()
                //初始容量
                .initialCapacity(config.getInitCacheCount())
                //最大容量
                .maximumSize(config.getMaxCapacityCount())
                //单位时间内没被 读/写 则过期
                .expireAfterAccess(config.getExpire(), TimeUnit.MINUTES)
                //开启统计
                .recordStats());
    }

    /**
     * 设置缓存
     * @param cacheType 缓存分类
     * @param cacheKey 缓存key
     * @param value 缓存值
     */
    public void setCache(String cacheType, String cacheKey, Object value) {
        if (config.getEnableCache()) {
            //自定义策略
            if (config.getCustomizationCache()) {
                cacheKey = mergeKey(cacheType, cacheKey);
                customizationCache.setCache(cacheType, cacheKey, value);
                return;
            }

            //默认策略
            if (value == null
                    || (value instanceof Collection<?> && CollectionUtils.isEmpty((Collection<?>) value))
                    || (value instanceof Map<?,?> && CollectionUtils.isEmpty(((Map<?,?>) value)))
            ) {
                if (config.getCacheNull()) {
                    cacheManager.getCache(cacheType).put(cacheKey, nullFlag);
                }
            } else {
                cacheManager.getCache(cacheType).put(cacheKey, value);
            }
        }
    }

    /**
     * 获取缓存
     * @param cacheType 缓存分类
     * @param cacheKey 缓存key
     * @return T
     */
    public <T>  CacheValue<T> getCache(String cacheType, String cacheKey, Class<T> clazz) {
        if (!config.getEnableCache()) {
            return new CacheValue<>(false,null);
        }

        //自定义策略
        if (config.getCustomizationCache()) {
            return customizationCache.getCache(cacheType, mergeKey(cacheType, cacheKey), clazz);
        }

        //默认策略
        Object value = cacheManager.getCache(cacheType).get(cacheKey);
        return value instanceof NullFlag? new CacheValue<T>(true, null) : new CacheValue<T>(false, (T) value);
    }

    /**
     * 删除缓存
     * @param cacheType 缓存分类
     * @param cacheKey 缓存key
     */
    public void deleteCache(String cacheType, String cacheKey) {
        if (config.getEnableCache()) {
            //自定义策略
            if (config.getCustomizationCache()) {
                customizationCache.deleteCache(cacheType, mergeKey(cacheType, cacheKey));
                return;
            }
            //默认策略
            Cache cache = cacheManager.getCache(cacheType);
            if (cache != null) {
                cache.evict(cacheKey);
            }
        }
    }

    /**
     * 删除缓存
     * @param cacheType 缓存分类
     * @param cacheKeys  缓存key集合
     */
    public void deleteCache(String cacheType, List<String> cacheKeys) {
        if (config.getEnableCache()) {
            //自定义策略
            if (config.getCustomizationCache()) {
                cacheKeys.replaceAll(s -> mergeKey(cacheType, s));
                customizationCache.deleteCache(cacheType, cacheKeys);
                return;
            }
            //默认策略
            Cache cache = cacheManager.getCache(cacheType);
            if (cache != null) {
                com.github.benmanes.caffeine.cache.Cache nativeCache = (com.github.benmanes.caffeine.cache.Cache) cache.getNativeCache();
                nativeCache.invalidateAll(cacheKeys);
            }
        }
    }

    /**
     * 删除缓存
     * @param prefix 多段key的前缀
     * @param caches key-缓存分类 value-缓存key集合
     */
    public void deleteCache(String prefix, Map<String, List<String>> caches) {
        if (config.getEnableCache()) {
            //自定义策略
            if (config.getCustomizationCache()) {
                caches.forEach((cacheType, cacheKeys) -> {
                    cacheKeys.replaceAll(s -> mergeKey(prefix, s));
                });
                customizationCache.deleteCache(caches);
                return;
            }
            //默认策略
            caches.forEach((cacheType, cacheKeys) -> {
                 Cache cache = cacheManager.getCache(cacheType);
                 if (cache != null) {
                    com.github.benmanes.caffeine.cache.Cache nativeCache = (com.github.benmanes.caffeine.cache.Cache) cache.getNativeCache();
                    nativeCache.invalidateAll(cacheKeys);
                 }
            });
        }
    }

    /**
     * key由多段组成时，合并key
     * @param key 各段的key
     * @return 合并后的key
     */
    public String mergeKey(String... key) {
        int length;
        if (key == null || (length = key.length) == 0) {
            throw new NullPointerException("缓存key为空");
        }

        if (length == 1) {
            return key[0];
        }

        return String.join(String.valueOf(config.getSeparate()), key);
    }


    /**
     * 空值标记
     */
    public static class NullFlag{}

    /**
     * 缓存值
     * @param filter 是否为过滤的空值，判断缓存空值情况下是否跳过后续的数据库查询
     * @param value 缓存值，在filter为true时，统一为null
     * @param <T>
     */
    public record CacheValue<T>(Boolean filter, T value) {}

}
