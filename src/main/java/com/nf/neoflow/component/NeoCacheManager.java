package com.nf.neoflow.component;


import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.nf.neoflow.config.NeoFlowConfig;
import com.nf.neoflow.enums.CacheEnums;
import com.nf.neoflow.interfaces.CustomizationCache;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
        cacheManager.setCacheNames(CacheEnums.filterStatistics());
        cacheManager.setCaffeine(Caffeine.newBuilder()
                //初始容量
                .initialCapacity(config.getInitCacheCount())
                //最大容量
                .maximumSize(config.getMaxCapacityCount())
                //单位时间内没被 读/写 则过期
                .expireAfterAccess(config.getExpire(), TimeUnit.MINUTES)
                //开启统计
                .recordStats());

        //单独处理统计缓存
        cacheManager.registerCustomCache(
                CacheEnums.C_S.getType(),
                Caffeine.newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                //单位时间没被 写 则过期
                .expireAfterWrite(config.getStatisticExpire(), TimeUnit.SECONDS)
                .recordStats().build());
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
        Cache cache = cacheManager.getCache(cacheType);
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(cacheKey);
            if (valueWrapper != null) {
                Object value = valueWrapper.get();
                return value instanceof NullFlag? new CacheValue<T>(true) : new CacheValue<T>(false, (T) value);
            }
        }
        return new CacheValue<T>(false);
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
     * @param cacheKeys  缓存key集合，为空删除分类下的所有缓存
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
                if (CollectionUtils.isEmpty(cacheKeys)) {
                    nativeCache.invalidateAll();
                }else {
                    nativeCache.invalidateAll(cacheKeys);
                }
            }
        }
    }

    /**
     * 删除缓存
     * @param cacheType 缓存分类
     */
    public void deleteCache(String... cacheType) {
        if (config.getEnableCache()) {
            //自定义策略
            if (config.getCustomizationCache()) {
                customizationCache.deleteCache(cacheType);
                return;
            }
            //默认策略
            if (cacheType == null || cacheType.length == 0) {
                for (String cacheName : cacheManager.getCacheNames()) {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                    }
                }
            } else {
                for (String type : cacheType) {
                    Cache cache = cacheManager.getCache(type);
                    if (cache != null) {
                        com.github.benmanes.caffeine.cache.Cache nativeCache = (com.github.benmanes.caffeine.cache.Cache) cache.getNativeCache();
                        nativeCache.invalidateAll();
                    }
                }
            }
        }
    }

    /**
     * 获取所有缓存的统计信息
     * @return Set
     */
    public Object cacheStatistics(){
        if (!config.getEnableCache()) {
            return null;
        }

        String type = CacheEnums.C_S.getType();
        String key = "all";
        if (config.getCustomizationCache()) {
            return customizationCache.cacheStatistics();
        }

        CacheValue<Set> cacheValue = getCache(type, key, Set.class);
        if (cacheValue.filter() || cacheValue.value() != null) {
            return cacheValue.value();
        }

        List<CacheStatistics> cacheStatistics = new ArrayList<>();
        for (String cacheType : cacheManager.getCacheNames()) {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheType);
            CacheEnums ce = CacheEnums.getByType(cacheType);
            if (ce != null) {
                if (cache == null) {
                   cacheStatistics.add(new CacheStatistics(ce));
                } else {
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = cache.getNativeCache();
                    cacheStatistics.add(new CacheStatistics(ce, nativeCache.stats(), nativeCache.asMap().keySet()));
                }
            }
        }
        setCache(type, key, cacheStatistics);
        return cacheStatistics;
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
     * @param <T> 缓存
     */
    public record CacheValue<T>(Boolean filter, T value) {
        public CacheValue(Boolean filter) {
            this(filter, null);
        }
    }

    /**
     * 缓存类型
     * @param type 缓存类型
     * @param info 信息
     * @param defaultRule 默认策略规则
     * @param customRule 自定义策略规则
     */
    public record CacheType(String type, String info, String defaultRule, String customRule) {
        public CacheType(CacheEnums ce) {
            this(ce.getType(), ce.getInfo(), ce.getDefaultRule(), ce.getCustomRule());
        }
    }

    /**
     * 缓存统计信息
     * @param cacheType 缓存类型
     * @param estimatedSize 估计数量
     * @param requestCount 请求次数
     * @param hitRate 命中率
     * @param missRate 未命中率
     * @param loadSuccessCount 加载新值成功的次数
     * @param loadFailureCount 加载新值失败的次数
     * @param averageLoadPenalty 加载操作的平均时间(ms)
     * @param evictionCount 驱逐缓存数量
     * @param estimatedKeys 估计存在的key
     */
    public record CacheStatistics(CacheType cacheType, Long estimatedSize,
                                  Long requestCount, Double hitRate, Double missRate,
                                  Long loadSuccessCount, Long loadFailureCount,
                                  Double averageLoadPenalty, Long evictionCount, Set<Object> estimatedKeys) {

        public CacheStatistics(CacheEnums ce, CacheStats stats, Set<Object> estimatedKeys) {
            this(new CacheType(ce), (long) estimatedKeys.size(),
                    stats.requestCount(), stats.hitRate(), stats.missRate(),
                    stats.loadSuccessCount(), stats.loadFailureCount(),
                    stats.averageLoadPenalty()/1000000,
                    stats.evictionCount(), estimatedKeys);
        }

        public CacheStatistics(CacheEnums ce) {
            this(new CacheType(ce), 0L,
                    0L,0D, 0D,
                    0L, 0L,
                    0D,
                    0L, null);
        }
    }

}
