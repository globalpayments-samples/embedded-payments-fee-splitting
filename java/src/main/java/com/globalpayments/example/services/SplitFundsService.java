package com.globalpayments.example.services;

import com.global.api.entities.Transaction;
import com.global.api.services.ProPayService;

/**
 * SplitFunds Service
 *
 * Handles ProPay SplitFunds transactions to transfer funds from
 * the platform's ProPay account to the seller's ProPay account
 * after a successful payment.
 *
 * @author Global Payments
 * @version 1.0
 */
public class SplitFundsService {
    private final String platformAccountNumber;

    /**
     * Creates a new SplitFundsService instance
     */
    public SplitFundsService() {
        String envValue = System.getenv("PLATFORM_PROPAY_ACCOUNT");
        this.platformAccountNumber = envValue != null ? envValue : "718580391";
    }

    /**
     * Executes a SplitFunds transaction to transfer funds to the seller
     *
     * @param sellerProPayAccount The seller's ProPay account number
     * @param amount The amount to transfer
     * @param transactionId The original payment transaction ID
     * @return SplitFundsResult with success status, transaction number, and error message
     */
    public SplitFundsResult executeSplit(String sellerProPayAccount, double amount, String transactionId) {
        try {
            Transaction response = ProPayService.splitFunds()
                .withAccountNumber(platformAccountNumber)
                .withReceivingAccountNumber(sellerProPayAccount)
                .withAmount(String.format("%.2f", amount))
                .withGlobaltransId(transactionId)
                .withGlobalTransSource("portico")
                .execute();

            boolean success = "00".equals(response.getResponseCode());
            String transNum = null;
            if (response.getPayFacData() != null) {
                transNum = response.getPayFacData().getTransNum();
            }

            return new SplitFundsResult(success, transNum, null);
        } catch (Exception e) {
            return new SplitFundsResult(false, null, e.getMessage());
        }
    }

    /**
     * Result object for SplitFunds operations
     */
    public static class SplitFundsResult {
        public final boolean success;
        public final String transNum;
        public final String errorMessage;

        public SplitFundsResult(boolean success, String transNum, String errorMessage) {
            this.success = success;
            this.transNum = transNum;
            this.errorMessage = errorMessage;
        }
    }
}
