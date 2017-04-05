package com.seastar.entity;

/**
 * Created by osx on 17/4/5.
 */
public class UserLog {
    private int appId;
    private String deviceid;
    private String androidid;
    private String board;
    private String device;
    private String brand;
    private String manufacturer;
    private String model;
    private String product;
    private String systemversion;
    private String network;

    @Override
    public String toString() {
        return "{" +
                "appId=" + appId +
                ", deviceid='" + deviceid + '\'' +
                ", androidid='" + androidid + '\'' +
                ", board='" + board + '\'' +
                ", device='" + device + '\'' +
                ", brand='" + brand + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", model='" + model + '\'' +
                ", product='" + product + '\'' +
                ", systemversion='" + systemversion + '\'' +
                ", network='" + network + '\'' +
                '}';
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getDeviceid() {
        return deviceid;
    }

    public void setDeviceid(String deviceid) {
        this.deviceid = deviceid;
    }

    public String getAndroidid() {
        return androidid;
    }

    public void setAndroidid(String androidid) {
        this.androidid = androidid;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSystemversion() {
        return systemversion;
    }

    public void setSystemversion(String systemversion) {
        this.systemversion = systemversion;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }
}
