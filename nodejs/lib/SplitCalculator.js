/**
 * SplitCalculator - Calculates marketplace fee splits
 *
 * Handles calculation of processing fees, platform fees, and seller payouts
 * for marketplace transactions.
 *
 * @class
 */
class SplitCalculator {
    /**
     * Processing fee rate as decimal (2.9%)
     * @type {number}
     * @static
     * @readonly
     */
    static PROCESSING_FEE_RATE = 0.029;

    /**
     * Fixed processing fee in dollars
     * @type {number}
     * @static
     * @readonly
     */
    static PROCESSING_FEE_FIXED = 0.30;

    /**
     * Creates a SplitCalculator instance
     *
     * @param {number} platformFeeRate - Platform fee percentage (5-25)
     * @throws {Error} If platform fee is outside valid range
     */
    constructor(platformFeeRate = 10.0) {
        if (platformFeeRate < 5 || platformFeeRate > 25) {
            throw new Error('Platform fee must be between 5% and 25%');
        }
        this.platformFeeRate = platformFeeRate;
    }

    /**
     * Calculates fee split for a transaction
     *
     * @param {number} amount - Transaction amount in dollars
     * @returns {Object} Split details including all fees and payout
     * @throws {Error} If amount is less than $0.50
     */
    calculateSplit(amount) {
        if (amount < 0.50) {
            throw new Error('Amount must be at least $0.50');
        }

        let processingFee = (amount * SplitCalculator.PROCESSING_FEE_RATE) + SplitCalculator.PROCESSING_FEE_FIXED;
        processingFee = Math.round(processingFee * 100) / 100;

        let platformFee = amount * (this.platformFeeRate / 100);
        platformFee = Math.round(platformFee * 100) / 100;

        let sellerPayout = amount - processingFee - platformFee;
        sellerPayout = Math.round(sellerPayout * 100) / 100;

        const total = processingFee + platformFee + sellerPayout;
        if (Math.abs(total - amount) > 0.01) {
            sellerPayout = Math.round((amount - processingFee - platformFee) * 100) / 100;
        }

        return {
            amount,
            processingFee,
            processingFeeRate: SplitCalculator.PROCESSING_FEE_RATE * 100,
            processingFeeFixed: SplitCalculator.PROCESSING_FEE_FIXED,
            platformFee,
            platformFeeRate: this.platformFeeRate,
            sellerPayout
        };
    }
}

export default SplitCalculator;
