package com.mooar.market.common;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * trait 定义：属性名、槽位列、类型（enum/number）、枚举编码表或数值范围
 */
@Data
public class TraitDef {

    public static final String TYPE_ENUM = "enum";
    public static final String TYPE_NUMBER = "number";

    /** 兜底字符串槽位 */
    public static final String SLOT_STR = "str";

    private String name;
    private String slot;
    private String type;
    private Map<String, Integer> values;
    private Integer min;
    private Integer max;

    /**
     * 枚举 trait：按给定顺序编码 1..n，顺序即为编码，一旦确定不可调整
     */
    public static TraitDef enumOf(String name, String slot, String... values) {
        TraitDef def = new TraitDef();
        def.setName(name);
        def.setSlot(slot);
        def.setType(TYPE_ENUM);
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            map.put(values[i], i + 1);
        }
        def.setValues(map);
        return def;
    }

    /**
     * 数值 trait：值本身可直接入库，min/max 仅用于前端渲染区间
     */
    public static TraitDef number(String name, String slot, int min, int max) {
        TraitDef def = new TraitDef();
        def.setName(name);
        def.setSlot(slot);
        def.setType(TYPE_NUMBER);
        def.setMin(min);
        def.setMax(max);
        return def;
    }
}
