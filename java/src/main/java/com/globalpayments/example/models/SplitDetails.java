package com.globalpayments.example.models;

public class SplitDetails {
    private double amount;
    private double processingFee;
    private double processingFeeRate;
    private double processingFeeFixed;
    private double platformFee;
    private double platformFeeRate;
    private double sellerPayout;
    private String sellerId;
    private String sellerName;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getProcessingFee() {
        return processingFee;
    }

    public void setProcessingFee(double processingFee) {
        this.processingFee = processingFee;
    }

    public double getProcessingFeeRate() {
        return processingFeeRate;
    }

    public void setProcessingFeeRate(double processingFeeRate) {
        this.processingFeeRate = processingFeeRate;
    }

    public double getProcessingFeeFixed() {
        return processingFeeFixed;
    }

    public void setProcessingFeeFixed(double processingFeeFixed) {
        this.processingFeeFixed = processingFeeFixed;
    }

    public double getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(double platformFee) {
        this.platformFee = platformFee;
    }

    public double getPlatformFeeRate() {
        return platformFeeRate;
    }

    public void setPlatformFeeRate(double platformFeeRate) {
        this.platformFeeRate = platformFeeRate;
    }

    public double getSellerPayout() {
        return sellerPayout;
    }

    public void setSellerPayout(double sellerPayout) {
        this.sellerPayout = sellerPayout;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
}
