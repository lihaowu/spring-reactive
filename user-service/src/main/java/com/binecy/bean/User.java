package com.binecy.bean;

import java.util.List;

public class User {
    private long id;
    private String name;
    private String label;
    // 收货地址经度
    private Double deliveryAddressLon;
    // 收货地址维度
    private Double deliveryAddressLat;

    private List<Rights> rights;

    public User() {
    }

    public User(long id, String name, String label) {
        this.id = id;
        this.name = name;
        this.label = label;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<Rights> getRights() {
        return rights;
    }

    public void setRights(List<Rights> rights) {
        this.rights = rights;
    }

    public Double getDeliveryAddressLon() {
        return deliveryAddressLon;
    }

    public void setDeliveryAddressLon(Double deliveryAddressLon) {
        this.deliveryAddressLon = deliveryAddressLon;
    }

    public Double getDeliveryAddressLat() {
        return deliveryAddressLat;
    }

    public void setDeliveryAddressLat(Double deliveryAddressLat) {
        this.deliveryAddressLat = deliveryAddressLat;
    }
}
