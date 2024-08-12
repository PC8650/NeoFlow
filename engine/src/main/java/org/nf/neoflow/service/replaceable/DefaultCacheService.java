package org.nf.neoflow.service.replaceable;

import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.nf.neoflow.component.NeoCacheManager;
import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.enums.CacheEnums;
import org.nf.neoflow.interfaces.CustomizationCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 默认缓存服务
 * @author PC8650
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "neo.customization-cache", havingValue = "false", matchIfMissing = true)
public class DefaultCacheService implements CustomizationCache {

    private final NeoFlowConfig config;

    private CaffeineCacheManager cacheManager;

    @PostConstruct
    public void initCacheManager () {
        if (!config.getEnableCache()) {
            return;
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
     * @param cacheType {@link CacheEnums CacheType}缓存类型
     * @param cacheKey 业务key
     * @param value 缓存值
     */
    @Override
    public void setCache(String cacheType, String cacheKey, Object value) {
        Objects.requireNonNull(cacheManager.getCache(cacheType)).put(cacheKey, value);
    }

    /**
     * 获取缓存
     * @param cacheType {@link CacheEnums CacheType}缓存类型
     * @param cacheKey 业务key
     * @param clazz 缓存类型Class
     * @return NeoCacheManager.CacheValue<T>
     * @param <T> 缓存值的类型
     */
    @Override
    public <T> NeoCacheManager.CacheValue<T> getCache(String cacheType, String cacheKey, Class<T> clazz) {
        Cache cache = cacheManager.getCache(cacheType);
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(cacheKey);
            if (valueWrapper != null) {
                Object value = valueWrapper.get();
                return value instanceof NeoCacheManager.NullFlag ? new NeoCacheManager.CacheValue<T>(true) : new NeoCacheManager.CacheValue<T>(false, clazz.cast(value));
            }
        }
        return new NeoCacheManager.CacheValue<T>(false);
    }

    /**
     * 删除缓存
     * @param cacheType {@link CacheEnums CacheType}缓存类型
     * @param cacheKey 业务key
     */
    @Override
    public void deleteCache(String cacheType, String cacheKey) {
        Cache cache = cacheManager.getCache(cacheType);
        if (cache != null) {
            cache.evict(cacheKey);
        }
    }

    /**
     * 删除缓存
     * @param cacheType {@link CacheEnums CacheType}缓存类型
     * @param cacheKeys 业务key
     */
    @Override
    public void deleteCache(String cacheType, List<String> cacheKeys) {
        Cache cache = cacheManager.getCache(cacheType);
        cacheDelete(cache, cacheKeys);
    }

    /**
     * 删除缓存
     * @param caches key: {@link CacheEnums CacheType}，value:cacheKey列表
     */
    @Override
    public void deleteCache(Map<String, List<String>> caches) {
        for (String cacheType : caches.keySet()) {
            Cache cache = cacheManager.getCache(cacheType);
            List<String> keys = caches.get(cacheType);
            cacheDelete(cache, keys);
        }
    }

    /**
     * 删除缓存
     * @param cacheType {@link CacheEnums CacheType}缓存类型
     */
    @Override
    public void deleteCache(String... cacheType) {
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
                cacheDelete(cache, null);
            }
        }
    }

    @Override
    public Object cacheStatistics() {
        String type = CacheEnums.C_S.getType();
        String key = "all";

        NeoCacheManager.CacheValue<Set> cacheValue = getCache(type, key, Set.class);
        if (cacheValue.filter() || cacheValue.value() != null) {
            return cacheValue.value();
        }

        List<NeoCacheManager.CacheStatistics> cacheStatistics = new ArrayList<>();
        for (String cacheType : cacheManager.getCacheNames()) {
            CaffeineCache cache = (CaffeineCache) cacheManager.getCache(cacheType);
            CacheEnums ce = CacheEnums.getByType(cacheType);
            if (ce != null) {
                if (cache == null) {
                    cacheStatistics.add(new NeoCacheManager.CacheStatistics(ce));
                } else {
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = cache.getNativeCache();
                    cacheStatistics.add(new NeoCacheManager.CacheStatistics(ce, nativeCache.stats(), nativeCache.asMap().keySet()));
                }
            }
        }
        setCache(type, key, cacheStatistics);

        return cacheStatistics;
    }

    /**
     * 缓存删除方法
     * @param cache Cache
     * @param cacheKeys cacheKeys
     */
    private void cacheDelete(Cache cache, List<String> cacheKeys) {
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
