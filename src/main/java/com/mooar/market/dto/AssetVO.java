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
    /** 挂单中数量（= orderList 里各订单 amount 之和） */
    private Long listing;
    /** 提现中数量（1155：frozen - listing；721：明细里 WITHDRAWING 的 nft 数） */
    private Long withdrawing;
    private List<Long> nftIds = new ArrayList<>();
    /** 721 资产明细（含状态） */
    private List<NftAssetVO> nftList = new ArrayList<>();
    /** 在售挂单明细：1155 一个订单可挂多个、同一道具可有多个不同价格的订单 */
    private List<OrderAssetVO> orderList = new ArrayList<>();
}
