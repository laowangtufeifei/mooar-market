package com.mooar.market.controller;

import com.mooar.market.common.Result;
import com.mooar.market.dto.TradeStatsVO;
import com.mooar.market.service.TradeStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final TradeStatsService tradeStatsService;

    /**
     * 各 item 的成交统计，支持按天/按月
     */
    @GetMapping("/trade")
    public Result<TradeStatsVO> trade(@RequestParam(defaultValue = TradeStatsService.DAY) String granularity,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestParam(required = false) Long itemId) {
        return Result.ok(tradeStatsService.stats(granularity, from, to, itemId));
    }
}
