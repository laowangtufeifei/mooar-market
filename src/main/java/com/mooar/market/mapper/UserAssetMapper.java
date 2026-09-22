package com.mooar.market.mapper;

import com.mooar.market.entity.UserAsset;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserAssetMapper {

    @Select("SELECT * FROM user_asset WHERE user_id = #{userId} AND item_id = #{itemId} LIMIT 1")
    UserAsset selectByUserAndItem(@Param("userId") Long userId, @Param("itemId") Long itemId);

    @Select("SELECT * FROM user_asset WHERE user_id = #{userId}")
    List<UserAsset> selectByUser(@Param("userId") Long userId);

    @Insert("INSERT INTO user_asset (user_id, item_id, available, frozen, created_at, updated_at) " +
            "VALUES (#{userId}, #{itemId}, #{available}, #{frozen}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAsset asset);

    @Update("UPDATE user_asset SET available = available + #{amount}, updated_at = #{updatedAt} " +
            "WHERE user_id = #{userId} AND item_id = #{itemId}")
    int addAvailable(@Param("userId") Long userId, @Param("itemId") Long itemId,
                     @Param("amount") Long amount, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE user_asset SET available = available - #{amount}, updated_at = #{updatedAt} " +
            "WHERE user_id = #{userId} AND item_id = #{itemId} AND available >= #{amount}")
    int subAvailable(@Param("userId") Long userId, @Param("itemId") Long itemId,
                     @Param("amount") Long amount, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE user_asset SET available = available - #{amount}, frozen = frozen + #{amount}, updated_at = #{updatedAt} " +
            "WHERE user_id = #{userId} AND item_id = #{itemId} AND available >= #{amount}")
    int freeze(@Param("userId") Long userId, @Param("itemId") Long itemId,
               @Param("amount") Long amount, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE user_asset SET frozen = frozen - #{amount}, available = available + #{amount}, updated_at = #{updatedAt} " +
            "WHERE user_id = #{userId} AND item_id = #{itemId} AND frozen >= #{amount}")
    int unfreeze(@Param("userId") Long userId, @Param("itemId") Long itemId,
                 @Param("amount") Long amount, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE user_asset SET frozen = frozen - #{amount}, updated_at = #{updatedAt} " +
            "WHERE user_id = #{userId} AND item_id = #{itemId} AND frozen >= #{amount}")
    int subFrozen(@Param("userId") Long userId, @Param("itemId") Long itemId,
                  @Param("amount") Long amount, @Param("updatedAt") Long updatedAt);
}
