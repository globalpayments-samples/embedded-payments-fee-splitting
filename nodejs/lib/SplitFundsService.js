import { PayFacService } from 'globalpayments-api';

/**
 * SplitFundsService - Handles ProPay SplitFunds transactions
 *
 * This service transfers funds from the platform's ProPay account
 * to the seller's ProPay account after a successful payment.
 *
 * @class
 */
class SplitFundsService {
    /**
     * Creates a new SplitFundsService instance
     *
     * @constructor
     */
    constructor() {
        this.platformAccountNumber = process.env.PLATFORM_PROPAY_ACCOUNT || '718580391';
    }

    /**
     * Executes a SplitFunds transaction to transfer funds to the seller
     *
     * @param {string} sellerProPayAccount - The seller's ProPay account number
     * @param {number} amount - The amount to transfer
     * @param {string} transactionId - The original payment transaction ID
     * @returns {Promise<Object>} Result object with success, transNum, and errorMessage
     */
    async executeSplit(sellerProPayAccount, amount, transactionId) {
        try {
            const response = await PayFacService.splitFunds()
                .withAccountNumber(this.platformAccountNumber)
                .withReceivingAccountNumber(sellerProPayAccount)
                .withAmount(amount.toFixed(2))
                .withGlobaltransId(transactionId)
                .withGlobalTransSource('portico')
                .execute();

            return {
                success: response.responseCode === '00',
                transNum: response.payFacData?.transNum || null,
                errorMessage: null
            };
        } catch (error) {
            return {
                success: false,
                transNum: null,
                errorMessage: error.message
            };
        }
    }
}

export default SplitFundsService;
