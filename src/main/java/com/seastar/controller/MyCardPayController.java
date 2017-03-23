package com.seastar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seastar.async.PushTask;
import com.seastar.common.Const;
import com.seastar.config.annotation.Authorization;
import com.seastar.config.annotation.Token;
import com.seastar.dao.AppDao;
import com.seastar.dao.PayDao;
import com.seastar.dao.SkuDao;
import com.seastar.entity.MyCardCompleteReq;
import com.seastar.entity.MyCardCompleteRsp;
import com.seastar.entity.MyCardReqAuthCodeReq;
import com.seastar.entity.MyCardReqAuthCodeRsp;
import com.seastar.model.App;
import com.seastar.model.MyCardTrade;
import com.seastar.model.PayInfo;
import com.seastar.model.Sku;
import com.seastar.utils.HttpUtils;
import com.seastar.utils.JWT;
import com.seastar.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by osx on 17/3/15.
 */
@RestController
public class MyCardPayController {
    @Value("${mycard.FacServiceId}")
    private String facServiceId;

    @Value("${mycard.TradeType}")
    private String tradeType;

    @Value("${mycard.HashKey}")
    private String hashKey;

    @Value("${mycard.SandBoxMode}")
    private String sandBoxMode;

    @Autowired
    private AppDao appRepo;

    @Autowired
    private PayDao payRepo;

    @Autowired
    private SkuDao skuRepo;

    @Autowired
    private PushTask pushTask;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private HttpUtils httpUtils = new HttpUtils();
    private ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(MyCardPayController.class);

