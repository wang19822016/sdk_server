package com.seastar.paypal;

import java.util.ArrayList;

/**
 * Created by osx on 16/12/9.
 */
public class Payment {
    private String id;
    private String state;
    private String payerId;
    private String payerStatus;
    private String amountTotal;
    private String amountSubTotal;
    private String currency;
    private String sku;
    private String itemName;
    private String custom;
    private String invoiceNumber;
    private String payemntDetailsUrl;
    private String paymentExecuteUrl;
    private String paymentApprovalUrl;

    private String source;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public String getPayerStatus() {
        return payerStatus;
    }

    public void setPayerStatus(String payerStatus) {
        this.payerStatus = payerStatus;
    }

    public String getAmountTotal() {
        return amountTotal;
    }

    public void setAmountTotal(String amountTotal) {
        this.amountTotal = amountTotal;
    }

    public String getAmountSubTotal() {
        return amountSubTotal;
    }

    public void setAmountSubTotal(String amountSubTotal) {
        this.amountSubTotal = amountSubTotal;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getPayemntDetailsUrl() {
        return payemntDetailsUrl;
    }

    public void setPayemntDetailsUrl(String payemntDetailsUrl) {
        this.payemntDetailsUrl = payemntDetailsUrl;
    }

    public String getPaymentExecuteUrl() {
        return paymentExecuteUrl;
    }

    public void setPaymentExecuteUrl(String paymentExecuteUrl) {
        this.paymentExecuteUrl = paymentExecuteUrl;
    }

    public String getPaymentApprovalUrl() {
        return paymentApprovalUrl;
    }

    public void setPaymentApprovalUrl(String paymentApprovalUrl) {
        this.paymentApprovalUrl = paymentApprovalUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
