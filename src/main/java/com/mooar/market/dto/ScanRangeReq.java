package com.mooar.market.dto;

import lombok.Data;

@Data
public class ScanRangeReq {
    /** 起始块号（必填） */
    private Long fromBlock;
    /** 结束块号，不传等于只扫 fromBlock 这一个块 */
    private Long toBlock;
}
