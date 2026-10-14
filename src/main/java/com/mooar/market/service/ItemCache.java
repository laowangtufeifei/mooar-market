package com.mooar.market.service;

import com.mooar.market.common.ItemType;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mooar.market.entity.Item;
import com.mooar.market.mapper.ItemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class ItemCache {

    private static final Logger log = LoggerFactory.getLogger(ItemCache.class);

    private final ItemMapper itemMapper;
    private final Cache<Long, Item> byId;
    private final Cache<String, Item> byContractToken;
    private final Cache<String, Item> byContract;
    private volatile List<Item> all;
    private volatile List<Item> scanItems;

    public ItemCache(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
        this.byId = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(128).build();
        this.byContractToken = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(128).build();
        this.byContract = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(128).build();
    }

    public List<Item> getAll() {
        if (all == null) {
            synchronized (this) {
                if (all == null) {
                    all = itemMapper.selectAll();
                }
            }
        }
        return all;
    }

    /**
     * 需要扫块的道具（item.state = 1），进程启动后加载一次
     */
    public List<Item> getScanItems() {
        if (scanItems == null) {
            synchronized (this) {
                if (scanItems == null) {
                    scanItems = itemMapper.selectScanItems();
                    log.info("扫块道具 {} 个: {}", scanItems.size(),
                            scanItems.stream().map(i -> i.getId() + ":" + i.getContract()).toList());
                }
            }
        }
        return scanItems;
    }

    public Item getById(Long id) {
        if (id == null) {
            return null;
        }
        Item item = byId.getIfPresent(id);
        if (item != null) {
            return item;
        }
        item = itemMapper.selectById(id);
        if (item != null) {
            byId.put(id, item);
        }
        return item;
    }

    public Item getByContractAndTokenId(String contract, Long tokenId) {
        String key = contract.toLowerCase() + ":" + tokenId;
        Item item = byContractToken.getIfPresent(key);
        if (item != null) {
            return item;
        }
        item = itemMapper.selectByContractAndTokenId(contract.toLowerCase(), tokenId);
        if (item != null) {
            byContractToken.put(key, item);
        }
        return item;
    }

    public Item getByContract(String contract) {
        if (contract == null || contract.isBlank()) {
            return null;
        }
        String key = contract.toLowerCase();
        Item item = byContract.getIfPresent(key);
        if (item != null) {
            return item;
        }
        item = itemMapper.selectByContract(key);
        if (item != null) {
            byContract.put(key, item);
        }
        return item;
    }

    public Item getGmtItem() {
        for (Item item : getAll()) {
            if (item.getType() == ItemType.ERC20) {
                return item;
            }
        }
        return null;
    }
}