    @Authorization
    @RequestMapping(value = "/api/pay/mycard/authcode", method = RequestMethod.POST)
    public ResponseEntity<MyCardReqAuthCodeRsp> onMycardReqAuthCode(@Token JWT jwt, @RequestBody MyCardReqAuthCodeReq req) {
        App app = appRepo.findOne(jwt.getPayload().getAppId());
        if (app == null) {
            logger.error("应用不存在, username: {}, appId: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId());
            return new ResponseEntity<MyCardReqAuthCodeRsp>(HttpStatus.BAD_REQUEST);
        }

        // 获取商品
        Sku sku = skuRepo.findOne(jwt.getPayload().getAppId(), req.itemCode);
        if (sku == null) {
            logger.error("商品不存在, username: {}, appId: {}, sku: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), sku.getSku());
            return new ResponseEntity<MyCardReqAuthCodeRsp>(HttpStatus.BAD_REQUEST);
        }

        String order = payRepo.getMycardOrder();
        String productName = "";
        try {
            productName = URLEncoder.encode(sku.getName(), "utf-8").toLowerCase();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        String preHashValue = facServiceId +
                order +
                tradeType +
                req.customerId +
                productName +
                sku.getPrice() +
                sku.getCurrency() +
                sandBoxMode + hashKey;

        preHashValue = Utils.sha256encode(preHashValue);

        String url = "https://b2b.mycard520.com.tw/MyBillingPay/api/AuthGlobal";
        if (sandBoxMode.equals("true"))
            url = "https://test.b2b.mycard520.com.tw/MyBillingPay/api/AuthGlobal";

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "*/*");

        // 厂商用户不需要在mycard后台配置任何商品id，所以也不需要传商品ID过去
        Map<String, String> body = new HashMap<>();
        body.put("FacServiceId", facServiceId);
        body.put("FacTradeSeq", order);
        body.put("TradeType", tradeType);
        body.put("CustomerId", req.customerId);
        body.put("ProductName", productName);
        body.put("Amount", sku.getPrice());
        body.put("Currency", sku.getCurrency());
        body.put("SandBoxMode", sandBoxMode);
        body.put("Hash", preHashValue);

        try {
            HttpUtils.HttpResp httpResp = httpUtils.post(url, body, headers);
            if (httpResp.code == 200) {
                JsonNode root = objectMapper.readTree(httpResp.body);
                if (root.get("ReturnCode").asText().equals("1")) {
                    String authCode = root.get("AuthCode").asText();
                    String tradeSeq = root.get("TradeSeq").asText();
                    String inGameSaveType = root.get("InGameSaveType").asText();

                    MyCardReqAuthCodeRsp rsp = new MyCardReqAuthCodeRsp();
                    rsp.sandBoxMode = (sandBoxMode.equals("true") ? true : false);
                    rsp.authCode = authCode;

                    // 将本次数据存储到redis
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("appId", jwt.getPayload().getAppId());
                    node.put("notifyUrl", app.getNotifyUrl());
                    node.put("appSecret", app.getSecret());
                    node.put("userId", jwt.getPayload().getUserId());
                    node.put("facTradeSeq", order); // 交易序号，厂商自定义
                    node.put("customerId", req.customerId); // 会员代号，用户id
                    node.put("serverId", req.serverId);
                    node.put("itemCode", sku.getSku()); // mycard品项代码
                    node.put("sandBoxMode", sandBoxMode); // 是否为测试环境
                    node.put("authCode", authCode); // 授权码
                    node.put("cparam", req.cparam);

                    redisTemplate.opsForValue().set("mycard_" + order, objectMapper.writeValueAsString(node), 30, TimeUnit.DAYS);

                    logger.info("请求授权成功, username: {}, origin: {}", jwt.getPayload().getUsername(), objectMapper.writeValueAsString(node));
                    return new ResponseEntity<MyCardReqAuthCodeRsp>(rsp, HttpStatus.OK);
                }
            }

            logger.error("去mycard请求授权码失败, username: {}, appId: {}, httpCode: {}, httpBody: {}, origin: {}",
                    jwt.getPayload().getUsername(),
                    jwt.getPayload().getAppId(),
                    httpResp.code,
                    httpResp.body,
                    req.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ResponseEntity<MyCardReqAuthCodeRsp>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Authorization
    @RequestMapping(value = "/api/pay/mycard/money", method = RequestMethod.POST)
    public ResponseEntity<MyCardCompleteRsp> onMycardComplete(@Token JWT jwt, @RequestBody MyCardCompleteReq req) {
        App app = appRepo.findOne(jwt.getPayload().getAppId());
        if (app == null) {
            logger.error("应用不存在, username: {}, appId: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId());
            return new ResponseEntity<MyCardCompleteRsp>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (processTrade(req.facTradeSeq)) {
            MyCardCompleteRsp rsp = new MyCardCompleteRsp();
            rsp.facTradeSeq = req.facTradeSeq;
            rsp.itemCode = "";

            logger.info("请款成功, username: {}, appId: {}, facTradeSeq: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.facTradeSeq);
            return new ResponseEntity<MyCardCompleteRsp>(rsp, HttpStatus.OK);
        }

        logger.info("请款失败, username: {}, appId: {}, facTradeSeq: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.facTradeSeq);
        return new ResponseEntity<MyCardCompleteRsp>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/mycard/notify", method = RequestMethod.POST)
    @ResponseBody
    public String doNotify(@RequestParam(value = "DATA") String body) {
        logger.info("收到mycard通知, {}", body);
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.get("ReturnCode").asText().equals("1")) {
                String facServiceId = root.get("FacServiceId").asText();
                int totalNum = root.get("TotalNum").asInt();
                JsonNode facTradeSeqs = root.get("FacTradeSeq");
                for (JsonNode facTradeSeq : facTradeSeqs) {
                    processTrade(facTradeSeq.asText());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @RequestMapping(value = "/mycard/cmp", method = RequestMethod.POST)
    @ResponseBody
    public String doDiffByMycardTradeNo(HttpServletRequest request, String myCardTradeNo) {
        String rsp = "<BR>";

        if (request.getParameter("MyCardTradeNo") != null) {
            MyCardTrade trade = payRepo.findMycards(request.getParameter("MyCardTradeNo"));
            List<MyCardTrade> list = new ArrayList<>();
            list.add(trade);
            rsp = assembleDiffResult(list);
        } else if (request.getParameter("StartDateTime") != null) {
            Date begin = getDate(request.getParameter("StartDateTime"));
            Date end = getDate(request.getParameter("EndDateTime"));
            rsp = assembleDiffResult(payRepo.findMycards(begin, end));
        }

        return rsp;
    }

    private Date getDate(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return sdf.parse(timeStr);
        } catch (ParseException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private String assembleDiffResult(List<MyCardTrade> results) {
        String result = "<BR>";
        if (results != null) {
            for (MyCardTrade map : results) {
                result += (String) map.getPaymentType() + ',';
                result += (String) map.getTradeSeq() + ',';
                result += (String) map.getMycardTradeNo() + ',';
                result += (String) map.getFacTradeSeq() + ',';
                result += (String) map.getCustomerId() + ',';
                result += (String) map.getAmount() + ',';
                result += (String) map.getCurrency() + ',';

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                String tradeDateStr = sdf.format(map.getTradeDateTime());

                result += tradeDateStr;
                result += "<BR>";
            }
        }

        return result;
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

    private boolean processTrade(String facTradeSeq) {
        try {
            String origin = redisTemplate.opsForValue().get("mycard_" + facTradeSeq);
            if (origin == null) return false;
            JsonNode trade = objectMapper.readTree(origin);
            if (trade == null) return false;

            Map<String, String> body = new HashMap<>();
            body.put("Authcode", trade.get("authCode").asText());

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Accept", "*/*");

            String queryUrl = "https://b2b.mycard520.com.tw/MyBillingPay/api/TradeQuery";
            if (trade.get("sandBoxMode").asText().equals("true") ? true : false)
                queryUrl = "https://test.b2b.mycard520.com.tw/MyBillingPay/api/TradeQuery";

            String getMoneyUrl = "https://b2b.mycard520.com.tw/MyBillingPay/api/PaymentConfirm";
            if (trade.get("sandBoxMode").asText().equals("true") ? true : false)
                getMoneyUrl = "https://test.b2b.mycard520.com.tw/MyBillingPay/api/PaymentConfirm";

            HttpUtils.HttpResp httpResp = httpUtils.post(queryUrl, body, headers);
            if (httpResp.code == 200) {
                JsonNode tradeQuery = objectMapper.readTree(httpResp.body);
                if (tradeQuery.get("ReturnCode").asText().equals("1")) {
                    String payResult = tradeQuery.get("PayResult").asText();
                    //String facTradeSeq = tradeQuery.get("FacTradeSeq").asText();
                    if (payResult.equals("3")) {
                        String paymentType = tradeQuery.get("PaymentType").asText(); // mycard付费方式, INGAME点卡，COSTPOINT会员扣点, FA018上海webatm, FA029中华电信HiNet连扣, FA200000002测试用
                        String amount = tradeQuery.get("Amount").asText(); // 交易金额，可以为整数，若有小数最多2位
                        String currency = tradeQuery.get("Currency").asText(); // 货币种类, TWD/HKD/USD

                        //1.PaymentType = INGAME 時，傳 MyCard 卡片號碼
                        //2.PaymentType = COSTPOINT 時，傳會員扣點交易序號，格式為 MMS 開頭+數字
                        //3.其餘 PaymentType 為 Billing 小額付款交易，傳 Billing 交易序號
                        //特別注意: 交易時，同一個 MyCard 卡片號碼、會員扣點交易序號和 Billing 交易序號只
                        //能被儲值成功一次，請廠商留意，以免造成重複儲值的情形
                        String myCardTradeNo = tradeQuery.get("MyCardTradeNo").asText();
                        String myCardType = tradeQuery.get("MyCardType").asText();
                        String promoCode = tradeQuery.get("PromoCode").asText();
                        String serialId = tradeQuery.get("SerialId").asText();

                        httpResp = httpUtils.post(getMoneyUrl, body, headers);
                        if (httpResp.code == 200) {
                            JsonNode getMoney = objectMapper.readTree(httpResp.body);
                            if (getMoney.get("ReturnCode").asText().equals("1")) {
                                String tradeSeq = getMoney.get("TradeSeq").asText();
                                serialId = getMoney.get("SerialId").asText();

                                redisTemplate.delete("mycard_" + facTradeSeq);

                                // 存储mycard订单
                                MyCardTrade myCardTrade1 = new MyCardTrade(paymentType, tradeSeq, myCardTradeNo, facTradeSeq, trade.get("customerId").asText(), amount, currency);
                                payRepo.saveMycard(myCardTrade1);

                                // 交易成功
                                // 生成订单
                                PayInfo payInfo = new PayInfo();
                                payInfo.setSku(trade.get("itemCode").asText());
                                payInfo.setUserId(trade.get("userId").asLong());
                                payInfo.setAppId(trade.get("appId").asInt());
                                payInfo.setOrder(facTradeSeq);
                                payInfo.setChannelOrder(tradeSeq);
                                payInfo.setChannelType(Const.PAY_CHANNEL_MYCARD);
                                payInfo.setCustomerId(trade.get("customerId").asText());
                                payInfo.setServerId(trade.get("serverId").asText());
                                payInfo.setPrice(amount);
                                payInfo.setCurrency(currency);
                                payInfo.setCurrency_used(currency);
                                payInfo.setSandbox(trade.get("sandBoxMode").asText().equals("true") ? Const.SANDBOX : Const.PRODUCTION);
                                payInfo.setExtra(Utils.b64encode(trade.get("cparam").asText()));
                                if (!payRepo.save(payInfo)) {
                                    logger.error("订单重复使用，或者数据库错误, origin: {}", objectMapper.writeValueAsString(payInfo));
                                }

                                String nurl = trade.get("notifyUrl").asText();
                                String secret = trade.get("appSecret").asText();
                                // 推送
                                if (!nurl.isEmpty()) {
                                    pushTask.submit(nurl, payInfo, secret);
                                }

                                return true;
                            }
                        }
                    }
                }
            }

            logger.error("请款失败, facTradeSeq: {}, httpCode: {}, httpBody: {}", facTradeSeq, httpResp.code, httpResp.body);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
