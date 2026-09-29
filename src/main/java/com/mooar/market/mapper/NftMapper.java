package com.mooar.market.mapper;

import com.mooar.market.entity.Nft;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface NftMapper {

    @Select("SELECT * FROM nft WHERE id = #{id}")
    Nft selectById(@Param("id") Long id);

    @Select("SELECT * FROM nft WHERE contract = #{contract} AND token_id = #{tokenId} LIMIT 1")
    Nft selectByContractAndTokenId(@Param("contract") String contract, @Param("tokenId") Long tokenId);

    @Select("SELECT * FROM nft WHERE owner_id = #{ownerId} AND state = 0")
    List<Nft> selectByOwner(@Param("ownerId") Long ownerId);

    @Insert("INSERT INTO nft (item_id, contract, token_id, owner_id, state, order_id, image, metadata, created_at, updated_at) " +
            "VALUES (#{itemId}, #{contract}, #{tokenId}, #{ownerId}, #{state}, #{orderId}, #{image},#{metadata}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Nft nft);

    @Update("UPDATE nft SET state = #{toState}, order_id = #{orderId}, updated_at = #{updatedAt} " +
            "WHERE id = #{id} AND state = #{fromState}")
    int updateState(@Param("id") Long id, @Param("fromState") Integer fromState,
                    @Param("toState") Integer toState, @Param("orderId") Long orderId,
                    @Param("updatedAt") Long updatedAt);

    @Update("UPDATE nft SET owner_id = #{ownerId}, state = 0, order_id = 0, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateOwner(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE nft SET metadata = #{metadata}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateMetadata(@Param("id") Long id, @Param("metadata") String metadata, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE nft SET state = 2, updated_at = #{updatedAt} WHERE id = #{id} AND state = 0 AND owner_id = #{ownerId}")
    int lockForWithdraw(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("updatedAt") Long updatedAt);

    @Update("UPDATE nft SET owner_id = 0, state = 0, updated_at = #{updatedAt} WHERE id = #{id}")
    int release(@Param("id") Long id, @Param("updatedAt") Long updatedAt);
}
