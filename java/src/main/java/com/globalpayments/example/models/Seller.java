package com.globalpayments.example.models;

public class Seller {
    private String id;
    private String name;
    private double platformFeeRate;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPlatformFeeRate() {
        return platformFeeRate;
    }

    public void setPlatformFeeRate(double platformFeeRate) {
        this.platformFeeRate = platformFeeRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
