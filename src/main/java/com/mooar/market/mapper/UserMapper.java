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

    @Select("SELECT * FROM `user` WHERE fid = #{fid} AND fid <> '' LIMIT 1")
    User selectByFid(@Param("fid") String fid);

    @Insert("INSERT INTO `user` (fid, name, avatar, email, stepn_email, address, state, created_at, updated_at) " +
            "VALUES (#{fid}, #{name}, #{avatar}, #{email}, #{stepnEmail}, #{address}, #{state}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE `user` SET name = #{name}, avatar = #{avatar}, email = #{email}, " +
            "stepn_email = #{stepnEmail}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Long id, @Param("name") String name, @Param("avatar") String avatar,
                        @Param("email") String email, @Param("stepnEmail") String stepnEmail,
                        @Param("updatedAt") Long updatedAt);

    /**
     * 扫块建的用户第一次登录时回填 fid
     */
    @Update("UPDATE `user` SET fid = #{fid}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateFid(@Param("id") Long id, @Param("fid") String fid, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE `user` SET address = #{address}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateAddress(@Param("id") Long id, @Param("address") String address,
                      @Param("updatedAt") Long updatedAt);
}
