package com.mooar.market.service;

import com.mooar.market.common.ItemType;

import com.mooar.market.dto.AssetVO;
import com.mooar.market.entity.Item;
import com.mooar.market.entity.Nft;
import com.mooar.market.entity.UserAsset;
import com.mooar.market.mapper.NftMapper;
import com.mooar.market.mapper.UserAssetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final ItemCache itemCache;
    private final UserAssetMapper userAssetMapper;
    private final NftMapper nftMapper;

    /**
     * 查询用户全部资产（同质化余额 + badge NFT）
     */
    public List<AssetVO> getAssets(Long userId) {
        List<Item> items = itemCache.getAll();
        Map<Long, UserAsset> assetMap = userAssetMapper.selectByUser(userId).stream()
                .collect(Collectors.toMap(UserAsset::getItemId, a -> a, (x, y) -> x));
        Map<Long, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getId, i -> i));

        List<AssetVO> result = new ArrayList<>();
        List<Nft> nfts = null;
        for (Item item : items) {
            AssetVO vo = new AssetVO();
            vo.setItemId(item.getId());
            vo.setName(item.getName());
            vo.setType(item.getType());
            vo.setImage(item.getImagePrefix() + item.getId());
            if (item.getType() == ItemType.ERC721) {
                if (nfts == null) {
                    nfts = nftMapper.selectByOwner(userId);
                }
                vo.setAvailable((long) nfts.size());
                vo.setFrozen(0L);
                vo.setNftIds(nfts.stream().map(Nft::getTokenId).collect(Collectors.toList()));
            } else {
                UserAsset a = assetMap.get(item.getId());
                vo.setAvailable(a == null ? 0L : a.getAvailable());
                vo.setFrozen(a == null ? 0L : a.getFrozen());
            }
            result.add(vo);
        }
        return result;
    }
}
