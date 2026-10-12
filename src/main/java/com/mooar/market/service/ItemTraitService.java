package com.mooar.market.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mooar.market.common.TraitDef;
import com.mooar.market.common.TraitDict;
import com.mooar.market.entity.Item;
import com.mooar.market.entity.Nft;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemTraitService {

    private static final Logger log = LoggerFactory.getLogger(ItemTraitService.class);

    /** 允许作为筛选列的槽位白名单（防 SQL 注入） */
    public static final Set<String> SLOT_WHITELIST = Set.of(
            "level", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8", "v9",
            "v10", "v11", "v12", "v13", "v14", "v15");

    private final ItemCache itemCache;
    private final ObjectMapper objectMapper;

    private final Cache<Long, Map<String, TraitDef>> defCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(256)
            .build();

    /**
     * 筛选条件：翻译后的单条 SQL 条件（列 + 比较符 + 值）
     */
    @Data
    public static class TraitCond {
        private String column;
        private String op;
        private Integer value;

        public TraitCond(String column, String op, Integer value) {
            this.column = column;
            this.op = op;
            this.value = value;
        }
    }

    /**
     * 某 collection 的 trait 定义（属性名 -> 定义）
     */
    public Map<String, TraitDef> getTraitDefs(Long itemId) {
        return defCache.get(itemId, this::load);
    }

    /**
     * 按定义解析 metadata，把属性值编码写入 nft 槽位
     */
    public void applyTraits(Nft nft, Long itemId, String metadata) {
        clearSlots(nft);
        Map<String, TraitDef> defs = getTraitDefs(itemId);
        if (defs.isEmpty() || metadata == null || metadata.isBlank()) {
            return;
        }
        try {
            JsonNode attributes = objectMapper.readTree(metadata).path("attributes");
            if (!attributes.isArray()) {
                return;
            }
            for (JsonNode attr : attributes) {
                String trait = attr.path("trait_type").asText("");
                String raw = attr.path("value").asText("");
                if (trait.isBlank() || raw.isBlank()) {
                    continue;
                }
                applyOne(nft, itemId, defs, trait, raw);
            }
        } catch (Exception e) {
            log.warn("解析 metadata 失败 item={} err={}", itemId, e.getMessage());
        }
    }

    /**
     * 把前端筛选串翻译成 SQL 条件，如 "Rarity:Legendary,Level:10-30"
     */
    public List<TraitCond> translateFilters(Long itemId, String filters) {
        List<TraitCond> conds = new ArrayList<>();
        if (filters == null || filters.isBlank()) {
            return conds;
        }
        Map<String, TraitDef> defs = getTraitDefs(itemId);
        for (String part : filters.split(",")) {
            int idx = part.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            addCond(conds, defs, part.substring(0, idx).trim(), part.substring(idx + 1).trim());
        }
        return conds;
    }

    /**
     * 把前端 JSON 形式的筛选条件翻译成 SQL 条件，属性可以只传一部分
     */
    public List<TraitCond> translateTraits(Long itemId, Map<String, List<String>> traits) {
        List<TraitCond> conds = new ArrayList<>();
        if (traits == null || traits.isEmpty()) {
            return conds;
        }
        Map<String, TraitDef> defs = getTraitDefs(itemId);
        for (Map.Entry<String, List<String>> entry : traits.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            for (String val : entry.getValue()) {
                if (val == null || val.isBlank()) {
                    continue;
                }
                addCond(conds, defs, entry.getKey().trim(), val.trim());
            }
        }
        return conds;
    }

    private void addCond(List<TraitCond> conds, Map<String, TraitDef> defs, String name, String val) {
        TraitDef def = defs.get(name);
        if (def != null && TraitDef.TYPE_NUMBER.equals(def.getType())) {
            if (SLOT_WHITELIST.contains(def.getSlot())) {
                addNumberCond(conds, def.getSlot(), val);
            }
            return;
        }
        TraitDef owner = def != null && def.getValues() != null && def.getValues().containsKey(val)
                ? def : findByValue(defs, val);
        if (owner == null || owner.getValues() == null || !SLOT_WHITELIST.contains(owner.getSlot())) {
            return;
        }
        Integer code = owner.getValues().get(val);
        if (code != null) {
            conds.add(new TraitCond(owner.getSlot(), "=", code));
        }
    }

    private void applyOne(Nft nft, Long itemId, Map<String, TraitDef> defs, String trait, String raw) {
        TraitDef def = defs.get(trait);
        if (def != null && TraitDef.SLOT_STR.equals(def.getSlot())) {
            nft.setStr(raw);
            return;
        }
        Integer code = def == null ? null : encode(def, raw);
        if (code != null) {
            setSlot(nft, def.getSlot(), code);
            return;
        }
        TraitDef alt = findByValue(defs, raw);
        if (alt == null) {
            log.warn("trait 未匹配 item={} trait={} value={}", itemId, trait, raw);
            return;
        }
        Integer altCode = encode(alt, raw);
        if (altCode != null) {
            setSlot(nft, alt.getSlot(), altCode);
            log.warn("trait 按值兜底 item={} trait={} value={} -> slot={}", itemId, trait, raw, alt.getSlot());
        }
    }

    private void clearSlots(Nft nft) {
        nft.setLevel(0);
        for (int i = 1; i <= 15; i++) {
            setSlot(nft, "v" + i, 0);
        }
        nft.setStr("");
    }

    private Map<String, TraitDef> load(Long itemId) {
        Item item = itemCache.getById(itemId);
        if (item == null) {
            return Collections.emptyMap();
        }
        Map<String, TraitDef> defs = TraitDict.get(item.getContract());
        if (!defs.isEmpty()) {
            return defs;
        }
        return parseTraitsJson(item.getTraitsJson());
    }

    private Map<String, TraitDef> parseTraitsJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, TraitDef> map = new LinkedHashMap<>();
            for (JsonNode node : objectMapper.readTree(json).path("traits")) {
                TraitDef def = new TraitDef();
                def.setName(node.path("name").asText());
                def.setSlot(node.path("slot").asText());
                def.setType(node.path("type").asText(TraitDef.TYPE_ENUM));
                if (TraitDef.TYPE_NUMBER.equals(def.getType())) {
                    def.setMin(node.path("min").asInt());
                    def.setMax(node.path("max").asInt());
                } else {
                    List<Map.Entry<String, Integer>> entries = new ArrayList<>();
                    node.path("values").fields()
                            .forEachRemaining(e -> entries.add(Map.entry(e.getKey(), e.getValue().asInt())));
                    entries.sort(Map.Entry.comparingByValue());
                    Map<String, Integer> values = new LinkedHashMap<>();
                    entries.forEach(e -> values.put(e.getKey(), e.getValue()));
                    def.setValues(values);
                }
                if (!def.getName().isBlank() && !def.getSlot().isBlank()) {
                    map.put(def.getName(), def);
                }
            }
            return map;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private TraitDef findByValue(Map<String, TraitDef> defs, String raw) {
        for (TraitDef def : defs.values()) {
            if (def.getValues() != null && def.getValues().containsKey(raw)) {
                return def;
            }
        }
        return null;
    }

    private void addNumberCond(List<TraitCond> conds, String column, String val) {
        int dash = val.indexOf('-');
        if (dash > 0) {
            Integer min = parseIntSafe(val.substring(0, dash));
            Integer max = parseIntSafe(val.substring(dash + 1));
            if (min != null) {
                conds.add(new TraitCond(column, ">=", min));
            }
            if (max != null) {
                conds.add(new TraitCond(column, "<=", max));
            }
        } else {
            Integer v = parseIntSafe(val);
            if (v != null) {
                conds.add(new TraitCond(column, "=", v));
            }
        }
    }

    private Integer encode(TraitDef def, String raw) {
        if (TraitDef.TYPE_NUMBER.equals(def.getType())) {
            return parseIntSafe(raw);
        }
        return def.getValues() == null ? null : def.getValues().get(raw);
    }

    private Integer parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void setSlot(Nft nft, String slot, int code) {
        switch (slot) {
            case "level" -> nft.setLevel(code);
            case "v1" -> nft.setV1(code);
            case "v2" -> nft.setV2(code);
            case "v3" -> nft.setV3(code);
            case "v4" -> nft.setV4(code);
            case "v5" -> nft.setV5(code);
            case "v6" -> nft.setV6(code);
            case "v7" -> nft.setV7(code);
            case "v8" -> nft.setV8(code);
            case "v9" -> nft.setV9(code);
            case "v10" -> nft.setV10(code);
            case "v11" -> nft.setV11(code);
            case "v12" -> nft.setV12(code);
            case "v13" -> nft.setV13(code);
            case "v14" -> nft.setV14(code);
            case "v15" -> nft.setV15(code);
            default -> {
            }
        }
    }
}
