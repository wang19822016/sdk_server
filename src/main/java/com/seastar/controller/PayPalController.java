package com.seastar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.seastar.async.PushTask;
import com.seastar.common.Const;
import com.seastar.config.annotation.Authorization;
import com.seastar.config.annotation.Token;
import com.seastar.dao.AppDao;
import com.seastar.dao.PayDao;
import com.seastar.dao.SkuDao;
import com.seastar.model.App;
import com.seastar.model.PayInfo;
import com.seastar.paypal.PayPalToken;
import com.seastar.paypal.PayPalTransition;
import com.seastar.model.Sku;
import com.seastar.paypal.PayPal;
import com.seastar.paypal.Payment;
import com.seastar.utils.JWT;
import com.seastar.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by osx on 16/12/16.
 */
@Controller
public class PayPalController {
    @Value("${paypal.clientid}")
    private String clientId;

    @Value("${paypal.secret}")
    private String secret;

    @Value("${paypal.url.oauth}")
    private String urlOAuth;

    @Value("${paypal.url.payment.create}")
    private String urlPaymentCreate;

    @Value("${paypal.url.payment.return}")
    private String urlPaymentReturn;

    @Value("${paypal.url.payment.cancel}")
    private String urlPaymentCancel;

    @Autowired
    private AppDao appDao;

    @Autowired
    private PayDao payDao;

    @Autowired
    private SkuDao skuDao;

    @Autowired
    private PushTask pushTask;

    private PayPalToken payPalToken;
    private PayPal payPal = new PayPal();
    private ConcurrentHashMap<String, PayPalTransition> transitions = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(PayPalController.class);

    @Authorization
    @RequestMapping(value = "/api/pay", method = RequestMethod.GET)
    public String index(@Token JWT jwt, @RequestParam(value = "sku") String sku, @RequestParam(value = "customer")
            String customer, @RequestParam(value = "server") String server, @RequestParam(value = "extra") String extra, Model model) {

        switch (jwt.getPayload().getPayType()) {
            case 0x01:
                model.addAttribute("google", true);
                model.addAttribute("mycard", false);
                model.addAttribute("paypal", false);
                break;
            case 0x02:
                model.addAttribute("google", false);
                model.addAttribute("mycard", true);
                model.addAttribute("paypal", false);
                break;
            case 0x03:
                model.addAttribute("google", true);
                model.addAttribute("mycard", false);
                model.addAttribute("paypal", true);
                break;
            case 0x04:
                model.addAttribute("google", true);
                model.addAttribute("mycard", true);
                model.addAttribute("paypal", false);
                break;
            case 0x05:
                model.addAttribute("google", true);
                model.addAttribute("mycard", false);
                model.addAttribute("paypal", true);
                break;
            case 0x06:
                model.addAttribute("google", true);
                model.addAttribute("mycard", true);
                model.addAttribute("paypal", true);
                break;
            case 0x07:
                model.addAttribute("google", false);
                model.addAttribute("mycard", true);
                model.addAttribute("paypal", true);
                break;
        }

        model.addAttribute("sku", sku);
        model.addAttribute("customer", customer);
        model.addAttribute("server", server);
        model.addAttribute("extra", extra);
        model.addAttribute("token", jwt.getToken());
        return "index";
    }

