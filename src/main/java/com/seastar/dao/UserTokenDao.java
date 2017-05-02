package com.seastar.dao;

import com.seastar.utils.JWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by osx on 17/3/14.
 */
@Component
public class UserTokenDao {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void save(JWT jwt) {
        redisTemplate.opsForValue().set("token_" + jwt.getToken(), "1", jwt.getPayload().getExp() - jwt.getPayload().getIat(), TimeUnit.SECONDS);
    }

    public String findOne(String token) {
        String json = redisTemplate.opsForValue().get("token_" + token);
        if (json != null) {
            return token;
        }

        return null;
    }

    public void delete(String token) {
        redisTemplate.delete("token_" + token);
    }
}
