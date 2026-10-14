package com.mooar.market.mapper;

import com.mooar.market.entity.Item;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ItemMapper {

    @Select("SELECT * FROM item")
    List<Item> selectAll();

    /**
     * 需要扫块的道具：item.state = 1
     */
    @Select("SELECT * FROM item WHERE state = 1")
    List<Item> selectScanItems();

    @Select("SELECT * FROM item WHERE id = #{id}")
    Item selectById(@Param("id") Long id);

    @Select("SELECT * FROM item WHERE contract = #{contract} AND token_id = #{tokenId} LIMIT 1")
    Item selectByContractAndTokenId(@Param("contract") String contract, @Param("tokenId") Long tokenId);

    @Select("SELECT * FROM item WHERE contract = #{contract} LIMIT 1")
    Item selectByContract(@Param("contract") String contract);
}
