package com.mooar.market.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssetVO {
    private Long itemId;
    private String name;
    private Integer type;
    private String image;
    private Long available;
    private Long frozen;
    /** 挂单中数量（721：挂单中的 nft 数；1155：等于 frozen） */
    private Long listing;
    /** 提现中数量（721 专属） */
    private Long withdrawing;
    private List<Long> nftIds = new ArrayList<>();
    /** 721 资产明细（含状态） */
    private List<NftAssetVO> nftList = new ArrayList<>();
}
