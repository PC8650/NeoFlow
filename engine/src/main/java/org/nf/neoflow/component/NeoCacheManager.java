package org.nf.neoflow.component;


import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.nf.neoflow.config.NeoFlowConfig;
import org.nf.neoflow.enums.CacheEnums;
import org.nf.neoflow.interfaces.CustomizationCache;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 缓存管理
 * @author PC8650
 */
@Component
@RequiredArgsConstructor
public class NeoCacheManager {

    private final NeoFlowConfig config;

    private final CustomizationCache customizationCache;

    private NullFlag nullFlag;

    @PostConstruct
    public void initCacheManager () {
        if (config.getEnableCache() && config.getCacheNull()) {
            nullFlag = new NullFlag();
        }
    }

    /**
     * 设置缓存
     * @param cacheType 缓存分类
     * @param cacheKey 缓存key
     * @param value 缓存值
     */
    public void setCache(String cacheType, String cacheKey, Object value) {
        if (!config.getEnableCache()) return;

        //缓存值为空
        if (value == null
                || (value instanceof Collection<?> && CollectionUtils.isEmpty((Collection<?>) value))
                || (value instanceof Map<?,?> && CollectionUtils.isEmpty(((Map<?,?>) value)))
        ) {
            //缓存空值
            if (config.getCacheNull()) value = nullFlag;
            //不缓存空值直接return
            else return;
        }

        customizationCache.setCache(cacheType, cacheKey, value);
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

        return customizationCache.getCache(cacheType, cacheKey, clazz);
    }

    /**
     * 删除缓存
     * @param cacheType 缓存分类
     * @param cacheKey 缓存key
     */
    public void deleteCache(String cacheType, String cacheKey) {
        if (!config.getEnableCache()) return;

        customizationCache.deleteCache(cacheType, cacheKey);
    }

    /**
     * 删除缓存
     * @param cacheType 缓存分类
     * @param cacheKeys  缓存key集合，为空删除分类下的所有缓存
     */
    public void deleteCache(String cacheType, List<String> cacheKeys) {
        if (!config.getEnableCache()) return;

        customizationCache.deleteCache(cacheType, cacheKeys);
    }

    /**
     * 删除缓存
     * @param caches Map<cacheType, List<cacheKey>>
     */
    public void deleteCache(Map<String, List<String>> caches) {
        if (!config.getEnableCache()) return;

        customizationCache.deleteCache(caches);
    }

    /**
     * 删除缓存
     * @param cacheType 缓存分类
     */
    public void deleteCache(String... cacheType) {
        if (!config.getEnableCache()) return;

        customizationCache.deleteCache(cacheType);
    }

    /**
     * 获取所有缓存的统计信息
     * @return Set
     */
    public Object cacheStatistics(){
        if (!config.getEnableCache()) return null;

        return customizationCache.cacheStatistics();
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
    public record CacheType(
            String type,
            String info,
            String defaultRule,
            String customRule
    ) {
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
    public record CacheStatistics(
            CacheType cacheType,
            Long estimatedSize,
            Long requestCount,
            Double hitRate,
            Double missRate,
            Long loadSuccessCount,
            Long loadFailureCount,
            Double averageLoadPenalty,
            Long evictionCount,
            Set<Object> estimatedKeys
    ) {
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
