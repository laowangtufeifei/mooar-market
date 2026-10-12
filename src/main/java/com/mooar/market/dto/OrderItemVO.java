package com.mooar.market.dto;

import lombok.Data;

/**
 * 在售挂单列表项（列表页只给基本信息，属性等详情走 order detail 接口）
 */
@Data
public class OrderItemVO {
    private Long orderId;
    private Long itemId;
    private Long sellerId;
    /** 721 的 nft id，1155 为 0 */
    private Long nftId;
    /** 721 的链上 tokenId，1155 为 0 */
    private Long tokenId;
    /** 721 取 nft 图片，1155 取道具图片 */
    private String image;
    /** 挂单数量，721 恒为 1 */
    private Long amount;
    private Long price;
}
