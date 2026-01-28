package com.globalpayments.example.services;

import com.globalpayments.example.models.SplitDetails;

/**
 * Calculates embedded payment fee splits for transactions.
 *
 * <p>Handles calculation of platform fees and seller payouts
 * following embedded payments commission structure.</p>
 *
 * <p>Platform Fee: Configurable percentage (3-5%)</p>
 * <p>Seller Payout: Transaction amount minus platform fee</p>
 *
 * @author Global Payments
 * @version 1.0
 */
public class SplitCalculator {
    private final double platformFeeRate;

    /**
     * Creates a new SplitCalculator with specified platform fee rate.
     *
     * @param platformFeeRate Platform commission percentage (must be 3-5)
     * @throws IllegalArgumentException if rate is outside valid range
     */
    public SplitCalculator(double platformFeeRate) {
        if (platformFeeRate < 3 || platformFeeRate > 5) {
            throw new IllegalArgumentException("Platform fee must be between 3% and 5%");
        }
        this.platformFeeRate = platformFeeRate;
    }

    /**
     * Calculates fee distribution for a transaction.
     *
     * <p>Computes the platform fee (configurable percentage) and seller payout
     * (remaining amount). All calculations are rounded to two decimal places.</p>
     *
     * @param amount Transaction amount in dollars
     * @return SplitDetails object containing all fee calculations
     * @throws IllegalArgumentException if amount is less than $0.50
     */
    public SplitDetails calculateSplit(double amount) {
        if (amount < 0.50) {
            throw new IllegalArgumentException("Amount must be at least $0.50");
        }

        double platformFee = Math.round((amount * (platformFeeRate / 100)) * 100.0) / 100.0;
        double sellerPayout = Math.round((amount - platformFee) * 100.0) / 100.0;

        SplitDetails details = new SplitDetails();
        details.setAmount(amount);
        details.setPlatformFee(platformFee);
        details.setPlatformFeeRate(platformFeeRate);
        details.setSellerPayout(sellerPayout);

        return details;
    }
}
