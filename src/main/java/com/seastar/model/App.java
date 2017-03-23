package com.seastar.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by osx on 16/12/12.
 */
@Entity
@Table(name = "app")
public class App {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "key", nullable = false, length = 80)
    private String key;

    @Column(name = "secret", nullable = false, length = 80)
    private String secret;

    @Column(name = "status")
    private Integer status;

    @Column(name = "pay_type")
    private Integer payType;

    @Column(name = "notify_url", length = 200)
    private String notifyUrl;

    @Column(name = "create_time", columnDefinition="DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;

    public App() {

    }

    public App(Integer id, String name, String key, String secret, Integer status, Integer payType, String notifyUrl, Date createTime) {
        this.id = id;
        this.name = name;
        this.key = key;
        this.secret = secret;
        this.status = status;
        this.payType = payType;
        this.notifyUrl = notifyUrl;
        this.createTime = createTime;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public Integer getStatus() {
        return status;
    }

    public Integer getPayType() {
        return payType;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public Date getCreateTime() {
        return createTime;
    }
}
