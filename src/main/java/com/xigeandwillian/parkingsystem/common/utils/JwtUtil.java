package com.xigeandwillian.parkingsystem.common.utils;

import com.xigeandwillian.parkingsystem.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * 生成jwt
     *
     * @param subject 主题
     * @param claims 存储的内容
     * @return token
     */
    public String createJWT(String subject, Map<String, Object> claims) {

        //令牌过期时间
        Date exp = new Date(System.currentTimeMillis() + jwtProperties.getExpiration());

        String jwtToken = Jwts.builder()
                .subject(subject)
                .claims(claims)
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes()))
                .expiration(exp)
                .compact();

        return jwtToken;
    }

    /**
     *
     * @param subject jwt主题
     * @return token
     */
    public String createJWT(String subject) {

        //令牌过期时间
        Date exp = new Date(System.currentTimeMillis() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(subject)
                .signWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes()))
                .expiration(exp)
                .compact();
    }

    /**
     * Token解密
     *
     * @param token jwtToken
     * @return 内容
     */
    public Claims parseJWT(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }

    /**
     * @author xige
     * @param token jwtToken
     * @return jwt主题
     */
    public String getSubject(String token){
        Claims claims = parseJWT(token);
        return claims.getSubject();
    }
}
