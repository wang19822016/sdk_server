package com.seastar.entity;

/**
 * Created by wjl on 2016/5/17.
 */
public class GoogleIabReq {
    public String googleOriginalJson = "";
    public String googleSignature = "";
    public String gameRoleId = "";
    public String serverId = "";
    public String cparam = "";
    public String price = "";
    public String currencyCode = "";

    @Override
    public String toString() {
        return "GoogleIabReq{" +
                "googleOriginalJson='" + googleOriginalJson + '\'' +
                ", googleSignature='" + googleSignature + '\'' +
                ", gameRoleId='" + gameRoleId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", cparam='" + cparam + '\'' +
                ", price='" + price + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                '}';
    }
}
