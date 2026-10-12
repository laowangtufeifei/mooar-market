package com.mooar.market.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.dto.OrderItemVO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * 在售挂单筛选结果缓存：同一筛选条件（含分页）多个用户共用一份结果。
 * 清除时机：TTL 到期、容量淘汰、商品上下架/成交/被提走。
 */
@Component
public class MarketSearchCache {

    private final Cache<String, List<OrderItemVO>> cache;

    public MarketSearchCache(MarketProperties properties) {
        MarketProperties.Search conf = properties.getSearch();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(conf.getCacheTtlSeconds()))
                .maximumSize(conf.getCacheMaxSize())
                .build();
    }

    public List<OrderItemVO> get(Long itemId, String key, Supplier<List<OrderItemVO>> loader) {
        return cache.get(itemId + "|" + key, k -> loader.get());
    }

    /**
     * 该 collection 的挂单发生变化，清掉它的全部筛选结果。
     * 事务中进行时挂到提交后执行，避免别的线程读到未提交数据又写回缓存。
     */
    public void invalidate(Long itemId) {
        if (itemId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    clear(itemId);
                }
            });
        } else {
            clear(itemId);
        }
    }

    private void clear(Long itemId) {
        String prefix = itemId + "|";
        cache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }
}
