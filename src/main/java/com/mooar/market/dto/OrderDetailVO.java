package com.mooar.market.dto;

import lombok.Data;

/**
 * 挂单详情：列表页点进去才调，带上属性、道具信息与时间
 */
@Data
public class OrderDetailVO {
    private Long orderId;
    private Long itemId;
    private String itemName;
    private String itemImage;
    private Long sellerId;
    /** 721 的 nft id，1155 为 0 */
    private Long nftId;
    /** 721 的链上 tokenId，1155 为 0 */
    private Long tokenId;
    /** 721 取 nft 图片，1155 取道具图片 */
    private String image;
    /** 721 的属性 JSON（attributes 数组），1155 为 null */
    private String metadata;
    /** 挂单数量，721 恒为 1 */
    private Long amount;
    private Long price;
    /** 0 在售 1 已售出 2 已取消 */
    private Integer state;
    private Long createdAt;
    private Long updatedAt;
}
