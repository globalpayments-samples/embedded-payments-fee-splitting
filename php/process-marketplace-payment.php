<?php

declare(strict_types=1);

/**
 * Marketplace Payment Processing Script
 *
 * This script demonstrates marketplace payment processing with fee splitting
 * using the Global Payments SDK (GP API). It handles tokenized card data from
 * the Drop-In UI, validates seller information, and processes payments with
 * automatic fee split calculation.
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   GlobalPayments_Marketplace
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */

require_once 'vendor/autoload.php';

use Dotenv\Dotenv;
use GlobalPayments\Api\Entities\Exceptions\ApiException;
use GlobalPayments\Api\PaymentMethods\CreditCardData;
use GlobalPayments\Api\ServiceConfigs\Gateways\GpApiConfig;
use GlobalPayments\Api\ServicesContainer;
use GlobalPayments\Api\Entities\Enums\Environment;
use GlobalPayments\Api\Entities\Enums\Channel;
use MarketplaceFee\SellerManager;
use MarketplaceFee\SplitCalculator;

ini_set('display_errors', '0');

/**
 * Configure the SDK with GP API credentials
 *
 * Sets up the Global Payments SDK with GP API configuration including
 * app credentials, environment settings, and channel specification.
 *
 * @return void
 */
function configureSdk(): void
{
    $dotenv = Dotenv::createImmutable(__DIR__);
    $dotenv->load();

    $config = new GpApiConfig();
    $config->appId = $_ENV['GP_APP_ID'];
    $config->appKey = $_ENV['GP_APP_KEY'];
    $config->environment = $_ENV['GP_API_ENVIRONMENT'] === 'PRODUCTION'
        ? Environment::PRODUCTION
        : Environment::TEST;
    $config->channel = Channel::CardNotPresent;
    $config->country = 'US';

    ServicesContainer::configureService($config);
}

// Initialize SDK configuration
configureSdk();

// Set response content type to JSON
header('Content-Type: application/json');

try {
    // Validate required fields
    if (!isset($_POST['payment_reference'], $_POST['amount'], $_POST['seller_id'])) {
        throw new ApiException('Missing required fields');
    }

    // Parse and validate amount
    $amount = floatval($_POST['amount']);
    if ($amount < 0.50) {
        throw new ApiException('Amount must be at least $0.50');
    }

    // Validate seller
    $sellerId = $_POST['seller_id'];
    if (!SellerManager::isValidSeller($sellerId)) {
        throw new ApiException('Invalid seller selected');
    }

    $seller = SellerManager::getSellerById($sellerId);

    // Calculate fee split
    $platformFeeRate = floatval($_POST['platform_fee_rate'] ?? 10.0);
    $calculator = new SplitCalculator($platformFeeRate);
    $splitDetails = $calculator->calculateSplit($amount);

    // Add seller information to split details
    $splitDetails['sellerId'] = $sellerId;
    $splitDetails['sellerName'] = $seller['name'];

    // Initialize payment data using tokenized card from Drop-In UI
    $card = new CreditCardData();
    $card->token = $_POST['payment_reference'];

    // Process the payment transaction with specified amount
    $response = $card->charge($amount)
        ->withCurrency('USD')
        ->execute();

    // Verify transaction was successful
    // GP API can return either 'SUCCESS' or '00' as success code
    if ($response->responseCode !== 'SUCCESS' && $response->responseCode !== '00') {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Payment processing failed',
            'error' => [
                'code' => 'PAYMENT_DECLINED',
                'details' => $response->responseMessage
            ]
        ]);
        exit;
    }

    // Return success response with transaction ID and split details
    echo json_encode([
        'success' => true,
        'message' => 'Payment successful! Transaction ID: ' . $response->transactionId,
        'data' => [
            'transactionId' => $response->transactionId,
            'amount' => $amount,
            'currency' => 'USD',
            'splitDetails' => $splitDetails
        ]
    ]);
} catch (ApiException $e) {
    // Handle payment processing errors
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Payment processing failed',
        'error' => [
            'code' => 'API_ERROR',
            'details' => $e->getMessage()
        ]
    ]);
} catch (\Exception $e) {
    // Handle general errors
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Internal server error',
        'error' => [
            'code' => 'SERVER_ERROR',
            'details' => $e->getMessage()
        ]
    ]);
}
