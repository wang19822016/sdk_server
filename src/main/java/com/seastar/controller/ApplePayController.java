package com.seastar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.async.PushTask;
import com.seastar.common.Const;
import com.seastar.config.annotation.Authorization;
import com.seastar.config.annotation.Token;
import com.seastar.dao.AppDao;
import com.seastar.dao.PayDao;
import com.seastar.dao.SkuDao;
import com.seastar.entity.AppleIapReceipt;
import com.seastar.entity.AppleIapReq;
import com.seastar.entity.PayRsp;
import com.seastar.model.App;
import com.seastar.model.PayInfo;
import com.seastar.model.Sku;
import com.seastar.utils.HttpUtils;
import com.seastar.utils.JWT;
import com.seastar.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by osx on 17/3/15.
 */
@RestController
public class ApplePayController {
    @Autowired
    private AppDao appRepo;

    @Autowired
    private PayDao payRepo;

    @Autowired
    private SkuDao skuRepo;

    @Autowired
    private PushTask pushTask;

    private ObjectMapper objectMapper = new ObjectMapper();

    private HttpUtils httpUtils = new HttpUtils();

    private Logger logger = LoggerFactory.getLogger(ApplePayController.class);

    @Authorization
    @RequestMapping(value = "/api/pay/apple", method = RequestMethod.POST)
    public ResponseEntity<PayRsp> onApplePay(@Token JWT jwt, @RequestBody AppleIapReq req) {
        App app = appRepo.findOne(jwt.getPayload().getAppId());
        if (app == null) {
            logger.error("没有应用, username: {}, appId: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        AppleIapReceipt appleIapReceipt = doIapVerify(req.receipt,false);
        if (appleIapReceipt == null) {
            logger.error("正式环境验证失败, username: {}, appId: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        int isSandbox = Const.PRODUCTION;
        // 在数据库中设置成正式环境，但苹果还是沙盒环境时重新去沙盒环境验证
        if (appleIapReceipt.status == 21007) {
            appleIapReceipt = doIapVerify(req.receipt, true);
            if (appleIapReceipt == null) {
                logger.error("沙盒环境验证失败, username: {}, appId: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.toString());
                return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
            }

            // 强制本次交易记录为沙盒
            isSandbox = Const.SANDBOX;
        }

        if (appleIapReceipt.status != 0) {
            logger.error("所有环境验证失败, username: {}, appId: {}, status: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), appleIapReceipt.status, req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        if (appleIapReceipt.receipt.in_app.size() == 0) {
            // ios6兼容处理
            if (!appleIapReceipt.receipt.product_id.equals(req.productId) ||
                    !appleIapReceipt.receipt.transaction_id.equals(req.transactionId)) {

                logger.error("ios6下，商品id或者流水号与苹果服务器的不符, username: {}, appId: {}, apple sku: {}, apple transactionId: {}, origin: {}",
                        jwt.getPayload().getUsername(),
                        jwt.getPayload().getAppId(),
                        appleIapReceipt.receipt.product_id,
                        appleIapReceipt.receipt.transaction_id,
                        req.toString());
                return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
            }
        } else {
            // ios7处理
            AppleIapReceipt.InApp inApp = null;
            for (int i = 0; i < appleIapReceipt.receipt.in_app.size(); i++) {
                inApp = appleIapReceipt.receipt.in_app.get(i);
                if (inApp.product_id.equals(req.productId) && inApp.transaction_id.equals(req.transactionId)) {
                    break;
                }
                inApp = null;
            }

            // 未找到交易信息
            if (inApp == null) {
                logger.error("ios7下，没有在苹果找到商品id或者流水号, username: {}, appId: {}, origin: {}",
                        jwt.getPayload().getUsername(),
                        jwt.getPayload().getAppId(),
                        req.toString());
                return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
            }
        }

        Sku sku = skuRepo.findOne(jwt.getPayload().getAppId(), req.productId);
        if (sku == null) {
            logger.error("没有配置商品, username: {}, appId: {}, sku: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.productId, req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        String order = payRepo.getAppleOrder();

        // 生成订单
        PayInfo payInfo = new PayInfo();
        payInfo.setSku(req.productId);
        payInfo.setUserId(jwt.getPayload().getUserId());
        payInfo.setAppId(jwt.getPayload().getAppId());
        payInfo.setOrder(order);
        payInfo.setChannelOrder(req.transactionId);
        payInfo.setChannelType(Const.PAY_CHANNEL_APPLE);
        payInfo.setCustomerId(req.gameRoleId);
        payInfo.setServerId(req.serverId);
        payInfo.setPrice(sku.getPrice());
        payInfo.setCurrency(sku.getCurrency());
        payInfo.setCurrency_used(req.currencyCode);
        payInfo.setSandbox(isSandbox);
        payInfo.setExtra(Utils.b64encode(req.cparam));
        payInfo.setCreateTime(new Date());
        payInfo.setStatus(0);
        if (!payRepo.save(payInfo)) {
            logger.error("入库失败，不是重复提交就是数据库有问题， username: {}, appId: {}, order:{}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), order, req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!app.getNotifyUrl().isEmpty()) {
            pushTask.submit(app.getNotifyUrl(), payInfo, app.getSecret());
        }

        PayRsp rsp = new PayRsp();
        rsp.sku = req.productId;
        rsp.order = order;

        logger.info("充值成功, username: {}, appId: {}, order:{}, sku: {}, roleId: {}, serverId: {}, price: {}, currency: {}, extra: {}",
                jwt.getPayload().getUsername(),
                jwt.getPayload().getAppId(),
                order,
                sku.getSku(),
                req.gameRoleId,
                req.serverId,
                req.price,
                req.currencyCode,
                req.cparam);
        return new ResponseEntity<PayRsp>(rsp, HttpStatus.OK);
    }

    private AppleIapReceipt doIapVerify(String receiptData, boolean sandbox) {

        String VERIFY_URL = "https://buy.itunes.apple.com/verifyReceipt";
        if (sandbox)
            VERIFY_URL = "https://sandbox.itunes.apple.com/verifyReceipt";

        //JsonObject receiptJsonObj = new JsonObject();
        //receiptJsonObj.put("receipt-data", receiptData);
        //receiptJsonObj.put("password", "");

        String verifyBody = "{\"receipt-data\" : \"" + receiptData + "\"}";

        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json; charset=UTF-8");
        headers.put("Accept", "application/json");
        try {

            HttpUtils.HttpResp httpResp = httpUtils.post(VERIFY_URL, verifyBody, headers);
            if (httpResp.code == 200) {
                JsonNode root = objectMapper.readTree(httpResp.body);
                if (root.get("status").asInt() == 0)
                    return objectMapper.readValue(httpResp.body, AppleIapReceipt.class);
                else {
                    AppleIapReceipt appleIapReceipt = new AppleIapReceipt();
                    appleIapReceipt.status = root.get("status").asInt();
                    return appleIapReceipt;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
