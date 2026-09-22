package com.mooar.market.mapper;

import com.mooar.market.entity.BalanceLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface BalanceLogMapper {

    @Insert("INSERT INTO balance_log (user_id, item_id, change_amount, balance_after, biz_type, ref_id, created_at) " +
            "VALUES (#{userId}, #{itemId}, #{changeAmount}, #{balanceAfter}, #{bizType}, #{refId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BalanceLog log);
}
