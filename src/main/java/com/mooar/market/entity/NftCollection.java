package com.mooar.market.entity;

import lombok.Data;

/**
 * 系列（collection）：一个合约一个，承载系列级的名称/封面/描述等展示信息，
 * 与 tokenId 级的 item 数据分离
 */
@Data
public class NftCollection {
    private Long id;
    private String contract;
    private String name;
    private String cover;
    private String description;
    private Integer sort;
    private Integer state;
    private Long createdAt;
    private Long updatedAt;
}
