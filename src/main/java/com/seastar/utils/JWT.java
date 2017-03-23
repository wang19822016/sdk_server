package com.seastar.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by osx on 17/3/11.
 * 一个JWT实际上就是一个字符串，它由三部分组成，头部、载荷与签名。
 */
public class JWT {
    public static class Payload {
        // 该JWT的签发者
        private String iss = "seastar";

        // 在什么时候签发, unix时间戳
        private long iat = 0;

        // 什么时候过期, unix时间戳
        private long exp = 0;

        // 接收该JWT的一方
        private String aud = "www.vrseastar.com";

        // 该jwt面向的用户
        private String sub = "";

        private String from_user = "seastar";
        private String target_user = "all";

        private long userId;
        private String username;
        private int appId;
        private int payType;
        private int loginType;


        public String getIss() {
            return iss;
        }

        public void setIss(String iss) {
            this.iss = iss;
        }

        public long getIat() {
            return iat;
        }

        public void setIat(long iat) {
            this.iat = iat;
        }

        public long getExp() {
            return exp;
        }

        public void setExp(long exp) {
            this.exp = exp;
        }

        public String getAud() {
            return aud;
        }

        public void setAud(String aud) {
            this.aud = aud;
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getFrom_user() {
            return from_user;
        }

        public void setFrom_user(String from_user) {
            this.from_user = from_user;
        }

        public String getTarget_user() {
            return target_user;
        }

        public void setTarget_user(String target_user) {
            this.target_user = target_user;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public int getAppId() {
            return appId;
        }

        public void setAppId(int appId) {
            this.appId = appId;
        }

        public int getPayType() {
            return payType;
        }

        public void setPayType(int payType) {
            this.payType = payType;
        }

        public int getLoginType() {
            return loginType;
        }

        public void setLoginType(int loginType) {
            this.loginType = loginType;
        }
    }

    public static class Header {
        // 类型
        private String typ = "JWT";

        // 签名算法
        private String alg = "HS256";

        public String getTyp() {
            return typ;
        }

        public String getAlg() {
            return alg;
        }
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    private Payload payload = new Payload();
    private String token = "";

    public JWT() {}

    public JWT(long expired, long userId, String username, int appId, int payType, int loginType, String key) {
        payload.setIat(System.currentTimeMillis() / 1000);
        payload.setExp(expired / 1000);
        payload.setSub(username);
        payload.setUserId(userId);
        payload.setUsername(username);
        payload.setAppId(appId);
        payload.setPayType(payType);
        payload.setLoginType(loginType);
        try {
            String payloadStr = objectMapper.writeValueAsString(payload);
            payloadStr = Utils.base64UrlEncode(payloadStr);

            String headerStr = objectMapper.writeValueAsString(new Header());
            headerStr = Utils.base64UrlEncode(headerStr);

            String sign = sign(headerStr + "." + payloadStr, key);

            token = headerStr + "." + payloadStr + "." + sign;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JWT(String token) {
        this.token = token;
        String[] strArray = token.split("\\.");
        if (strArray.length == 3) {
            try {
                payload = objectMapper.readValue(Utils.base64UrlDecode(strArray[1]), Payload.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean check(String key) {
        String[] strArray = token.split("\\.");
        if (strArray.length != 3)
            return false;

        String sign = sign(strArray[0] + "." + strArray[1], key);
        if (!sign.equals(strArray[2]))
            return false;
        return true;
    }

    public Payload getPayload() {
        return payload;
    }

    public String getToken() {
        return token;
    }

    private String sign(String data, String key)
    {
        StringBuilder hs = new StringBuilder();
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(Charset.forName("UTF-8")), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);

            byte[] hmac = mac.doFinal(data.getBytes(Charset.forName("UTF-8")));

            String stmp;
            for (int n = 0; hmac != null && n < hmac.length; n++) {
                stmp = Integer.toHexString(hmac[n] & 0XFF);
                if (stmp.length() == 1)
                    hs.append('0');
                hs.append(stmp);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return hs.toString().toUpperCase();
    }
}
