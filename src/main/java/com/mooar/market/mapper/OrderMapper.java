package com.mooar.market.mapper;

import com.mooar.market.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface OrderMapper {

    @Select("SELECT * FROM `order` WHERE id = #{id}")
    Order selectById(@Param("id") Long id);

    @Select("<script>SELECT * FROM `order` WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Order> selectByIds(@Param("ids") List<Long> ids);

    @Select("SELECT * FROM `order` WHERE state = 0 AND item_id = #{itemId} " +
            "ORDER BY price ASC, id ASC LIMIT #{offset}, #{limit}")
    List<Order> selectActive(@Param("itemId") Long itemId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM `order` WHERE state = 0 AND item_id = #{itemId} ORDER BY price ASC, id ASC")
    List<Order> selectActiveByItem(@Param("itemId") Long itemId);

    @Select("SELECT * FROM `order` WHERE seller_id = #{sellerId} ORDER BY id DESC")
    List<Order> selectBySeller(@Param("sellerId") Long sellerId);

    @Select("SELECT COUNT(*) FROM `order` WHERE nft_id = #{nftId} AND nft_id > 0 AND state = 0")
    int countActiveByNft(@Param("nftId") Long nftId);

    @Insert("INSERT INTO `order` (seller_id, item_id, nft_id, amount, price, state, buyer_id, created_at, updated_at) " +
            "VALUES (#{sellerId}, #{itemId}, #{nftId}, #{amount}, #{price}, #{state}, #{buyerId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("UPDATE `order` SET state = #{toState}, buyer_id = #{buyerId}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND state = #{fromState}")
    int updateState(@Param("id") Long id, @Param("fromState") Integer fromState,
                    @Param("toState") Integer toState, @Param("buyerId") Long buyerId,
                    @Param("updatedAt") Long updatedAt);

    @Update("UPDATE `order` SET amount = amount - #{take}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND amount >= #{take}")
    int subAmount(@Param("id") Long id, @Param("take") long take, @Param("updatedAt") Long updatedAt);
}
