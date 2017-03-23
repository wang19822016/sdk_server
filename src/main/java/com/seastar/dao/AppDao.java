package com.seastar.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.model.App;
import com.seastar.repository.AppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by osx on 16/12/12.
 */
@Component
public class AppDao {
    @Autowired
    private AppRepository appRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    public App findOne(int appId) {
        try {
            String json = redisTemplate.opsForValue().get("app_" + appId);
            if (json != null) {
                return objectMapper.readValue(json, App.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        App app = appRepository.findOne(appId);
        if (app != null) {
            try {
                redisTemplate.opsForValue().set("app_" + appId, objectMapper.writeValueAsString(app), 30, TimeUnit.DAYS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return app;
    }
}
