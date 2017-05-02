package com.seastar.dao;

import com.seastar.common.Const;
import com.seastar.model.MyCardTrade;
import com.seastar.model.PayInfo;
import com.seastar.repository.MyCardTradeRepository;
import com.seastar.repository.PayInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by osx on 16/12/13.
 */
@Component
public class PayDao {
    @Autowired
    private PayInfoRepository payInfoRepository;

    @Autowired
    private MyCardTradeRepository myCardTradeRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean save(PayInfo info) {
        // 首先查看是否已经使用
        String key = "";
        if (info.getChannelType() == Const.PAY_CHANNEL_GOOGLE) {
            key = "order_google_" + info.getChannelOrder();

            if (!info.getChannelOrder().isEmpty()) {
                // 订单已经用过
                if (redisTemplate.opsForValue().get(key) != null)
                    return false;

                redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.DAYS);
            }
        } else if (info.getChannelType() == Const.PAY_CHANNEL_APPLE) {
            key = "order_apple_" + info.getChannelOrder();

            // 订单已经用过
            if (redisTemplate.opsForValue().get(key) != null)
                return false;

            redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.DAYS);
        }

        // 存储到数据库
        payInfoRepository.save(info);

        return true;
    }

    public void saveMycard(MyCardTrade myCardTrade) {
        myCardTradeRepository.save(myCardTrade);
    }

    public List<MyCardTrade> findMycards(Date startDateTime, Date endDateTime) {
        return myCardTradeRepository.findByTime(startDateTime, endDateTime);
    }

    public MyCardTrade findMycards(String tradeNo) {
        return myCardTradeRepository.findByMycardTradeNo(tradeNo);
    }


    public String getGoogleOrder() {
        return "ST_Google_" + UUID.randomUUID().toString();
    }

    public String getAppleOrder() {
        return "ST_Apple_" + UUID.randomUUID().toString();
    }

    public String getMycardOrder() {
        return "ST_Mycard_" + UUID.randomUUID().toString();
    }

    public String getPaypalOrder() {
        return "ST_Paypal_" + UUID.randomUUID().toString();
    }
}
