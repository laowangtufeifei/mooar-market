package com.mooar.market.mapper;

import com.mooar.market.entity.ChainAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ChainAccountMapper {

    @Select("SELECT * FROM chain_account WHERE address = #{address} LIMIT 1")
    ChainAccount selectByAddress(@Param("address") String address);

    @Insert("INSERT INTO chain_account (address, encrypted_private_key, chain_id, state, created_at, updated_at) " +
            "VALUES (#{address}, #{encryptedPrivateKey}, #{chainId}, #{state}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChainAccount account);

    @Update("UPDATE chain_account SET encrypted_private_key = #{encryptedPrivateKey}, updated_at = #{updatedAt} " +
            "WHERE address = #{address}")
    int updatePrivateKey(@Param("address") String address, @Param("encryptedPrivateKey") String encryptedPrivateKey,
                         @Param("updatedAt") Long updatedAt);
}
