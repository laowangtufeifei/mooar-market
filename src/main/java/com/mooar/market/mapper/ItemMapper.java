package com.mooar.market.mapper;

import com.mooar.market.entity.Item;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ItemMapper {

    @Select("SELECT * FROM item WHERE state = 0")
    List<Item> selectAll();

    @Select("SELECT * FROM item WHERE id = #{id}")
    Item selectById(@Param("id") Long id);

    @Select("SELECT * FROM item WHERE contract = #{contract} AND token_id = #{tokenId} LIMIT 1")
    Item selectByContractAndTokenId(@Param("contract") String contract, @Param("tokenId") Long tokenId);

    @Select("SELECT * FROM item WHERE contract = #{contract} LIMIT 1")
    Item selectByContract(@Param("contract") String contract);
}
