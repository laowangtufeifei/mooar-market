package com.mooar.market.mapper;

import com.mooar.market.service.ItemTraitService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NftSqlProvider {

    private static final List<String> OPS = List.of("=", ">=", "<=", ">", "<");

    /**
     * 拼筛选 SQL：同列多个等值条件取 OR（多选），不同列与区间条件取 AND
     */
    @SuppressWarnings("unchecked")
    public String searchByTraits(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("SELECT * FROM nft WHERE item_id = #{itemId}");
        List<ItemTraitService.TraitCond> conds = (List<ItemTraitService.TraitCond>) params.get("conds");
        if (conds != null) {
            Map<String, List<Integer>> eqGroup = new LinkedHashMap<>();
            List<String> others = new ArrayList<>();
            for (int i = 0; i < conds.size(); i++) {
                ItemTraitService.TraitCond c = conds.get(i);
                if (!ItemTraitService.SLOT_WHITELIST.contains(c.getColumn()) || !OPS.contains(c.getOp())) {
                    continue;
                }
                if ("=".equals(c.getOp())) {
                    eqGroup.computeIfAbsent(c.getColumn(), k -> new ArrayList<>()).add(i);
                } else {
                    others.add(c.getColumn() + " " + c.getOp() + " #{conds[" + i + "].value}");
                }
            }
            for (String other : others) {
                sb.append(" AND ").append(other);
            }
            for (Map.Entry<String, List<Integer>> e : eqGroup.entrySet()) {
                sb.append(" AND (");
                List<Integer> idx = e.getValue();
                for (int i = 0; i < idx.size(); i++) {
                    if (i > 0) {
                        sb.append(" OR ");
                    }
                    sb.append(e.getKey()).append(" = #{conds[").append(idx.get(i)).append("].value}");
                }
                sb.append(")");
            }
        }
        sb.append(" ORDER BY id DESC LIMIT #{offset}, #{limit}");
        return sb.toString();
    }
}
