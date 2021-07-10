package com.binecy.bean;


import org.springframework.data.annotation.Id;

public class DeliveryCompany {
    @Id
    private long id;
    private String name;
//    private String label;

    public DeliveryCompany() {
    }

    public DeliveryCompany(long id, String name) {
        this.id = id;
        this.name = name;
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
}
