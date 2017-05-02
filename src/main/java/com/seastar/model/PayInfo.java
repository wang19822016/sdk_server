package com.seastar.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by osx on 16/12/13.
 */
@Entity
@Table(name = "pay_info", indexes = {
        @Index(name = "single_pay_index", columnList = "order"),
        @Index(name = "multi_pay_index", columnList = "channel_order,channel_type")
})
public class PayInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "`id`")
    private Long id;

    @Column(name = "`order`", nullable = false, length = 60)
    private String order;

    @Column(name = "app_id")
    private Integer appId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "customer_id", nullable = false, length = 48)
    private String customerId;

    @Column(name = "server_id", nullable = false, length = 16)
    private String serverId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "price", nullable = false, length = 24)
    private String price;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency;

    @Column(name = "currency_used", nullable = false, length = 8)
    private String currency_used;

    @Column(name = "channel_type")
    private Integer channelType;

    @Column(name = "channel_order", nullable = false, length = 60)
    private String channelOrder;

    @Column(name = "create_time", columnDefinition = "DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name = "notify_time", columnDefinition = "DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date notifyTime;

    @Column(name = "sandbox")
    private Integer sandbox;

    @Column(name = "extra", nullable = true, length = 200)
    private String extra;

    @Override
    public String toString() {
        return "{" +
                "id=" + id +
                ", order='" + order + '\'' +
                ", appId=" + appId +
                ", userId=" + userId +
                ", customerId='" + customerId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", status=" + status +
                ", sku='" + sku + '\'' +
                ", price='" + price + '\'' +
                ", currency='" + currency + '\'' +
                ", currency_used='" + currency_used + '\'' +
                ", channelType=" + channelType +
                ", channelOrder='" + channelOrder + '\'' +
                ", createTime=" + createTime +
                ", notifyTime=" + notifyTime +
                ", sandbox=" + sandbox +
                ", extra='" + extra + '\'' +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCurrency_used() {
        return currency_used;
    }

    public void setCurrency_used(String currency_used) {
        this.currency_used = currency_used;
    }

    public Integer getChannelType() {
        return channelType;
    }

    public void setChannelType(Integer channelType) {
        this.channelType = channelType;
    }

    public String getChannelOrder() {
        return channelOrder;
    }

    public void setChannelOrder(String channelOrder) {
        this.channelOrder = channelOrder;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getNotifyTime() {
        return notifyTime;
    }

    public void setNotifyTime(Date notifyTime) {
        this.notifyTime = notifyTime;
    }

    public Integer getSandbox() {
        return sandbox;
    }

    public void setSandbox(Integer sandbox) {
        this.sandbox = sandbox;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
