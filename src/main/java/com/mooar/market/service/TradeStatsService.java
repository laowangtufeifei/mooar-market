package com.mooar.market.service;

import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.config.MarketProperties;
import com.mooar.market.dto.TradeStatItemVO;
import com.mooar.market.dto.TradeStatPoint;
import com.mooar.market.dto.TradeStatRow;
import com.mooar.market.dto.TradeStatsVO;
import com.mooar.market.entity.Item;
import com.mooar.market.mapper.TradeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台成交统计：按 item 汇总成交笔数/成交量/成交额，支持按天或按月分桶
 */
@Service
@RequiredArgsConstructor
public class TradeStatsService {

    public static final String DAY = "day";
    public static final String MONTH = "month";

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int DEFAULT_DAYS = 30;
    private static final int DEFAULT_MONTHS = 12;
    private static final int MAX_PERIODS = 400;

    private final TradeMapper tradeMapper;
    private final ItemCache itemCache;
    private final MarketProperties properties;

    /**
     * @param granularity day / month，其他值按 day
     * @param from        起始日期 yyyy-MM-dd，不传按默认区间
     * @param to          结束日期 yyyy-MM-dd（含），不传取今天
     * @param itemId      只看某个 item，不传返回全部
     */
    public TradeStatsVO stats(String granularity, String from, String to, Long itemId) {
        boolean byMonth = MONTH.equalsIgnoreCase(granularity);
        ZoneId zone = zone();
        LocalDate end = parseDate(to, LocalDate.now(zone));
        LocalDate start;
        if (from == null || from.isBlank()) {
            start = byMonth ? end.withDayOfMonth(1).minusMonths(DEFAULT_MONTHS - 1L) : end.minusDays(DEFAULT_DAYS - 1L);
        } else {
            start = parseDate(from, null);
        }
        if (byMonth) {
            start = start.withDayOfMonth(1);
            end = end.withDayOfMonth(1);
        }
        if (start.isAfter(end)) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        List<String> periods = buildPeriods(start, end, byMonth);

        long fromMs = start.atStartOfDay(zone).toInstant().toEpochMilli();
        long toMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long tzOffsetMs = zone.getRules().getOffset(Instant.ofEpochMilli(fromMs)).getTotalSeconds() * 1000L;

        Map<String, Object> params = new HashMap<>();
        params.put("fromMs", fromMs);
        params.put("toMs", toMs);
        params.put("tzOffsetMs", tzOffsetMs);
        if (itemId != null) {
            params.put("itemId", itemId);
        }
        List<TradeStatRow> rows = tradeMapper.selectStats(params);

        Map<Long, Map<String, TradeStatPoint>> pointsByItem = new LinkedHashMap<>();
        Map<Long, long[]> totalsByItem = new LinkedHashMap<>();
        for (TradeStatRow row : rows) {
            String label = periodLabel(row.getDayIndex(), byMonth);
            TradeStatPoint point = pointsByItem
                    .computeIfAbsent(row.getItemId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(label, k -> newPoint(label));
            point.setCount(point.getCount() + row.getTradeCount());
            point.setAmount(point.getAmount() + row.getTradeAmount());
            point.setValue(point.getValue() + row.getTradeValue());
            long[] total = totalsByItem.computeIfAbsent(row.getItemId(), k -> new long[3]);
            total[0] += row.getTradeCount();
            total[1] += row.getTradeAmount();
            total[2] += row.getTradeValue();
        }

        TradeStatsVO vo = new TradeStatsVO();
        vo.setGranularity(byMonth ? MONTH : DAY);
        vo.setFrom(start.format(DAY_FMT));
        vo.setTo(end.format(DAY_FMT));
        vo.setPeriods(periods);
        for (Item item : targetItems(itemId)) {
            Map<String, TradeStatPoint> byPeriod = pointsByItem.get(item.getId());
            long[] total = totalsByItem.getOrDefault(item.getId(), new long[3]);
            TradeStatItemVO itemVO = new TradeStatItemVO();
            itemVO.setItemId(item.getId());
            itemVO.setItemName(item.getName());
            itemVO.setType(item.getType());
            itemVO.setTradeCount(total[0]);
            itemVO.setTradeAmount(total[1]);
            itemVO.setTradeValue(total[2]);
            if (byPeriod != null) {
                for (String period : periods) {
                    TradeStatPoint point = byPeriod.get(period);
                    if (point != null) {
                        itemVO.getPoints().add(point);
                    }
                }
            }
            vo.getItems().add(itemVO);
        }
        vo.getItems().sort(Comparator.comparingLong(TradeStatItemVO::getTradeCount).reversed()
                .thenComparing(TradeStatItemVO::getItemId));
        return vo;
    }

    private List<Item> targetItems(Long itemId) {
        if (itemId == null) {
            return itemCache.getAll();
        }
        Item item = itemCache.getById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.ITEM_NOT_FOUND);
        }
        return Collections.singletonList(item);
    }

    private String periodLabel(Long dayIndex, boolean byMonth) {
        LocalDate date = LocalDate.ofEpochDay(dayIndex);
        return byMonth ? date.format(MONTH_FMT) : date.format(DAY_FMT);
    }

    private TradeStatPoint newPoint(String period) {
        TradeStatPoint point = new TradeStatPoint();
        point.setPeriod(period);
        point.setCount(0L);
        point.setAmount(0L);
        point.setValue(0L);
        return point;
    }

    private List<String> buildPeriods(LocalDate start, LocalDate end, boolean byMonth) {
        List<String> periods = new ArrayList<>();
        DateTimeFormatter fmt = byMonth ? MONTH_FMT : DAY_FMT;
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            periods.add(cursor.format(fmt));
            if (periods.size() > MAX_PERIODS) {
                throw new BizException(ErrorCode.PARAM_ERROR);
            }
            cursor = byMonth ? cursor.plusMonths(1) : cursor.plusDays(1);
        }
        return periods;
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim(), DAY_FMT);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(properties.getAdmin().getStatsZone());
        } catch (Exception e) {
            return ZoneId.of("Asia/Shanghai");
        }
    }
}
