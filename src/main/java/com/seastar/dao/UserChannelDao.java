package com.seastar.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.model.UserChannel;
import com.seastar.repository.UserChannelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by osx on 16/12/12.
 */
@Component
public class UserChannelDao {
    @Autowired
    private UserChannelRepository userChannelRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();

    public UserChannel findOne(int channelType, String channelId) {
        try {
            String json = redisTemplate.opsForValue().get("userchannel_" + channelId + channelType);
            if (json != null) {
                return objectMapper.readValue(json, UserChannel.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        UserChannel userChannel = userChannelRepository.findByChannelIdAndChannelType(channelId, channelType);

        try {
            if (userChannel != null)
                redisTemplate.opsForValue().set("userchannel_" + channelId + channelType, objectMapper.writeValueAsString(userChannel), 30, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return userChannel;
    }

    public List<UserChannel> findAll(long userId) {
        return userChannelRepository.findByUserId(userId);
    }

    public void save(UserChannel userChannel) {
        userChannelRepository.save(userChannel);

        try {
            redisTemplate.opsForValue().set("userchannel_" + userChannel.getUserId() + userChannel.getChannelType(), objectMapper.writeValueAsString(userChannel), 30, TimeUnit.DAYS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delete(UserChannel userChannel) {
        userChannelRepository.deleteByChannelIdAndChannelType(userChannel.getChannelId(), userChannel.getChannelType());
        if (userChannel != null)
            redisTemplate.delete("userchannel_" + userChannel.getChannelId() + userChannel.getChannelType());
    }
}
