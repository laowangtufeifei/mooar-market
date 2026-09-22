package com.mooar.market.mapper;

import com.mooar.market.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper {

    @Select("SELECT * FROM `user` WHERE address = #{address} LIMIT 1")
    User selectByAddress(@Param("address") String address);

    @Select("SELECT * FROM `user` WHERE id = #{id}")
    User selectById(@Param("id") Long id);

    @Insert("INSERT INTO `user` (fid, email, address, state, created_at, updated_at) " +
            "VALUES (#{fid}, #{email}, #{address}, #{state}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE `user` SET fid = #{fid}, email = #{email}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateLogin(@Param("id") Long id, @Param("fid") String fid, @Param("email") String email,
                    @Param("updatedAt") Long updatedAt);
}
