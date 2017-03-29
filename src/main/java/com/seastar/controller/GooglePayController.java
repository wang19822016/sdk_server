package com.seastar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.async.PushTask;
import com.seastar.common.Const;
import com.seastar.config.annotation.Authorization;
import com.seastar.config.annotation.Token;
import com.seastar.dao.AppDao;
import com.seastar.dao.AppGoogleDao;
import com.seastar.dao.PayDao;
import com.seastar.dao.SkuDao;
import com.seastar.entity.GoogleIabReq;
import com.seastar.entity.GooglePurchase;
import com.seastar.entity.PayRsp;
import com.seastar.model.*;
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

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Created by osx on 17/3/15.
 */
/*
    {
       "orderId":"GPA.1234-5678-9012-34567",
       "packageName":"com.example.app",
       "productId":"exampleSku",
       "purchaseTime":1345678900000,
       "purchaseState":0,
       "developerPayload":"bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ",
       "purchaseToken":"opaque-token-up-to-1000-characters"
     }
     通过payload添加productid，增加安全性
*/
@RestController
public class GooglePayController {

    @Autowired
    private AppDao appRepo;

    @Autowired
    private AppGoogleDao appGoogleRepo;

    @Autowired
    private PayDao payRepo;

    @Autowired
    private SkuDao skuRepo;

    @Autowired
    private PushTask pushTask;

    private Logger logger = LoggerFactory.getLogger(GooglePayController.class);
    private ObjectMapper mapper = new ObjectMapper();

    @Authorization
    @RequestMapping(value = "/api/pay/google", method = RequestMethod.POST)
    public ResponseEntity<PayRsp> onGooglePay(@Token JWT jwt, @RequestBody GoogleIabReq req) {
        App app = appRepo.findOne(jwt.getPayload().getAppId());
        // 验证数据的可靠性
        if (app == null) {
            logger.error("没有应用， username: {}, appId: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        AppGoogle appGoogle = appGoogleRepo.findOne(2);//jwt.getPayload().getAppId());
        if (appGoogle == null || appGoogle.getKey().isEmpty()) {
            logger.error("没有Google Key， username: {}, appId: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        // 提取谷歌订单
        // 注意：测试环境下orderid不存在
        String signedData = Utils.b64decode(req.googleOriginalJson);
        GooglePurchase purchase = null;
        try {
            purchase = mapper.readValue(signedData, GooglePurchase.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (purchase == null) {
            logger.error("上传的google json错误， username: {}, appId: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        // 验证
        boolean result = verifyGoogleIap(appGoogle.getKey(), signedData, req.googleSignature);
        if (!result) {
            logger.error("google验证失败， username: {}, appId: {}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        Sku sku = skuRepo.findOne(jwt.getPayload().getAppId(), purchase.productId);
        if (sku == null) {
            logger.error("没有配置商品， username: {}, appId: {}, sku: {}, origin:{}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), purchase.productId, req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.BAD_REQUEST);
        }

        String order = payRepo.getGoogleOrder();

        // 生成订单
        PayInfo payInfo = new PayInfo();
        payInfo.setSku(sku.getSku());
        payInfo.setUserId(jwt.getPayload().getUserId());
        payInfo.setAppId(jwt.getPayload().getAppId());
        payInfo.setOrder(order);
        payInfo.setChannelOrder(purchase.orderId);
        payInfo.setChannelType(Const.PAY_CHANNEL_GOOGLE);
        payInfo.setCustomerId(req.gameRoleId);
        payInfo.setServerId(req.serverId);
        payInfo.setPrice(sku.getPrice());
        payInfo.setCurrency(sku.getCurrency());
        payInfo.setCurrency_used(req.currencyCode);
        payInfo.setSandbox(purchase.orderId.isEmpty() ? Const.SANDBOX : Const.PRODUCTION);
        payInfo.setExtra(Utils.b64encode(req.cparam));
        payInfo.setCreateTime(new Date());
        payInfo.setStatus(0);
        if (!payRepo.save(payInfo)) {
            logger.error("入库失败，不是重复提交就是数据库有问题， username: {}, appId: {}, order:{}, sku: {}, origin: {}",
                    jwt.getPayload().getUsername(),
                    jwt.getPayload().getAppId(),
                    order,
                    sku.getSku(),
                    req.toString());
            return new ResponseEntity<PayRsp>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (!app.getNotifyUrl().isEmpty()) {
            logger.error("没有配置通知地址, username: {}, appId: {}, order:{}, origin: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), order, req.toString());
            pushTask.submit(app.getNotifyUrl(), payInfo, app.getSecret());
        }

        PayRsp rsp = new PayRsp();
        rsp.order = order;
        rsp.sku = purchase.productId;

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

    /**
     * 根据游戏的public key验证支付时从Google Market返回的signedData与signature的值是否对应
     *
     * @param base64key
     *            ：配置在Google Play开发者平台上的公钥
     * @param originalJson
     *            ：支付成功时响应的物品信息
     * @param signature
     *            ：已加密后的签名
     * @return boolean：true 验证成功<br/>
     *         false 验证失败
     */
    private boolean verifyGoogleIap(String base64key, String originalJson, String signature) {
        try {
            // 解密出验证key
            byte[] decodedKey = Base64.getDecoder().decode(base64key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));

            // 验证票据
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(publicKey);
            sig.update(originalJson.getBytes());

            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (InvalidKeyException ex) {
            ex.printStackTrace();
        } catch (SignatureException ex) {
            ex.printStackTrace();
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
