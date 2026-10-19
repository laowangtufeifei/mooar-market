package com.mooar.market.service;

import com.mooar.market.common.ItemType;
import com.mooar.market.common.NftState;
import com.mooar.market.common.OrderState;
import com.mooar.market.dto.AssetVO;
import com.mooar.market.dto.NftAssetVO;
import com.mooar.market.dto.OrderAssetVO;
import com.mooar.market.entity.Item;
import com.mooar.market.entity.Nft;
import com.mooar.market.entity.Order;
import com.mooar.market.entity.UserAsset;
import com.mooar.market.mapper.NftMapper;
import com.mooar.market.mapper.OrderMapper;
import com.mooar.market.mapper.UserAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final ItemCache itemCache;
    private final UserAssetMapper userAssetMapper;
    private final NftMapper nftMapper;
    private final OrderMapper orderMapper;

    /**
     * 查询用户全部资产（同质化余额 + NFT），NFT 按状态返回明细
     */
    public List<AssetVO> getAssets(Long userId) {
        List<Item> items = itemCache.getAll();
        Map<Long, UserAsset> assetMap = userAssetMapper.selectByUser(userId).stream()
                .collect(Collectors.toMap(UserAsset::getItemId, a -> a, (x, y) -> x));
        Map<Long, List<Nft>> nftMap = nftMapper.selectByOwner(userId).stream()
                .filter(n -> n.getItemId() != null)
                .collect(Collectors.groupingBy(Nft::getItemId));
        Map<Long, List<Order>> orderMap = orderMapper.selectBySeller(userId).stream()
                .filter(o -> Integer.valueOf(OrderState.ACTIVE).equals(o.getState()))
                .collect(Collectors.groupingBy(Order::getItemId));

        List<AssetVO> result = new ArrayList<>();
        for (Item item : items) {
            AssetVO vo = new AssetVO();
            vo.setItemId(item.getId());
            vo.setName(item.getName());
            vo.setType(item.getType());
            vo.setImage(item.getImagePrefix());
            List<Order> orders = orderMap.getOrDefault(item.getId(), Collections.emptyList());
            fillOrderList(vo, orders);
            if (item.getType() == ItemType.ERC721) {
                fillNft(vo, nftMap.getOrDefault(item.getId(), Collections.emptyList()));
            } else {
                UserAsset a = assetMap.get(item.getId());
                long available = a == null ? 0L : a.getAvailable();
                long frozen = a == null ? 0L : a.getFrozen();
                long listing = 0;
                for (Order order : orders) {
                    listing += order.getAmount() == null ? 0L : order.getAmount();
                }
                vo.setAvailable(available);
                vo.setFrozen(frozen);
                vo.setListing(listing);
                // 1155 的 frozen = 挂单冻结 + 提现冻结，减掉挂单剩下的是提现中的
                vo.setWithdrawing(Math.max(0L, frozen - listing));
            }
            result.add(vo);
        }
        return result;
    }

    private void fillOrderList(AssetVO vo, List<Order> orders) {
        for (Order order : orders) {
            OrderAssetVO o = new OrderAssetVO();
            o.setOrderId(order.getId());
            o.setPrice(order.getPrice());
            o.setAmount(order.getAmount());
            vo.getOrderList().add(o);
        }
    }

    private void fillNft(AssetVO vo, List<Nft> nfts) {
        Map<Long, Order> orderMap = loadOrders(nfts);
        long normal = 0;
        long listing = 0;
        long withdrawing = 0;
        for (Nft nft : nfts) {
            int state = nft.getState() == null ? NftState.NORMAL : nft.getState();
            NftAssetVO n = new NftAssetVO();
            n.setNftId(nft.getId());
            n.setTokenId(nft.getTokenId());
            n.setImage(nft.getImage());
            n.setMetadata(nft.getMetadata());
            n.setState(state);
            Order order = nft.getOrderId() == null ? null : orderMap.get(nft.getOrderId());
            if (order != null) {
                n.setOrderId(order.getId());
                n.setPrice(order.getPrice());
            }
            vo.getNftList().add(n);
            if (state == NftState.LISTING) {
                listing++;
            } else if (state == NftState.WITHDRAWING) {
                withdrawing++;
            } else if (state == NftState.NORMAL) {
                normal++;
                vo.getNftIds().add(nft.getTokenId());
            }
        }
        vo.setAvailable(normal);
        vo.setListing(listing);
        vo.setWithdrawing(withdrawing);
        vo.setFrozen(listing + withdrawing);
    }

    private Map<Long, Order> loadOrders(List<Nft> nfts) {
        List<Long> ids = new ArrayList<>();
        for (Nft nft : nfts) {
            if (nft.getOrderId() != null && nft.getOrderId() > 0) {
                ids.add(nft.getOrderId());
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Order::getId, o -> o, (x, y) -> x));
    }
}
