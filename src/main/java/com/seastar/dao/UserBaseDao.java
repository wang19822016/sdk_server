package com.seastar.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.model.UserBase;
import com.seastar.repository.UserBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by osx on 16/12/12.
 */
@Component
public class UserBaseDao {
    @Autowired
    private UserBaseRepository userBaseRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    public long getMaxId() {
        return redisTemplate.opsForValue().increment("USERID_IN_ALL_SERVER", 1);
    }

    public UserBase findOne(long userId) {
        if (userId < 0)
            return null;

        try {
            String json = redisTemplate.opsForValue().get("userbase_id_" + userId);
            if (json != null) {
                return objectMapper.readValue(json, UserBase.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        UserBase userBase = userBaseRepository.findOne(userId);

        try {
            if (userBase != null)
                redisTemplate.opsForValue().set("userbase_id_" + userId, objectMapper.writeValueAsString(userBase), 30, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return userBase;
    }

    public UserBase findOneByName(String userName) {
        try {
            String json = redisTemplate.opsForValue().get("userbase_name_" + userName);
            if (json != null) {
                return objectMapper.readValue(json, UserBase.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        UserBase userBase = userBaseRepository.findByName(userName);

        try {
            if (userBase != null)
                redisTemplate.opsForValue().set("userbase_name_" + userName, objectMapper.writeValueAsString(userBase), 30, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return userBase;
    }

    public void save(UserBase userBase) {
        userBaseRepository.save(userBase);

        try {
            redisTemplate.opsForValue().set("userbase_id_" + userBase.getId(), objectMapper.writeValueAsString(userBase), 30, TimeUnit.DAYS);
            redisTemplate.opsForValue().set("userbase_name_" + userBase.getName(), objectMapper.writeValueAsString(userBase), 30, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
