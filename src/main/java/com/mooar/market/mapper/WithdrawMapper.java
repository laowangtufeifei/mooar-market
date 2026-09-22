package com.mooar.market.mapper;

import com.mooar.market.entity.Withdraw;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface WithdrawMapper {

    @Select("SELECT * FROM withdraw WHERE id = #{id}")
    Withdraw selectById(@Param("id") Long id);

    @Select("SELECT * FROM withdraw WHERE state = #{state} ORDER BY id ASC LIMIT #{limit}")
    List<Withdraw> selectByState(@Param("state") Integer state, @Param("limit") int limit);

    @Insert("INSERT INTO withdraw (user_id, item_id, nft_id, amount, fee, to_addr, tx_hash, state, retry_count, nonce, err_msg, created_at, updated_at) " +
            "VALUES (#{userId}, #{itemId}, #{nftId}, #{amount}, #{fee}, #{toAddr}, #{txHash}, #{state}, #{retryCount}, #{nonce}, #{errMsg}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Withdraw withdraw);

    @Update("UPDATE withdraw SET state = #{toState}, tx_hash = #{txHash}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND state = #{fromState}")
    int updateTxHash(@Param("id") Long id, @Param("fromState") Integer fromState,
                     @Param("toState") Integer toState, @Param("txHash") String txHash,
                     @Param("updatedAt") Long updatedAt);

    @Update("UPDATE withdraw SET state = #{toState}, retry_count = #{retryCount}, err_msg = #{errMsg}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int updateFail(@Param("id") Long id, @Param("toState") Integer toState,
                   @Param("retryCount") Integer retryCount, @Param("errMsg") String errMsg,
                   @Param("updatedAt") Long updatedAt);
}
