package com.mooar.market.mapper;

import com.mooar.market.service.ItemTraitService;

import java.util.List;
import java.util.Map;

public class NftSqlProvider {

    @SuppressWarnings("unchecked")
    public String searchByTraits(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("SELECT * FROM nft WHERE item_id = #{itemId}");
        SqlCondBuilder.append(sb, (List<ItemTraitService.TraitCond>) params.get("conds"), null);
        sb.append(" ORDER BY id DESC LIMIT #{offset}, #{limit}");
        return sb.toString();
    }
}
