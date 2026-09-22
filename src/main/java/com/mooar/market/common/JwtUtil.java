package com.mooar.market.common;

import com.mooar.market.config.MarketProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtUtil {

    private final boolean enable;
    private final PublicKey publicKey;

    public JwtUtil(MarketProperties properties) {
        this.enable = properties.getJwt().isEnable();
        this.publicKey = enable ? parsePublicKey(properties.getJwt().getPublicKey()) : null;
    }

    public LoginUser parse(String token) {
        if (token == null || token.isBlank()) {
            throw new BizException(ErrorCode.LOGIN_TOKEN_ERROR);
        }
        Claims claims;
        try {
            JwtParserBuilder builder = Jwts.parserBuilder();
            if (enable) {
                builder.setSigningKey(publicKey);
                claims = builder.build().parseClaimsJws(token).getBody();
            } else {
                claims = (Claims) builder.build().parse(token.substring(0, token.lastIndexOf(".") + 1)).getBody();
            }
        } catch (Exception e) {
            throw new BizException(ErrorCode.LOGIN_TOKEN_ERROR);
        }
        String fid = claims.get("uid", String.class);
        String email = claims.get("email", String.class);
        String address = claims.get("evm_address", String.class);
        if (fid == null || fid.isBlank() || address == null || address.isBlank()) {
            throw new BizException(ErrorCode.LOGIN_TOKEN_ERROR);
        }
        return new LoginUser(fid, email, address.toLowerCase());
    }

    private PublicKey parsePublicKey(String base64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
