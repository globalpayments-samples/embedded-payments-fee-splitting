/**
 * Global Payments SDK Template - Node.js
 * 
 * This Express application provides a starting template for Global Payments SDK integration.
 * Customize the endpoints and logic below for your specific use case.
 */

import express from 'express';
import * as dotenv from 'dotenv';
import crypto from 'crypto';
import {
    ServicesContainer,
    GpApiConfig,
    Address,
    CreditCardData,
    ApiError,
    Channel,
    Environment
} from 'globalpayments-api';

// Load environment variables from .env file
dotenv.config();

/**
 * Initialize Express application with necessary middleware
 */
const app = express();
const port = process.env.PORT || 8000;

app.use(express.static('.')); // Serve static files
app.use(express.urlencoded({ extended: true })); // Parse form data
app.use(express.json()); // Parse JSON requests

// Configure Global Payments SDK with credentials and settings
const config = new GpApiConfig();
config.appId = process.env.GP_APP_ID;
config.appKey = process.env.GP_APP_KEY;
config.environment = process.env.GP_ENVIRONMENT === 'production'
    ? 'production'
    : 'test';
config.channel = Channel.CardNotPresent;
config.country = 'US';
ServicesContainer.configureService(config);

/**
 * Utility function to sanitize postal code
 * Customize validation logic as needed for your use case
 */
const sanitizePostalCode = (postalCode) => {
    return postalCode.replace(/[^a-zA-Z0-9-]/g, '').slice(0, 10);
};

/**
 * Config endpoint - provides public API key for client-side use
 * Customize response data as needed
 */
app.get('/config', (req, res) => {
    res.json({
        success: true,
        data: {
            publicApiKey: process.env.PUBLIC_API_KEY
            // Add other configuration data as needed
        }
    });
});

/**
 * Access Token endpoint - generates restricted access tokens for frontend tokenization
 * Used by Drop-In UI for secure client-side card tokenization
 */
app.post('/get-access-token', async (req, res) => {
    try {
        const nonce = crypto.randomBytes(16).toString('hex');
        const secret = crypto.createHash('sha512')
            .update(nonce + process.env.GP_APP_KEY)
            .digest('hex');

        const tokenRequest = {
            app_id: process.env.GP_APP_ID,
            nonce: nonce,
            secret: secret,
            grant_type: 'client_credentials',
            seconds_to_expire: 600,
            permissions: ['PMT_POST_Create_Single']
        };

        const apiEndpoint = process.env.GP_ENVIRONMENT === 'production'
            ? 'https://apis.globalpay.com/ucp/accesstoken'
            : 'https://apis.sandbox.globalpay.com/ucp/accesstoken';

        const response = await fetch(apiEndpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-GP-Version': '2021-03-22'
            },
            body: JSON.stringify(tokenRequest)
        });

        const data = await response.json();

        res.json({
            success: true,
            token: data.token
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

/**
 * Example payment processing endpoint
 * Customize this endpoint for your specific payment flow
 */
app.post('/process-payment', async (req, res) => {
    try {
        // TODO: Add your payment processing logic here
        // Example implementation for basic charge:
        
        if (!req.body.payment_token) {
            throw new Error('Payment token is required');
        }

        const card = new CreditCardData();
        card.token = req.body.payment_token;

        // Customize amount and other parameters as needed
        const amount = req.body.amount || 10.00;

        // Add billing address if needed
        const address = new Address();
        if (req.body.billing_zip) {
            address.postalCode = sanitizePostalCode(req.body.billing_zip);
        }

        const response = await card.charge(amount)
            .withAllowDuplicates(true)
            .withCurrency('USD')
            .withAddress(address)
            .execute();

        // Verify transaction was successful
        if (response.responseCode !== '00' && response.responseCode !== 'SUCCESS') {
            res.status(400).json({
                success: false,
                message: 'Payment processing failed',
                error: {
                    code: 'PAYMENT_DECLINED',
                    details: response.responseMessage
                }
            });
            return;
        }

        res.json({
            success: true,
            message: 'Payment processed successfully',
            data: { transactionId: response.transactionId }
        });

    } catch (error) {
        res.status(500).json({
            success: false,
            message: 'Payment processing failed',
            error: error.message
        });
    }
});

/**
 * Add your custom endpoints here
 * Examples:
 * - app.post('/authorize', ...) // Authorization only
 * - app.post('/capture', ...)   // Capture authorized payment
 * - app.post('/refund', ...)    // Process refund
 * - app.get('/transaction/:id', ...) // Get transaction details
 */

// Start the server
app.listen(port, '0.0.0.0', () => {
    console.log(`Server running at http://localhost:${port}`);
    console.log(`Customize this template for your use case!`);
});