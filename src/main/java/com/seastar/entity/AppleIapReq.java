package com.seastar.entity;

/**
 * Created by wjl on 2016/5/18.
 */
public class AppleIapReq {
    public String transactionId = "";
    public String productId = "";
    public String receipt = "";
    public String gameRoleId = "";
    public String serverId = "";
    public String cparam = "";
    public String price = "";
    public String currencyCode = "";

    @Override
    public String toString() {
        return "AppleIapReq{" +
                "transactionId='" + transactionId + '\'' +
                ", productId='" + productId + '\'' +
                ", receipt='" + receipt + '\'' +
                ", gameRoleId='" + gameRoleId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", cparam='" + cparam + '\'' +
                ", price='" + price + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
}
