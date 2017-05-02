package com.seastar.entity;

/**
 * Created by osx on 17/3/14.
 */
public class UserReq {
    private int appId = 0;
    private int type = 0; // 0-seastar, 1-guest, 2-google, 3-gamecenter, 4-facebook
    private String email = "";
    private String sign = ""; // md5(appId + type + email + appkey)

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
