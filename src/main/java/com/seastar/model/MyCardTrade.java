package com.seastar.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by osx on 17/3/22.
 */
@Entity
@Table(name = "mycard_trade")
public class MyCardTrade {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "payment_type", nullable = false, length = 15)
    private String paymentType;

    @Column(name = "trade_seq", nullable = false, length = 60)
    private String tradeSeq;

    @Column(name = "mycard_trade_no", nullable = false, length = 60)
    private String mycardTradeNo;

    @Column(name = "fac_trade_seq", nullable = false, length = 60)
    private String facTradeSeq;

    @Column(name = "customer_id", nullable = false, length = 60)
    private String customerId;

    @Column(name = "amount", nullable = false, length = 60)
    private String amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "trade_date_time", columnDefinition="DATETIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date tradeDateTime;


    public MyCardTrade() {}

    public MyCardTrade(String paymentType, String tradeSeq, String mycardTradeNo, String facTradeSeq, String customerId, String amount, String currency) {
        this.paymentType = paymentType;
        this.tradeSeq = tradeSeq;
        this.mycardTradeNo = mycardTradeNo;
        this.facTradeSeq = facTradeSeq;
        this.customerId = customerId;
        this.currency = currency;
        this.amount = amount;
        this.tradeDateTime = new Date();
    }

    public Integer getId() {
        return id;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getTradeSeq() {
        return tradeSeq;
    }

    public String getMycardTradeNo() {
        return mycardTradeNo;
    }

    public String getFacTradeSeq() {
        return facTradeSeq;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Date getTradeDateTime() {
        return tradeDateTime;
    }
}
