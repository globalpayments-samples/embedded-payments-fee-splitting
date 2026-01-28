package com.globalpayments.example.models;

public class SplitDetails {
    private double amount;
    private double platformFee;
    private double platformFeeRate;
    private double sellerPayout;
    private String sellerId;
    private String sellerName;
    private String transactionId;
    private String splitTransactionId;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getSplitTransactionId() {
        return splitTransactionId;
    }

    public void setSplitTransactionId(String splitTransactionId) {
        this.splitTransactionId = splitTransactionId;
    }
}
