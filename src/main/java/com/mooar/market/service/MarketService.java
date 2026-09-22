package com.mooar.market.service;

import com.mooar.market.common.ItemType;

import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.dto.CollectionVO;
import com.mooar.market.dto.NftOrderVO;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketService {

    private static final Logger orderLog = LoggerFactory.getLogger("ORDER_LOG");

    private final ItemCache itemCache;
    private final OrderMapper orderMapper;
    private final TradeMapper tradeMapper;
    private final UserAssetMapper userAssetMapper;
    private final NftMapper nftMapper;
    private final BalanceLogMapper balanceLogMapper;
    private final MarketProperties properties;

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
        order.setState(0);
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
            if (nft.getState() != 0) {
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
            if (nftMapper.updateState(nftId, 0, 1, order.getId(), now) == 0) {
                throw new BizException(ErrorCode.NFT_LOCKED);
            }
        } else {
            if (userAssetMapper.freeze(userId, itemId, amount, now) == 0) {
                throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
            }
        }
        orderLog.info("挂单 orderId={} seller={} item={} amount={} nftId={} price={}",
                order.getId(), userId, itemId, amount, nftId, price);
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
        if (order.getState() != 0) {
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

        if (orderMapper.updateState(orderId, 0, 1, buyerId, now) == 0) {
            throw new BizException(ErrorCode.ORDER_NOT_ACTIVE);
        }
        if (userAssetMapper.subAvailable(buyerId, gmtId, total, now) == 0) {
            throw new BizException(ErrorCode.ASSET_NOT_ENOUGH);
        }
        userAssetMapper.addAvailable(order.getSellerId(), gmtId, sellerIncome, now);

        if (item.getType() == ItemType.ERC721) {
            nftMapper.updateOwner(order.getNftId(), buyerId, now);
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
        if (order.getState() != 0) {
            throw new BizException(ErrorCode.ORDER_NOT_ACTIVE);
        }
        long now = System.currentTimeMillis();
        if (orderMapper.updateState(orderId, 0, 2, 0L, now) == 0) {
            throw new BizException(ErrorCode.ORDER_NOT_ACTIVE);
        }
        Item item = itemCache.getById(order.getItemId());
        if (item.getType() == ItemType.ERC721) {
            nftMapper.updateState(order.getNftId(), 1, 0, 0L, now);
        } else {
            userAssetMapper.unfreeze(userId, order.getItemId(), order.getAmount(), now);
        }
        orderLog.info("取消挂单 orderId={} seller={} item={}", orderId, userId, order.getItemId());
    }

    /**
     * 查询在售挂单列表
     */
    public List<Order> listActive(Long itemId, int page, int pageSize) {
        return orderMapper.selectActive(itemId, (page - 1) * pageSize, pageSize);
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

    /**
     * collection 详情：1155 返回聚合，721 返回 NFT 挂单列表
     */
    public Object collectionDetail(Long itemId) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (item.getType() == ItemType.ERC721) {
            List<Order> orders = orderMapper.selectActiveByItem(itemId);
            List<NftOrderVO> list = new ArrayList<>();
            for (Order o : orders) {
                Nft nft = nftMapper.selectById(o.getNftId());
                if (nft == null) {
                    continue;
                }
                NftOrderVO vo = new NftOrderVO();
                vo.setOrderId(o.getId());
                vo.setNftId(nft.getId());
                vo.setTokenId(nft.getTokenId());
                vo.setMetadata(nft.getMetadata());
                vo.setPrice(o.getPrice());
                list.add(vo);
            }
            return list;
        }
        return buildCollection(item);
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
                if (orderMapper.updateState(o.getId(), 0, 1, buyerId, now) == 0) {
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
    }

    private CollectionVO buildCollection(Item item) {
        List<Order> orders = orderMapper.selectActiveByItem(item.getId());
        CollectionVO vo = new CollectionVO();
        vo.setItemId(item.getId());
        vo.setName(item.getName());
        vo.setType(item.getType());
        vo.setImage(item.getImagePrefix() + item.getId());
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
