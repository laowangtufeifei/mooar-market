package com.mooar.market.mapper;

import com.mooar.market.entity.Trade;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface TradeMapper {

    @Insert("INSERT INTO trade (order_id, item_id, nft_id, amount, price, total, fee, seller_id, buyer_id, created_at) " +
            "VALUES (#{orderId}, #{itemId}, #{nftId}, #{amount}, #{price}, #{total}, #{fee}, #{sellerId}, #{buyerId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Trade trade);
}
