package com.seastar.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seastar.model.PayInfo;
import com.seastar.utils.HttpUtils;
import com.seastar.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wjl on 2016/8/24.
 */
@Component
public class PushTask {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ConcurrentHashMap<String, String> operativeDataMap = new ConcurrentHashMap<>();
    private PriorityBlockingQueue<PushItem> blockingQueue;
    private HttpUtils httpUtils = new HttpUtils();

    private ObjectMapper mapper = new ObjectMapper();

    private long[] PUSH_GAP = new long[] { 5 * 60 * 1000, 15 * 60 * 1000, 60 * 60 * 1000, 3 * 60 * 60 * 1000 };

    private Logger logger = LoggerFactory.getLogger(PushTask.class);

    @PostConstruct
    public void init() {
        blockingQueue = new PriorityBlockingQueue<>(200, (PushItem x, PushItem y) -> {
            if (x.time < y.time)
                return -1;
            else if (x.time > y.time)
                return 1;
            else
                return 0;
        });
    }

    @Async
    public void submit(String url, PayInfo payInfo, String appSecret) {
        String coin = "0";
        String giveCoin = "0";
        String money = payInfo.getPrice();
        String productId = payInfo.getSku();

        /*
        // 提取附赠数据
        if (payInfo.getChannelType() == Const.PAY_CHANNEL_MYCARD) {
            // 首先获取数据，查看是否有活动
            String result = redisTemplate.opsForValue().get("mycard" + payInfo.getOrder());
            if (result != null) {
                try {
                    JsonNode trade = mapper.readTree(result);
                    String promoCode = trade.get("promoCode").asText();
                    if (!promoCode.isEmpty()) {
                        // 提取活动码对应的充值活动
                        productId = productId + promoCode; // 此处一定要记着，活动码是跟其他活动混在t_operative里面的
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        String key = payInfo.getAppId() + productId;
        String result = operativeDataMap.get(key);
        if (result != null) {
            try {
                String[] arrs = result.split("|");
                coin = arrs[0];
                giveCoin = arrs[1];
                money = arrs[2];
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                Map<String, Object> resultMap =
                        jdbcTemplate.queryForMap("select virtualCoin, giveVirtualCoin, money from operative where appId=? and productId=?",
                                payInfo.getAppId(), productId);

                coin = (String) resultMap.get("virtualCoin");
                giveCoin = (String) resultMap.get("giveVirtualCoin");
                money = (String) resultMap.get("money");

                operativeDataMap.put(key, coin + "|" + giveCoin + "|" + money);
            } catch (DataAccessException ex) {
                ex.printStackTrace();
            }
        }
        */
        try {
            ObjectNode root = mapper.createObjectNode();

            root.put("order", payInfo.getOrder());
            root.put("appId", payInfo.getAppId() + "");
            root.put("userId", payInfo.getUserId() + "");
            root.put("gameRoleId", payInfo.getCustomerId());
            root.put("serverId", payInfo.getServerId());
            root.put("channelType", payInfo.getChannelType() + "");
            root.put("productId", payInfo.getSku());
            root.put("productAmount", 1 + "");
            root.put("money", money);
            root.put("currency", payInfo.getCurrency());
            root.put("status", 0 + "");
            root.put("sandbox", payInfo.getSandbox() + "");
            root.put("cparam", payInfo.getExtra());
            root.put("virtualCoin", coin);
            root.put("giveVirtualCoin", giveCoin);


            String md5Origin = payInfo.getOrder() + "|" +
                    payInfo.getAppId() + "|" +
                    payInfo.getUserId() + "|" +
                    payInfo.getCustomerId() + "|" +
                    payInfo.getServerId() + "|" +
                    payInfo.getChannelType() + "|" +
                    payInfo.getSku() + "|" +
                    1 + "|" +
                    money + "|" + //data.getString("money") +
                    payInfo.getCurrency() + "|" +
                    0 + "|" +
                    payInfo.getSandbox() + "|" +
                    payInfo.getExtra() + "|" +
                    coin + "|" +
                    giveCoin + "|" +
                    appSecret;

            root.put("sign", Utils.md5encode(md5Origin));

            PushItem item = new PushItem();
            item.url = url;
            item.data = mapper.writeValueAsString(root);
            item.order = payInfo.getOrder();
            item.retry = 0;
            item.time = System.currentTimeMillis();

            blockingQueue.offer(item);

            logger.info("push pay begin: {} {} {}", item.url, item.data, md5Origin);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Scheduled(fixedDelay = 200)
    public void scanDelay() {
        try {
            long curTime = System.currentTimeMillis();
            while (true) {
                PushItem top = blockingQueue.peek();
                if (top == null || top.time > curTime)
                    break;

                PushItem item = blockingQueue.poll();

                Map<String, String> headers = new HashMap<>();
                headers.put("content-type", "application/json; charset=UTF-8");
                headers.put("Accept", "application/json");

                // 开始推送
                HttpUtils.HttpResp httpResp = httpUtils.post(item.url, item.data, headers);
                if (httpResp.code == 200 && httpResp.body.equals(trim(item.order))) {
                    // 推送成功
                    try {
                        jdbcTemplate.update("update pay_info set status=?, notifyTime=now() where `order`=?", 0, item.order);

                        logger.info("push pay success: {} {}", item.url, item.data);
                    } catch (DataAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 推送失败，进入重试队列
                    if (item.retry <= 3) {
                        item.time = System.currentTimeMillis() + PUSH_GAP[item.retry];
                        item.retry++;

                        blockingQueue.offer(item);

                        logger.info("push pay fail, retry: {} {} {}", item.url, item.data, httpResp.body);
                    } else {
                        // 彻底失败
                        try {
                            jdbcTemplate.update("update pay_info set status=?, notifyTime=now() where `order`=?", 1, item.order);

                            logger.info("push pay fail: {} {}", item.url, item.data);
                        } catch (DataAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String trim(String str) {
        String dest = "";
        if (str!=null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }
}
