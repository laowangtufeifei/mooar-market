package com.mooar.market.dto;

import lombok.Data;

@Data
public class CollectionVO {
    /** collection 标识：合约地址。1155 同一合约下多个 tokenId 归为同一个 collection */
    private String collectionId;
    /** 代表 item（同合约下 id 最小的那行），兼容按 itemId 的老调用 */
    private Long itemId;
    private String name;
    private Integer type;
    private String image;
    private String traits;
    /** 该 collection 下的 item 个数（1155 即 tokenId 个数；721 为 1） */
    private Integer itemCount;
    private Long minPrice;
    private Long stock;
    private Integer listingCount;
}
