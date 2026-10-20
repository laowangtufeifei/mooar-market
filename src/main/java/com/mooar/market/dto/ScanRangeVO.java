package com.mooar.market.dto;

import lombok.Data;

@Data
public class ScanRangeVO {
    private Long fromBlock;
    private Long toBlock;
    /** 本次新处理的转账条数（已按 tx_hash + log_index 去重，重复扫不会重复入账） */
    private Integer processed;
}
