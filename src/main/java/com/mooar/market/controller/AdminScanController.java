package com.mooar.market.controller;

import com.mooar.market.chain.DepositScanner;
import com.mooar.market.common.BizException;
import com.mooar.market.common.ErrorCode;
import com.mooar.market.common.Result;
import com.mooar.market.dto.ScanRangeReq;
import com.mooar.market.dto.ScanRangeVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手动补扫：用于漏扫/改配置后回补历史块，不影响定时任务的扫块游标
 */
@RestController
@RequestMapping("/admin/scan")
@RequiredArgsConstructor
public class AdminScanController {

    private static final Logger scanLog = LoggerFactory.getLogger("ORDER_LOG");
    /** 单次补扫的块数上限，防止一个请求跑太久 */
    private static final long MAX_RANGE = 5000;

    private final DepositScanner depositScanner;

    /**
     * 扫指定块区间，只补扫不推进游标；重复扫同一区间不会重复入账
     */
    @PostMapping
    public Result<ScanRangeVO> scanRange(@RequestBody ScanRangeReq req) throws Exception {
        if (req.getFromBlock() == null || req.getFromBlock() <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        long from = req.getFromBlock();
        long to = req.getToBlock() == null ? from : req.getToBlock();
        if (to < from) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        if (to - from + 1 > MAX_RANGE) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        int processed = depositScanner.scanRange(from, to);
        scanLog.info("手动补扫 from={} to={} 新处理 {} 条", from, to, processed);
        ScanRangeVO vo = new ScanRangeVO();
        vo.setFromBlock(from);
        vo.setToBlock(to);
        vo.setProcessed(processed);
        return Result.ok(vo);
    }
}
