package com.mooar.market.mapper;

import com.mooar.market.entity.DepositLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface DepositLogMapper {

    @Insert("INSERT IGNORE INTO deposit_log (tx_hash, log_index, block_number, block_time, from_addr, to_addr, " +
            "contract, token_id, amount, json_data, created_at) " +
            "VALUES (#{txHash}, #{logIndex}, #{blockNumber}, #{blockTime}, #{fromAddr}, #{toAddr}, " +
            "#{contract}, #{tokenId}, #{amount}, #{jsonData}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnore(DepositLog log);
}
