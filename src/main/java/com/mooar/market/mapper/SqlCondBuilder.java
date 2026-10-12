package com.mooar.market.mapper;

import com.mooar.market.service.ItemTraitService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 trait 条件拼成 SQL 片段：同列等值条件取 OR（多选），不同列与区间条件取 AND。
 * 列名走槽位白名单、操作符走白名单、值全部参数化，避免注入。
 */
public final class SqlCondBuilder {

    private static final List<String> OPS = List.of("=", ">=", "<=", ">", "<");

    private SqlCondBuilder() {
    }

    /**
     * @param alias 表别名，可为空；非空时列会写成 alias.column
     */
    public static void append(StringBuilder sb, List<ItemTraitService.TraitCond> conds, String alias) {
        if (conds == null || conds.isEmpty()) {
            return;
        }
        String prefix = alias == null || alias.isBlank() ? "" : alias + ".";
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
                others.add(prefix + c.getColumn() + " " + c.getOp() + " #{conds[" + i + "].value}");
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
                sb.append(prefix).append(e.getKey())
                        .append(" = #{conds[").append(idx.get(i)).append("].value}");
            }
            sb.append(")");
        }
    }
}
