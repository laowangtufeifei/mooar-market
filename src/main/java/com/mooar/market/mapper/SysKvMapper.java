package com.mooar.market.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysKvMapper {

    @Select("SELECT v FROM sys_kv WHERE k = #{k} LIMIT 1")
    String get(@Param("k") String k);

    @Insert("INSERT INTO sys_kv (k, v) VALUES (#{k}, #{v}) ON DUPLICATE KEY UPDATE v = #{v}")
    int put(@Param("k") String k, @Param("v") String v);
}
