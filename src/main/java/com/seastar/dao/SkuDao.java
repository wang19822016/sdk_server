package com.seastar.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.model.Sku;
import com.seastar.repository.SkuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by osx on 16/12/10.
 */
@Component
public class SkuDao {

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Sku findOne(int appId, String sku) {
        try {
            String json = redisTemplate.opsForValue().get("sku_" + appId + sku);
            if (json != null) {
                return objectMapper.readValue(json, Sku.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Sku skuItem = skuRepository.findByAppIdAndSku(appId, sku);

        try {
            if (skuItem != null)
                redisTemplate.opsForValue().set("sku_" + appId + sku, objectMapper.writeValueAsString(skuItem), 30, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return skuItem;
    }
}
