package com.seastar.entity;

/**
 * Created by os on 16-6-18.
 */
public class MyCardReqAuthCodeReq {
    public String customerId = "";
    public String serverId = "";
    public String itemCode = "";
    public String cparam = "";

    @Override
    public String toString() {
        return "MycardReqAuthCodeReq{" +
                "customerId='" + customerId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", itemCode='" + itemCode + '\'' +
                ", cparam='" + cparam + '\'' +
                '}';
    }
}
