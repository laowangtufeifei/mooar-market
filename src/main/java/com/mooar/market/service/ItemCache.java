package com.mooar.market.service;

import com.mooar.market.common.ItemType;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mooar.market.entity.Item;
import com.mooar.market.mapper.ItemMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class ItemCache {

    private final ItemMapper itemMapper;
    private final Cache<Long, Item> byId;
    private final Cache<String, Item> byContractToken;
    private volatile List<Item> all;

    public ItemCache(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
        this.byId = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(128).build();
        this.byContractToken = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(10)).maximumSize(128).build();
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

    public Item getGmtItem() {
        for (Item item : getAll()) {
            if (item.getType() == ItemType.ERC20) {
                return item;
            }
        }
        return null;
    }
}
