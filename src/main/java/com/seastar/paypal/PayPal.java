package com.seastar.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seastar.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by osx on 16/12/8.
 */
public class PayPal {
    private ObjectMapper objectMapper = new ObjectMapper();
    private HttpUtils httpUtils = new HttpUtils();
    private Logger logger = LoggerFactory.getLogger(PayPal.class);

    public PayPalToken refreshToken(String url, String clientId, String secret) {
        try {
            String authString = clientId + ":" + secret;
            byte[] authEncBytes = Base64.getEncoder().encode(authString.getBytes("UTF-8"));
            String authEncString = new String(authEncBytes);

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Accept-Language", "en_US");
            headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            headers.put("Authorization", "Basic " + authEncString);

            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "client_credentials");

            HttpUtils.HttpResp resp = httpUtils.post(url, body, headers);
            logger.info("url: {}, httpCode: {}, httpBody: {}", url, resp.code, resp.body);
            if (resp.code == 200) {
                return objectMapper.readValue(resp.body, PayPalToken.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Payment createPayment(String url,
                                 String token_type,
                                 String access_token,
                                 String returnUrl,
                                 String cancelUrl,
                                 String itemSku,
                                 String itemName,
                                 String itemPrice,
                                 String itemCurrency,
                                 String itemDescription,
                                 String description,
                                 String invoiceNumber,
                                 String custom) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Accept-Language", "en_US");
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("Authorization", token_type + " " + access_token);

        Map<String, Object> root = new HashMap<>();
        root.put("intent", "sale");

        Map<String, Object> redirect_urls = new HashMap<>();
        redirect_urls.put("return_url", returnUrl);
        redirect_urls.put("cancel_url", cancelUrl);
        root.put("redirect_urls", redirect_urls);

        Map<String, Object> payer = new HashMap<>();
        payer.put("payment_method", "paypal");
        root.put("payer", payer);

        ArrayList<Object> transactions = new ArrayList<>();
        root.put("transactions", transactions);
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("description", description);
        transaction.put("invoice_number", invoiceNumber);
        transaction.put("custom", custom);
        transactions.add(transaction);

        Map<String, Object> amount = new HashMap<>();
        amount.put("total", itemPrice);
        amount.put("currency", itemCurrency);

        Map<String, Object> details = new HashMap<>();
        details.put("subtotal", itemPrice);
        amount.put("details", details);
        transaction.put("amount", amount);

        Map<String, Object> item_list = new HashMap<>();
        transaction.put("item_list", item_list);

        ArrayList<Object> items = new ArrayList<>();
        item_list.put("items", items);

        Map<String, Object> item = new HashMap<>();
        item.put("quantity", "1");
        item.put("name", itemName);
        item.put("price", itemPrice);
        item.put("currency", itemCurrency);
        item.put("description", itemDescription);
        item.put("sku", itemSku);
        items.add(item);

        try {
            String bodyStr = objectMapper.writeValueAsString(root);
            HttpUtils.HttpResp resp = httpUtils.post(url, bodyStr, headers);
            logger.info("url: {}, postBody: {}, httpCode: {}, httpBody: {}", url, bodyStr, resp.code, resp.body);
            if (resp.code == 201 || resp.code == 200) {
                return parsePayment(resp.body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Payment getPaymentDetails(String url, String token_type, String access_token) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Accept-Language", "en_US");
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("Authorization", token_type + " " + access_token);

        try {
            HttpUtils.HttpResp resp = httpUtils.get(url, headers);
            logger.info("url: {}, httpCode: {}, httpBody: {}", url, resp.code, resp.body);
            if (resp.code == 200) {
                return parsePayment(resp.body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Payment executePayment(String url, String payerId, String token_type, String access_token) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Accept-Language", "en_US");
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("Authorization", token_type + " " + access_token);

        Map<String, String> body = new HashMap<>();
        body.put("payer_id", payerId);

        try {
            HttpUtils.HttpResp resp = httpUtils.post(url, objectMapper.writeValueAsString(body), headers);
            logger.info("url: {}, postBody: {}, httpCode: {}, httpBody: {}", url, objectMapper.writeValueAsString(body), resp.code, resp.body);
            if (resp.code == 200) {
                return parsePayment(resp.body);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Payment parsePayment(String source) throws IOException {
        Payment payment = new Payment();
        payment.setSource(source);

        JsonNode rootNode = objectMapper.readTree(source);
        if (rootNode.has("id"))
            payment.setId(rootNode.get("id").asText());
        if (rootNode.has("state"))
            payment.setState(rootNode.get("state").asText());
        if (rootNode.has("payer")) {
            JsonNode payerNode = rootNode.get("payer");
            if (payerNode.has("status"))
                payment.setPayerStatus(payerNode.get("status").asText());
            if (payerNode.has("payer_info")) {
                JsonNode payerInfoNode = payerNode.get("payer_info");
                if (payerInfoNode.has("payer_id"))
                    payment.setPayerId(payerInfoNode.get("payer_id").asText());
            }
        }
        if (rootNode.has("transactions")) {
            JsonNode transactionsNode = rootNode.get("transactions");
            if (transactionsNode.isArray() && transactionsNode.size() > 0) {
                JsonNode transactionNode = transactionsNode.get(0);

                if (transactionNode.has("amount")) {
                    JsonNode amountNode = transactionNode.get("amount");
                    if (amountNode.has("total"))
                        payment.setAmountTotal(amountNode.get("total").asText());
                    if (amountNode.has("currency"))
                        payment.setCurrency(amountNode.get("currency").asText());
                    if (amountNode.has("details")) {
                        JsonNode detailsNode = amountNode.get("details");
                        if (detailsNode.has("subtotal"))
                            payment.setAmountSubTotal(detailsNode.get("subtotal").asText());
                    }
                }

                if (transactionNode.has("item_list")) {
                    JsonNode itemListNode = transactionNode.get("item_list");
                    if (itemListNode.has("items")) {
                        JsonNode itemsNode = itemListNode.get("items");
                        if (itemsNode.isArray() && itemsNode.size() > 0) {
                            JsonNode itemNode = itemsNode.get(0);
                            if (itemNode.has("sku"))
                                payment.setSku(itemNode.get("sku").asText());
                            if (itemNode.has("name"))
                                payment.setItemName(itemNode.get("name").asText());
                        }
                    }
                }

                if (transactionNode.has("custom"))
                    payment.setCustom(transactionNode.get("custom").asText());
                if (transactionNode.has("invoice_number"))
                    payment.setInvoiceNumber(transactionNode.get("invoice_number").asText());
            }
        }

        if (rootNode.has("links")) {
            JsonNode linksNode = rootNode.get("links");
            if (linksNode.isArray()) {
                for (int i = 0; i < linksNode.size(); i++) {
                    JsonNode linkNode = linksNode.get(i);
                    if (linkNode.has("rel")) {
                        if (linkNode.get("rel").asText().equals("self"))
                            payment.setPayemntDetailsUrl(linkNode.get("href").asText());
                        else if (linkNode.get("rel").asText().equals("execute"))
                            payment.setPaymentExecuteUrl(linkNode.get("href").asText());
                        else if (linkNode.get("rel").asText().equals("approval_url"))
                            payment.setPaymentApprovalUrl(linkNode.get("href").asText());
                    }
                }
            }
        }

        return payment;
    }
}
