/**
 * SplitCalculator - Calculates embedded payment fee splits
 *
 * Handles calculation of platform fees and seller payouts
 * for embedded payment transactions.
 *
 * @class
 */
class SplitCalculator {
    /**
     * Creates a SplitCalculator instance
     *
     * @param {number} platformFeeRate - Platform fee percentage (3-5)
     * @throws {Error} If platform fee is outside valid range
     */
    constructor(platformFeeRate = 3.0) {
        if (platformFeeRate < 3 || platformFeeRate > 5) {
            throw new Error('Platform fee must be between 3% and 5%');
        }
        this.platformFeeRate = platformFeeRate;
    }

    /**
     * Calculates fee split for a transaction
     *
     * @param {number} amount - Transaction amount in dollars
     * @returns {Object} Split details including platform fee and seller payout
     * @throws {Error} If amount is less than $0.50
     */
    calculateSplit(amount) {
        if (amount < 0.50) {
            throw new Error('Amount must be at least $0.50');
        }

        let platformFee = amount * (this.platformFeeRate / 100);
        platformFee = Math.round(platformFee * 100) / 100;

        let sellerPayout = amount - platformFee;
        sellerPayout = Math.round(sellerPayout * 100) / 100;

        return {
            amount,
            platformFee,
            platformFeeRate: this.platformFeeRate,
            sellerPayout
        };
    }
}

export default SplitCalculator;
