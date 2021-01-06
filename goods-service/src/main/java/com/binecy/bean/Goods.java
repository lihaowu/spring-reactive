package com.binecy.bean;

public class Goods {
    private long id;
    private String name;
    private Integer price;
    private String label;

    public Goods() {
    }

    public Goods(long id, String name, int price, String label) {
        this.id = id;
        this.name = name;
        this.price = price;
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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Goods{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", label='" + label + '\'' +
                '}';
    }
}
