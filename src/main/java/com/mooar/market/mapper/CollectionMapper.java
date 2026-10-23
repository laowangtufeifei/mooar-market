package com.mooar.market.mapper;

import com.mooar.market.entity.NftCollection;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface CollectionMapper {

    @Select("SELECT * FROM collection WHERE state = 0 ORDER BY sort ASC, id ASC")
    List<NftCollection> selectAll();

    @Select("SELECT * FROM collection WHERE id = #{id}")
    NftCollection selectById(@Param("id") Long id);
}
