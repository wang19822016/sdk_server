package com.seastar.entity;

/**
 * Created by wjl on 2016/5/16.
 */
public class UserTokenRsp {
    private String access_token = "";
    private long expires_in = 0;
    private String token_type = "";

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public long getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }
}