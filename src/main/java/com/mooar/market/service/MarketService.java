package com.mooar.market.service;

import com.mooar.market.common.ItemType;

import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.common.NftState;
import com.mooar.market.common.OrderState;
import com.mooar.market.common.TraitDef;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.dto.CollectionVO;
import com.mooar.market.dto.NftOrderVO;
import com.mooar.market.dto.OrderAggRow;
import com.mooar.market.dto.OrderDetailVO;
import com.mooar.market.dto.OrderItemVO;
import com.mooar.market.dto.OrderRow;
import com.mooar.market.dto.TraitSearchReq;
import com.mooar.market.entity.BalanceLog;
import com.mooar.market.entity.Item;
import com.mooar.market.entity.Order;
import com.mooar.market.entity.Nft;
import com.mooar.market.entity.Trade;
import com.mooar.market.entity.UserAsset;
import com.mooar.market.mapper.BalanceLogMapper;
import com.mooar.market.mapper.OrderMapper;
import com.mooar.market.mapper.NftMapper;
import com.mooar.market.mapper.TradeMapper;
import com.mooar.market.mapper.UserAssetMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketService {

    private static final Logger orderLog = LoggerFactory.getLogger("ORDER_LOG");

    private static final String SORT_DESC = "desc";

    private final ItemCache itemCache;
    private final OrderMapper orderMapper;
    private final TradeMapper tradeMapper;
    private final UserAssetMapper userAssetMapper;
    private final NftMapper nftMapper;
    private final BalanceLogMapper balanceLogMapper;
    private final MarketProperties properties;
    private final ItemTraitService itemTraitService;
    private final MarketSearchCache searchCache;

    /**
     * 挂单：冻结资产并生成卖单
     */
    @Transactional
    public Long createOrder(Long userId, Long itemId, Long amount, Long nftId, Long price) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (price == null || price <= 0) {
            throw new BizException(ErrorCode.ORDER_AMOUNT_ERROR);
        }
        long now = System.currentTimeMillis();
        Order order = new Order();
        order.setSellerId(userId);
        order.setItemId(itemId);
        order.setPrice(price);
        order.setState(OrderState.ACTIVE);
        order.setBuyerId(0L);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        if (item.getType() == ItemType.ERC721) {
            if (nftId == null) {
                throw new BizException(ErrorCode.PARAM_ERROR);
            }
            Nft nft = nftMapper.selectById(nftId);
            if (nft == null || !nft.getOwnerId().equals(userId)) {
                throw new BizException(ErrorCode.NFT_NOT_OWNER);
            }
            if (!Integer.valueOf(NftState.NORMAL).equals(nft.getState())) {
                throw new BizException(ErrorCode.NFT_LOCKED);
            }
            order.setNftId(nftId);
            order.setAmount(1L);
        } else {
            if (amount == null || amount <= 0) {
                throw new BizException(ErrorCode.ORDER_AMOUNT_ERROR);
            }
            order.setAmount(amount);
            order.setNftId(0L);
        }

        orderMapper.insert(order);
        if (item.getType() == ItemType.ERC721) {
            if (nftMapper.updateState(nftId, NftState.NORMAL, NftState.LISTING, order.getId(), now) == 0) {
                throw new BizException(ErrorCode.NFT_LOCKED);
            }
        } else {
            if (userAssetMapper.freeze(userId, itemId, amount, now) == 0) {
                throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
            }
        }
        orderLog.info("挂单 orderId={} seller={} item={} amount={} nftId={} price={}",
                order.getId(), userId, itemId, amount, nftId, price);
        searchCache.invalidate(itemId);
        return order.getId();
    }

    /**
     * 吃单成交：买家付 GMT，资产转移，卖家收 GMT
     */
    @Transactional
    public void buyOrder(Long buyerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getState() != OrderState.ACTIVE) {
            throw new BizException(ErrorCode.ORDER_NOT_ACTIVE);
        }
        if (order.getSellerId().equals(buyerId)) {
            throw new BizException(ErrorCode.ORDER_SELF_BUY);
        }
        Item item = itemCache.getById(order.getItemId());
        long now = System.currentTimeMillis();
        long total = order.getAmount() * order.getPrice();
        long fee = total * properties.getFee().getTradeFeeRate() / 100;
        long sellerIncome = total - fee;
        Long gmtId = itemCache.getGmtItem().getId();

        if (orderMapper.updateState(orderId, OrderState.ACTIVE, OrderState.SOLD, buyerId, now) == 0) {
            throw new BizException(ErrorCode.ORDER_NOT_ACTIVE);
        }
        if (userAssetMapper.subAvailable(buyerId, gmtId, total, now) == 0) {
            throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
        }
        userAssetMapper.addAvailable(order.getSellerId(), gmtId, sellerIncome, now);

        if (item.getType() == ItemType.ERC721) {
            nftMapper.updateOwner(order.getNftId(), buyerId, NftState.NORMAL, now);
        } else {
            userAssetMapper.subFrozen(order.getSellerId(), item.getId(), order.getAmount(), now);
            creditAvailable(buyerId, item.getId(), order.getAmount(), now);
        }

        Trade trade = new Trade();
        trade.setOrderId(orderId);
        trade.setItemId(order.getItemId());
        trade.setNftId(order.getNftId());
        trade.setAmount(order.getAmount());
        trade.setPrice(order.getPrice());
        trade.setTotal(total);
        trade.setFee(fee);
        trade.setSellerId(order.getSellerId());
        trade.setBuyerId(buyerId);
        trade.setCreatedAt(now);
        tradeMapper.insert(trade);

        writeBalanceLog(buyerId, gmtId, -total, now, 3, orderId);
        writeBalanceLog(order.getSellerId(), gmtId, sellerIncome, now, 3, orderId);

        orderLog.info("成交 orderId={} buyer={} seller={} item={} amount={} price={} total={} fee={}",
                orderId, buyerId, order.getSellerId(), order.getItemId(), order.getAmount(), order.getPrice(), total, fee);
        searchCache.invalidate(order.getItemId());
    }

    /**
     * 取消挂单并解冻资产
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getSellerId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (order.getState() != OrderState.ACTIVE) {
            throw new BizException(ErrorCode.ORDER_NOT_ACTIVE);
        }
        long now = System.currentTimeMillis();
        if (orderMapper.updateState(orderId, OrderState.ACTIVE, OrderState.CANCELED, 0L, now) == 0) {
            throw new BizException(ErrorCode.ORDER_NOT_ACTIVE);
        }
        Item item = itemCache.getById(order.getItemId());
        if (item.getType() == ItemType.ERC721) {
            nftMapper.updateState(order.getNftId(), NftState.LISTING, NftState.NORMAL, 0L, now);
        } else {
            userAssetMapper.unfreeze(userId, order.getItemId(), order.getAmount(), now);
        }
        orderLog.info("取消挂单 orderId={} seller={} item={}", orderId, userId, order.getItemId());
        searchCache.invalidate(order.getItemId());
    }

    /**
     * 查询在售挂单列表
     */
    public List<Order> listActive(Long itemId, int page, int pageSize) {
        Item item = itemCache.getById(itemId);
        List<Order> result = new ArrayList<>();
        if (item.getType() == ItemType.ERC20) {
            CollectionVO collectionVO = buildCollection(item);
            Order order = new Order();
            order.setAmount(Long.valueOf(collectionVO.getListingCount()));
            order.setItemId(itemId);
            order.setPrice(collectionVO.getMinPrice());
            return result;
        }
        return orderMapper.selectActive(itemId, (page - 1) * pageSize, pageSize);
    }

    /**
     * collection 的筛选条件定义（槽位 + 属性名 + 值编码），供前端渲染筛选菜单
     */
    public List<Map<String, Object>> listTraitDefs(Long itemId) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (TraitDef def : itemTraitService.getTraitDefs(itemId).values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", def.getName());
            m.put("type", def.getType());
            if ("number".equals(def.getType())) {
                m.put("min", def.getMin());
                m.put("max", def.getMax());
            } else {
                List<Map<String, Object>> values = new ArrayList<>();
                if (def.getValues() != null) {
                    def.getValues().forEach((k, v) -> {
                        Map<String, Object> vm = new LinkedHashMap<>();
                        vm.put("value", k);
                        vm.put("code", v);
                        values.add(vm);
                    });
                }
                m.put("values", values);
            }
            result.add(m);
        }
        return result;
    }

    /**
     * 按 trait 条件搜索 nft，filters 形如 "Rarity:Legendary,Level:10-30"
     */
    public List<Nft> searchNfts(Long itemId, String filters, int page, int pageSize) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", itemId);
        params.put("conds", itemTraitService.translateFilters(itemId, filters));
        params.put("offset", Math.max(0, (page - 1) * pageSize));
        params.put("limit", pageSize);
        return nftMapper.searchByTraits(params);
    }

    /**
     * 按属性筛选在售挂单，同一条件的结果走缓存
     */
    public List<OrderItemVO> searchOrders(Long itemId, TraitSearchReq req) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        // 1155 是按量成交的同质化道具：列表只给「最低价 + 在售总量」，不逐单返回，分页/排序/属性筛选都不适用
        if (item.getType() == ItemType.ERC1155) {
            return searchCache.get(itemId, "1155-agg", () -> queryAggOrders(item));
        }
        int page = Math.max(1, req.getPage());
        int pageSize = Math.min(Math.max(1, req.getPageSize()), properties.getSearch().getMaxPageSize());
        boolean is721 = item.getType() == ItemType.ERC721;
        String tokenIdLike = is721 ? digitsOnly(req.getTokenId()) : null;
        boolean desc = SORT_DESC.equalsIgnoreCase(req.getSortOrder());
        String key = cacheKey(page, pageSize, req.getTraits(), tokenIdLike, desc);
        return searchCache.get(itemId, key,
                () -> queryOrders(item, req.getTraits(), tokenIdLike, desc, page, pageSize));
    }

    private List<OrderItemVO> queryOrders(Item item, Map<String, List<String>> traits, String tokenIdLike,
                                          boolean desc, int page, int pageSize) {
        List<ItemTraitService.TraitCond> conds = item.getType() == ItemType.ERC721
                ? itemTraitService.translateTraits(item.getId(), traits)
                : Collections.emptyList();
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", item.getId());
        params.put("activeState", OrderState.ACTIVE);
        params.put("conds", conds);
        params.put("offset", (page - 1) * pageSize);
        params.put("limit", pageSize);
        if (tokenIdLike != null) {
            params.put("tokenIdLike", tokenIdLike);
        }
        params.put("desc", desc);
        List<OrderRow> rows = orderMapper.searchOrders(params);

        String itemImage = item.getImagePrefix();
        List<OrderItemVO> result = new ArrayList<>(rows.size());
        for (OrderRow row : rows) {
            OrderItemVO vo = new OrderItemVO();
            vo.setOrderId(row.getOrderId());
            vo.setItemId(row.getItemId());
            vo.setSellerId(row.getSellerId());
            vo.setNftId(row.getNftId());
            vo.setTokenId(row.getTokenId() == null ? 0L : row.getTokenId());
            vo.setImage(row.getNftImage() == null || row.getNftImage().isBlank() ? itemImage : row.getNftImage());
            vo.setAmount(row.getAmount());
            vo.setPrice(row.getPrice());
            result.add(vo);
        }
        return result;
    }

    /**
     * 1155 挂单聚合：一条记录 = 最低价 + 在售总量。
     * 没有在售挂单时返回空列表；orderId / sellerId 对聚合行无意义，置空
     */
    private List<OrderItemVO> queryAggOrders(Item item) {
        OrderAggRow agg = orderMapper.selectAggByItem(item.getId(), OrderState.ACTIVE);
        if (agg == null || agg.getTotalAmount() == null || agg.getTotalAmount() <= 0) {
            return Collections.emptyList();
        }
        OrderItemVO vo = new OrderItemVO();
        vo.setItemId(item.getId());
        vo.setNftId(0L);
        vo.setTokenId(0L);
        vo.setImage(item.getImagePrefix());
        vo.setAmount(agg.getTotalAmount());
        vo.setPrice(agg.getMinPrice());
        List<OrderItemVO> result = new ArrayList<>(1);
        result.add(vo);
        return result;
    }

    /**
     * 挂单详情：属性等完整内容在这里返回
     */
    public OrderDetailVO orderDetail(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        Item item = itemCache.getById(order.getItemId());
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderId(order.getId());
        vo.setItemId(order.getItemId());
        vo.setSellerId(order.getSellerId());
        vo.setNftId(order.getNftId());
        vo.setAmount(order.getAmount());
        vo.setPrice(order.getPrice());
        vo.setState(order.getState());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setUpdatedAt(order.getUpdatedAt());
        vo.setTokenId(0L);
        if (item != null) {
            vo.setItemName(item.getName());
            vo.setItemImage(item.getImagePrefix());
        }
        if (order.getNftId() != null && order.getNftId() > 0) {
            Nft nft = nftMapper.selectById(order.getNftId());
            if (nft != null) {
                vo.setTokenId(nft.getTokenId());
                vo.setImage(nft.getImage());
                vo.setMetadata(nft.getMetadata());
            }
        }
        if (vo.getImage() == null || vo.getImage().isBlank()) {
            vo.setImage(vo.getItemImage());
        }
        return vo;
    }

    /**
     * 缓存 key：条件规范化（属性名与值都排序），保证同一条件不同顺序命中同一份缓存
     */
    private String cacheKey(int page, int pageSize, Map<String, List<String>> traits, String tokenIdLike,
                            boolean desc) {
        StringBuilder sb = new StringBuilder();
        sb.append(page).append('#').append(pageSize);
        if (tokenIdLike != null) {
            sb.append("#token=").append(tokenIdLike);
        }
        sb.append("#sort=").append(desc ? "Desc" : "Asc");
        if (traits != null && !traits.isEmpty()) {
            traits.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        List<String> values = new ArrayList<>(e.getValue());
                        Collections.sort(values);
                        sb.append('#').append(e.getKey()).append('=').append(String.join(",", values));
                    });
        }
        return sb.toString();
    }

    /**
     * tokenId 模糊搜索：只接受纯数字，顺带挡掉 LIKE 通配符（% _），非法输入按不筛处理
     */
    private String digitsOnly(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return null;
        }
        String s = tokenId.trim();
        return s.matches("\\d{1,19}") ? s : null;
    }

    /**
     * collection 列表：可交易道具（box/badge）的聚合信息
     */
    public List<CollectionVO> listCollections() {
        List<Item> items = itemCache.getAll();
        List<CollectionVO> result = new ArrayList<>();
        for (Item item : items) {
            if (item.getType() == ItemType.ERC20) {
                continue;
            }
            result.add(buildCollection(item));
        }
        return result;
    }

    public List<CollectionVO> collectionsList() {
        List<Item> items = itemCache.getAll();
        List<CollectionVO> result = new ArrayList<>();
        for (Item item : items) {
            if(item.getType() == ItemType.ERC721 || item.getType() == ItemType.ERC1155)
                result.add(buildCollectionList(item));
        }
        return result;
    }

    /**
     * 兼容 itemId 与合约地址两种入参，统一解析成 itemId
     */
    public Long resolveItemId(String key) {
        if (key == null || key.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        String k = key.trim();
        Item item;
        if (k.length() == 42 && (k.startsWith("0x") || k.startsWith("0X"))) {
            item = itemCache.getByContract(k.toLowerCase());
        } else if (k.length() == 40 && k.matches("(?i)[0-9a-f]{40}")) {
            item = itemCache.getByContract("0x" + k.toLowerCase());
        } else if (k.matches("\\d+")) {
            item = itemCache.getById(Long.parseLong(k));
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        return item.getId();
    }

    /**
     * collection 详情：统一以列表返回，1155 返回单元素聚合，721 返回 NFT 挂单列表
     */
    public List<Object> collectionDetail(Long itemId) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        List<Object> list = new ArrayList<>();
        if (item.getType() != ItemType.ERC721) {
            list.add(buildCollection(item));
            return list;
        }
        List<Order> orders = orderMapper.selectActiveByItem(itemId);
        for (Order o : orders) {
            Nft nft = nftMapper.selectById(o.getNftId());
            if (nft == null) {
                continue;
            }
            NftOrderVO vo = new NftOrderVO();
            vo.setOrderId(o.getId());
            vo.setNftId(nft.getId());
            vo.setTokenId(nft.getTokenId());
            vo.setImage(nft.getImage());
            vo.setMetadata(nft.getMetadata());
            vo.setPrice(o.getPrice());
            list.add(vo);
        }
        return list;
    }

    /**
     * 1155 批量预下单：按价格从低到高吃单，返回总价
     */
    public long previewBuy(Long buyerId, Long itemId, long amount) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (item.getType() != ItemType.ERC1155 || amount <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        List<Order> orders = orderMapper.selectActiveByItem(itemId);
        return computeTotal(orders, amount, buyerId);
    }

    /**
     * 1155 批量成交：价格与预下单一致才成交，支持部分成交
     */
    @Transactional
    public void buyByAmount(Long buyerId, Long itemId, long amount, long expectedTotal) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (item.getType() != ItemType.ERC1155 || amount <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        long now = System.currentTimeMillis();
        Long gmtId = itemCache.getGmtItem().getId();
        int rate = properties.getFee().getTradeFeeRate();
        List<Order> orders = orderMapper.selectActiveByItem(itemId);
        long actualTotal = computeTotal(orders, amount, buyerId);
        if (actualTotal != expectedTotal) {
            throw new BizException(ErrorCode.ORDER_PRICE_CHANGED);
        }
        long need = amount;
        for (Order o : orders) {
            if (need <= 0) {
                break;
            }
            if (o.getSellerId().equals(buyerId)) {
                continue;
            }
            long take = Math.min(o.getAmount(), need);
            long cost = take * o.getPrice();
            long fee = cost * rate / 100;
            if (take == o.getAmount()) {
                if (orderMapper.updateState(o.getId(), OrderState.ACTIVE, OrderState.SOLD, buyerId, now) == 0) {
                    throw new BizException(ErrorCode.ORDER_PRICE_CHANGED);
                }
            } else {
                if (orderMapper.subAmount(o.getId(), take, now) == 0) {
                    throw new BizException(ErrorCode.ORDER_PRICE_CHANGED);
                }
            }
            if (userAssetMapper.subAvailable(buyerId, gmtId, cost, now) == 0) {
                throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
            }
            userAssetMapper.addAvailable(o.getSellerId(), gmtId, cost - fee, now);
            userAssetMapper.subFrozen(o.getSellerId(), itemId, take, now);
            creditAvailable(buyerId, itemId, take, now);

            Trade trade = new Trade();
            trade.setOrderId(o.getId());
            trade.setItemId(itemId);
            trade.setNftId(0L);
            trade.setAmount(take);
            trade.setPrice(o.getPrice());
            trade.setTotal(cost);
            trade.setFee(fee);
            trade.setSellerId(o.getSellerId());
            trade.setBuyerId(buyerId);
            trade.setCreatedAt(now);
            tradeMapper.insert(trade);

            writeBalanceLog(buyerId, gmtId, -cost, now, 3, o.getId());
            writeBalanceLog(o.getSellerId(), gmtId, cost - fee, now, 3, o.getId());

            orderLog.info("批量成交 orderId={} buyer={} seller={} item={} take={} price={} cost={} fee={}",
                    o.getId(), buyerId, o.getSellerId(), itemId, take, o.getPrice(), cost, fee);
            need -= take;
        }
        searchCache.invalidate(itemId);
    }

    private CollectionVO buildCollection(Item item) {
        List<Order> orders = orderMapper.selectActiveByItem(item.getId());
        CollectionVO vo = new CollectionVO();
        vo.setItemId(item.getId());
        vo.setName(item.getName());
        vo.setType(item.getType());
        vo.setImage(item.getImagePrefix());
        if (orders.isEmpty()) {
            vo.setMinPrice(null);
            vo.setStock(0L);
            vo.setListingCount(0);
        } else {
            vo.setMinPrice(orders.get(0).getPrice());
            vo.setStock(orders.stream().mapToLong(Order::getAmount).sum());
            vo.setListingCount(orders.size());
        }
        return vo;
    }

    private CollectionVO buildCollectionList(Item item) {
        CollectionVO vo = new CollectionVO();
        vo.setItemId(item.getId());
        vo.setName(item.getName());
        vo.setType(item.getType());
        vo.setImage(item.getImagePrefix());
        vo.setTraits(item.getTraitsJson());
        return vo;
    }

    private long computeTotal(List<Order> orders, long amount, Long buyerId) {
        long need = amount;
        long total = 0;
        for (Order o : orders) {
            if (buyerId != null && o.getSellerId().equals(buyerId)) {
                continue;
            }
            long take = Math.min(o.getAmount(), need);
            total += take * o.getPrice();
            need -= take;
            if (need == 0) {
                break;
            }
        }
        if (need > 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        return total;
    }

    private void creditAvailable(Long userId, Long itemId, long amount, long now) {
        UserAsset asset = userAssetMapper.selectByUserAndItem(userId, itemId);
        if (asset == null) {
            UserAsset a = new UserAsset();
            a.setUserId(userId);
            a.setItemId(itemId);
            a.setAvailable(amount);
            a.setFrozen(0L);
            a.setCreatedAt(now);
            a.setUpdatedAt(now);
            userAssetMapper.insert(a);
        } else {
            userAssetMapper.addAvailable(userId, itemId, amount, now);
        }
    }

    private void writeBalanceLog(Long userId, Long itemId, long change, long now, int bizType, Long refId) {
        UserAsset asset = userAssetMapper.selectByUserAndItem(userId, itemId);
        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setItemId(itemId);
        log.setChangeAmount(change);
        log.setBalanceAfter(asset == null ? 0L : asset.getAvailable());
        log.setBizType(bizType);
        log.setRefId(refId);
        log.setCreatedAt(now);
        balanceLogMapper.insert(log);
    }
}
