package com.seastar.paypal;

/**
 * Created by osx on 16/12/9.
 */
public class PayPalToken {
    private String scope;

    // The access token issued by PayPal. After the access token expires (see expires_in), you must request a new access token.
    // Value assigned by PayPal.
    private String access_token;

    // The type of the token issued as described in OAuth2.0 RFC6749, Section 7.1. Value is case insensitive.
    // Value assigned by PayPal.
    private String token_type;

    private String app_id;

    private int expires_in;

    private String nonce;

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public String getApp_id() {
        return app_id;
    }

    public void setApp_id(String app_id) {
        this.app_id = app_id;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(int expires_in) {
        this.expires_in = expires_in;
    }
}
