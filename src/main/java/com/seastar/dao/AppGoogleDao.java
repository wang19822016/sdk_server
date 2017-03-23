package com.seastar.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.model.AppGoogle;
import com.seastar.repository.AppGoogleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by osx on 16/12/13.
 */
@Component
public class AppGoogleDao {
    @Autowired
    private AppGoogleRepository appGoogleRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    public AppGoogle findOne(int appId) {
        try {
            String json = redisTemplate.opsForValue().get("app_google_" + appId);
            if (json != null) {
                return objectMapper.readValue(json, AppGoogle.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        AppGoogle appGoogle = appGoogleRepository.findOne(appId);

        try {
            if (appGoogle != null)
                redisTemplate.opsForValue().set("app_google_" + appId, objectMapper.writeValueAsString(appGoogle), 30, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return appGoogle;
    }
}
