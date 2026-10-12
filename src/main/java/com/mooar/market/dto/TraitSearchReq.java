package com.mooar.market.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按属性筛选在售挂单的请求体
 *
 * itemId 支持道具 id 或合约地址；
 * traits 形如 {"Rarity":["Legendary","Common"], "Level":["10-30"], "Character":["Mooarty"]}，
 * 属性可以只传一部分；同一个属性多个值之间是 OR，不同属性之间是 AND
 */
@Data
public class TraitSearchReq {

    private String itemId;
    /**
     * 链上 tokenId 模糊搜索（包含），如传 "123" 可命中 12345、12123；仅 721 有效
     */
    private String tokenId;
    private Map<String, List<String>> traits = new LinkedHashMap<>();
    private int page = 1;
    private int pageSize = 20;
    /** 排序方向：asc（默认，价格由小到大）/ desc */
    private String sortOrder = "asc";
}