    @RequestMapping(value = "/api/pay/paypal/create")
    public String create(@RequestParam(value = "sku") String sku,
                         @RequestParam(value = "customerId") String customerId,
                         @RequestParam(value = "serverId") String serverId,
                         @RequestParam(value = "extra") String extra,
                         @RequestParam(value = "token") String sdkToken) throws JsonProcessingException {

        JWT jwt = new JWT(sdkToken);
        App app = appDao.findOne(jwt.getPayload().getAppId());
        if (app == null) {
            logger.error("应用不存在, username: {}, appId: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId());
            return "redirect:/api/pay/paypal/fail";
        }
        if (!jwt.check(app.getSecret())) {
            logger.error("sdk token验证失败, username: {}, appId: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId());
            return "redirect:/api/pay/paypal/fail";
        }

        // check payment
        Sku itemSku = skuDao.findOne(jwt.getPayload().getAppId(), sku);
        if (itemSku == null) {
            logger.error("商品不存在, username: {}, appId: {}, sku: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), sku);
            return "redirect:/api/pay/paypal/fail";
        }

        // 刷新paypal的token
        refreshToken();

        Payment payment = payPal.createPayment(urlPaymentCreate,
                payPalToken.getToken_type(),
                payPalToken.getAccess_token(),
                urlPaymentReturn,
                urlPaymentCancel,
                sku,
                itemSku.getName(),
                itemSku.getPrice(),
                itemSku.getCurrency(),
                itemSku.getName(),
                itemSku.getName(),
                payDao.getPaypalOrder(),
                "");

        if (payment != null) {
            PayPalTransition transition = new PayPalTransition();
            transition.setTime(System.currentTimeMillis());
            transition.setAppId(jwt.getPayload().getAppId());
            transition.setUserId(jwt.getPayload().getUserId());
            transition.setCustomerId(customerId);
            transition.setServerId(serverId);
            transition.setCustom(extra);
            transition.setAppSecret(app.getSecret());
            transition.setNotifyUrl(app.getNotifyUrl());
            transition.setCurrency(itemSku.getCurrency());
            transition.setPayment(payment);
            transitions.put(payment.getId(), transition);
        } else {
            logger.error("生成交易失败, username: {}, appId: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId());
            return "redirect:/api/pay/paypal/fail";
        }

        logger.info("生成交易成功, username: {}, appId: {}, sku: {}", jwt.getPayload().getUsername(), jwt.getPayload().getAppId(), sku);
        return "redirect:" + payment.getPaymentApprovalUrl();
    }

    @RequestMapping(value = "/api/pay/paypal/execute")
    public String execute(@RequestParam(value = "paymentId") String paymentId, @RequestParam("token") String token, @RequestParam("PayerID") String payerId, Model model) throws IOException {
        PayPalTransition transition = transitions.get(paymentId);
        if (transition == null) {
            logger.error("交易不存在, paymentId: {}", paymentId);
            return "redirect:/api/pay/paypal/fail";
        }

        refreshToken();

        // 执行payment
        Payment payment = payPal.executePayment(transition.getPayment().getPaymentExecuteUrl(), payerId, payPalToken.getToken_type(), payPalToken.getAccess_token());
        if (payment != null && payment.getPayerId() != null && payment.getState().equals("approved")) {
            transitions.remove(paymentId);

            logger.info("交易成功, paymentId: {}", paymentId);
            saveToDb(transition, payment);

            model.addAttribute("orderid", payment.getInvoiceNumber());
            return "success";

        } else {
            logger.error("执行交易失败, paymentId: {}", paymentId);
            return "redirect:/api/pay/paypal/fail";
        }
    }

    @RequestMapping(value = "/api/pay/paypal/cancel")
    public String cancel() {
        return "cancel";
    }

    @RequestMapping(value = "/api/pay/paypal/fail")
    public String fail() {
        return "fail";
    }

    @Scheduled(fixedDelay = 6000000) // 60分钟遍历一次
    public void scanLeakOrder() {
        List<PayPalTransition> list = new ArrayList<>();

        for (Map.Entry<String, PayPalTransition> entry : transitions.entrySet()) {
            list.add(entry.getValue());
        }

        long currTime = System.currentTimeMillis();

        // 遍历列表
        for (PayPalTransition trans : list) {
            if (currTime - trans.getTime() > 30 * 60 * 1000) {// 超过30分钟没处理的订单需要检查是否是漏单
                // 删除过期数据
                transitions.remove(trans.getPayment().getId());

                // 查询交易详情，交易完成的做补单处理
                if (trans.getPayment().getPayemntDetailsUrl() != null) {
                    Payment payment = payPal.getPaymentDetails(trans.getPayment().getPayemntDetailsUrl(), payPalToken.getToken_type(), payPalToken.getAccess_token());
                    if (payment != null && payment.getPayerId() != null && payment.getPaymentExecuteUrl() != null && !payment.getState().equals("failed")) {
                        Payment payment1 = payPal.executePayment(trans.getPayment().getPaymentExecuteUrl(), payment.getPayerId(), payPalToken.getToken_type(), payPalToken.getAccess_token());
                        if (payment1 != null && payment1.getPayerId() != null && payment.getState().equals("approved")) {
                            saveToDb(trans, payment);
                        }

                    }
                }
            }
        }
    }

    private void saveToDb(PayPalTransition transition, Payment payment) {
        // 存储
        // 生成订单
        PayInfo payInfo = new PayInfo();
        payInfo.setSku(payment.getSku());
        payInfo.setUserId(transition.getUserId());
        payInfo.setAppId(transition.getAppId());
        payInfo.setOrder(payment.getInvoiceNumber());
        payInfo.setChannelOrder(payment.getId() + "|" + payment.getPayerId());
        payInfo.setChannelType(Const.PAY_CHANNEL_PAYPAL);
        payInfo.setCustomerId(transition.getCustomerId());
        payInfo.setServerId(transition.getServerId());
        payInfo.setPrice(payment.getAmountTotal());
        payInfo.setCurrency(transition.getCurrency());
        payInfo.setCurrency_used(payment.getCurrency());
        if (urlPaymentCreate.contains("sandbox"))
            payInfo.setSandbox(1);
        else
            payInfo.setSandbox(0);
        payInfo.setExtra(Utils.b64encode(transition.getCustom()));
        if (!payDao.save(payInfo)) {
            // 提示错误
            logger.error("入库失败，不是重复提交就是数据库有问题， username: {}, appId: {}, order:{}, origin: {}", payInfo.getUserId(), payInfo.getAppId(), payInfo.getOrder(), payInfo.toString());
        }

        // 推送
        if (!transition.getNotifyUrl().isEmpty()) {
            pushTask.submit(transition.getNotifyUrl(), payInfo, transition.getAppSecret());
        }
    }

    private synchronized void refreshToken() {
        if (payPalToken == null || payPalToken.isExpired()) {
            payPalToken = payPal.refreshToken(urlOAuth, clientId, secret);
        }
    }
}
