package com.seastar.model;


import javax.persistence.*;
import java.util.Date;

/**
 * Created by osx on 16/12/10.
 */
@Entity
@Table(name = "sku", indexes = {
        @Index(name = "multi_sku_index", columnList = "app_id,sku", unique = true),
        @Index(name = "single_sku_index", columnList = "app_id")
})
public class Sku {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "`id`")
    private Integer id;

    @Column(name = "app_id")
    private Integer appId;

    @Column(name = "sku", nullable = false, length = 32)
    private String sku;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "price", nullable = false, length = 24)
    private String price;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "platform")
    private Integer platform;

    @Column(name = "create_time", columnDefinition="DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getPlatform() {
        return platform;
    }

    public void setPlatform(Integer platform) {
        this.platform = platform;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
