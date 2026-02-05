/**
 * Embedded Payments Processing Application
 *
 * This Express application demonstrates embedded payments processing with fee splitting
 * using the Global Payments SDK. It handles card data from the frontend, validates seller
 * information, and processes payments with automatic fee split calculation.
 */

import express from 'express';
import * as dotenv from 'dotenv';
import {
    ServicesContainer,
    GpApiConfig,
    Address,
    CreditCardData,
    ApiError,
    Channel,
    Environment
} from 'globalpayments-api';
import multer from 'multer';
import SellerManager from './lib/SellerManager.js';
import SplitCalculator from './lib/SplitCalculator.js';

// Load environment variables from .env file
dotenv.config();

/**
 * Initialize Express application with necessary middleware
 */
const app = express();
const port = process.env.PORT || 8000;

app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Configure multer for multipart/form-data parsing
const upload = multer();

// Configure Global Payments SDK with credentials and settings
const config = new GpApiConfig();
config.appId = process.env.GP_APP_ID;
config.appKey = process.env.GP_APP_KEY;
config.environment = process.env.GP_API_ENVIRONMENT === 'PRODUCTION'
    ? 'production'
    : 'test';
config.channel = Channel.CardNotPresent;
config.country = 'US';
ServicesContainer.configureService(config);

/**
 * Utility function to sanitize postal code
 */
const sanitizePostalCode = (postalCode) => {
    if (!postalCode) return '';
    return postalCode.replace(/[^a-zA-Z0-9-]/g, '').slice(0, 10);
};

/**
 * Embedded payments processing endpoint with fee splitting
 */
app.post('/process-embedded-payments-payment', upload.none(), async (req, res) => {
    try {
        const {
            card_name,
            card_number,
            card_expiry,
            card_cvv,
            billing_zip,
            amount,
            seller_id,
            platform_fee_rate
        } = req.body;

        // Validate required fields
        if (!card_name || !card_number || !card_expiry || !card_cvv ||
            !billing_zip || !amount || !seller_id) {
            return res.status(400).json({
                success: false,
                message: 'Missing required fields'
            });
        }

        const amountNum = parseFloat(amount);
        if (amountNum < 0.50) {
            return res.status(400).json({
                success: false,
                message: 'Amount must be at least $0.50'
            });
        }

        // Validate seller
        if (!SellerManager.isValidSeller(seller_id)) {
            return res.status(400).json({
                success: false,
                message: 'Invalid seller selected'
            });
        }

        const seller = SellerManager.getSellerById(seller_id);
        const platformFeeRate = platform_fee_rate ? parseFloat(platform_fee_rate) : 10.0;

        // Calculate split
        const calculator = new SplitCalculator(platformFeeRate);
        const splitDetails = calculator.calculateSplit(amountNum);
        splitDetails.sellerId = seller_id;
        splitDetails.sellerName = seller.name;

        // Parse expiry date (MM/YY format)
        const expiryParts = card_expiry.split('/');
        if (expiryParts.length !== 2) {
            return res.status(400).json({
                success: false,
                message: 'Invalid expiry date format. Use MM/YY'
            });
        }

        const expiryMonth = expiryParts[0].padStart(2, '0');
        const expiryYear = '20' + expiryParts[1];

        // Initialize payment data with card details
        const card = new CreditCardData();
        card.cardHolderName = card_name;
        card.number = card_number.replace(/\s/g, '');
        card.expMonth = expiryMonth;
        card.expYear = expiryYear;
        card.cvn = card_cvv;

        const address = new Address();
        address.postalCode = sanitizePostalCode(billing_zip);

        // Process payment
        const response = await card.charge(amountNum)
            .withAllowDuplicates(true)
            .withCurrency('USD')
            .withAddress(address)
            .execute();

        if (!response || (response.responseCode !== '00' && response.responseCode !== 'SUCCESS')) {
            return res.status(400).json({
                success: false,
                message: 'Payment processing failed',
                error: {
                    code: 'PAYMENT_DECLINED',
                    details: response?.responseMessage || 'Transaction declined'
                }
            });
        }

        res.json({
            success: true,
            message: `Payment successful! Transaction ID: ${response.transactionId}`,
            data: {
                transactionId: response.transactionId,
                amount: amountNum,
                currency: 'USD',
                splitDetails
            }
        });
    } catch (error) {
        if (error instanceof ApiError || error.name === 'ApiError') {
            return res.status(400).json({
                success: false,
                message: 'Payment processing failed',
                error: {
                    code: 'API_ERROR',
                    details: error.message
                }
            });
        }

        res.status(500).json({
            success: false,
            message: 'Internal server error',
            error: {
                code: 'SERVER_ERROR',
                details: error.message
            }
        });
    }
});

// Serve static files - MUST come after API routes
app.use(express.static('.'));

// Start the server
app.listen(port, '0.0.0.0', () => {
    console.log(`Server running at http://localhost:${port}`);
});
