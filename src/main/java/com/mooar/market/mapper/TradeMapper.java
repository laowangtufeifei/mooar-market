package com.mooar.market.mapper;

import com.mooar.market.dto.TradeStatRow;
import com.mooar.market.entity.Trade;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface TradeMapper {

    @Insert("INSERT INTO trade (order_id, item_id, nft_id, amount, price, total, fee, seller_id, buyer_id, created_at) " +
            "VALUES (#{orderId}, #{itemId}, #{nftId}, #{amount}, #{price}, #{total}, #{fee}, #{sellerId}, #{buyerId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Trade trade);

    /**
     * 按 item + 天聚合成交量，天按传入的时区偏移折算，避免依赖数据库会话时区
     */
    @Select("<script>SELECT item_id AS itemId, " +
            "((created_at + #{tzOffsetMs}) DIV 86400000) AS dayIndex, " +
            "COUNT(*) AS tradeCount, IFNULL(SUM(amount), 0) AS tradeAmount, IFNULL(SUM(total), 0) AS tradeValue " +
            "FROM trade WHERE created_at &gt;= #{fromMs} AND created_at &lt; #{toMs} " +
            "<if test='itemId != null'>AND item_id = #{itemId} </if>" +
            "GROUP BY item_id, dayIndex ORDER BY item_id, dayIndex</script>")
    List<TradeStatRow> selectStats(Map<String, Object> params);
}
