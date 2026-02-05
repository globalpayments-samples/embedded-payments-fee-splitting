package com.globalpayments.example.services;

import com.globalpayments.example.models.SplitDetails;

/**
 * Calculates embedded payments fee splits for transactions.
 *
 * <p>Handles calculation of processing fees, platform fees, and seller payouts
 * following commission structure.</p>
 *
 * <p>Processing Fee: 2.9% + $0.30 (standard payment processing)</p>
 * <p>Platform Fee: Configurable percentage (5-25%)</p>
 * <p>Seller Payout: Transaction amount minus both fees</p>
 *
 * @author Global Payments
 * @version 1.0
 */
public class SplitCalculator {
    /**
     * Processing fee rate as decimal (2.9%).
     * Applied as a percentage of the transaction amount.
     */
    private static final double PROCESSING_FEE_RATE = 0.029;

    /**
     * Fixed processing fee in dollars ($0.30).
     * Applied to every transaction in addition to the percentage rate.
     */
    private static final double PROCESSING_FEE_FIXED = 0.30;

    private final double platformFeeRate;

    /**
     * Creates a new SplitCalculator with specified platform fee rate.
     *
     * @param platformFeeRate Platform commission percentage (must be 5-25)
     * @throws IllegalArgumentException if rate is outside valid range
     */
    public SplitCalculator(double platformFeeRate) {
        if (platformFeeRate < 5 || platformFeeRate > 25) {
            throw new IllegalArgumentException("Platform fee must be between 5% and 25%");
        }
        this.platformFeeRate = platformFeeRate;
    }

    /**
     * Calculates fee distribution for a transaction.
     *
     * <p>Computes the processing fee (2.9% + $0.30), platform fee
     * (configurable percentage), and seller payout (remaining amount).
     * All calculations are rounded to two decimal places.</p>
     *
     * @param amount Transaction amount in dollars
     * @return SplitDetails object containing all fee calculations
     * @throws IllegalArgumentException if amount is less than $0.50
     */
    public SplitDetails calculateSplit(double amount) {
        if (amount < 0.50) {
            throw new IllegalArgumentException("Amount must be at least $0.50");
        }

        double processingFee = Math.round(((amount * PROCESSING_FEE_RATE) + PROCESSING_FEE_FIXED) * 100.0) / 100.0;
        double platformFee = Math.round((amount * (platformFeeRate / 100)) * 100.0) / 100.0;
        double sellerPayout = Math.round((amount - processingFee - platformFee) * 100.0) / 100.0;

        double total = processingFee + platformFee + sellerPayout;
        if (Math.abs(total - amount) > 0.01) {
            sellerPayout = Math.round((amount - processingFee - platformFee) * 100.0) / 100.0;
        }

        SplitDetails details = new SplitDetails();
        details.setAmount(amount);
        details.setProcessingFee(processingFee);
        details.setProcessingFeeRate(PROCESSING_FEE_RATE * 100);
        details.setProcessingFeeFixed(PROCESSING_FEE_FIXED);
        details.setPlatformFee(platformFee);
        details.setPlatformFeeRate(platformFeeRate);
        details.setSellerPayout(sellerPayout);

        return details;
    }
}
