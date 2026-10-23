package com.mooar.market.service;

import com.mooar.market.entity.NftCollection;
import com.mooar.market.mapper.CollectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * collection 表数据量很小（一个合约一条），整体加载后常驻内存
 */
@Component
@RequiredArgsConstructor
public class CollectionCache {

    private final CollectionMapper collectionMapper;

    private volatile List<NftCollection> all;
    private volatile Map<Long, NftCollection> byId;
    private volatile Map<String, NftCollection> byContract;

    public List<NftCollection> getAll() {
        ensureLoaded();
        return all;
    }

    public NftCollection getById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        ensureLoaded();
        return byId.get(id);
    }

    public NftCollection getByContract(String contract) {
        if (contract == null || contract.isBlank()) {
            return null;
        }
        ensureLoaded();
        return byContract.get(contract.toLowerCase());
    }

    private void ensureLoaded() {
        if (all != null) {
            return;
        }
        synchronized (this) {
            if (all != null) {
                return;
            }
            List<NftCollection> list = collectionMapper.selectAll();
            Map<Long, NftCollection> ids = new HashMap<>(list.size() * 2);
            Map<String, NftCollection> contracts = new HashMap<>(list.size() * 2);
            for (NftCollection c : list) {
                ids.put(c.getId(), c);
                if (c.getContract() != null) {
                    contracts.put(c.getContract().toLowerCase(), c);
                }
            }
            all = list;
            byId = ids;
            byContract = contracts;
        }
    }
}
