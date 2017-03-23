package com.seastar;

import com.seastar.repository.UserBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by osx on 17/3/16.
 */
@Component
@Order(value = 1)
public class StartupRunner implements CommandLineRunner {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserBaseRepository userBaseRepository;

    @Override
    public void run(String... args) throws Exception {
        Long id = userBaseRepository.getMaxId();
        if (id == null)
            redisTemplate.opsForValue().set("USERID_IN_ALL_SERVER", "0");
        else
            redisTemplate.opsForValue().set("USERID_IN_ALL_SERVER", id + "");
    }
}
