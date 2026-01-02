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
use GlobalPayments\Api\Entities\Address;
use GlobalPayments\Api\Entities\Exceptions\ApiException;
use GlobalPayments\Api\PaymentMethods\CreditCardData;
use GlobalPayments\Api\ServiceConfigs\Gateways\GpApiConfig;
use GlobalPayments\Api\ServicesContainer;
use GlobalPayments\Api\Entities\Enums\Environment;
use GlobalPayments\Api\Entities\Enums\Channel;
use MarketplaceFee\SellerManager;
use MarketplaceFee\SplitCalculator;
use MarketplaceFee\Utils;

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

// DEBUG: Log credential verification (REMOVE IN PRODUCTION)
error_log('=== GP API Configuration Debug ===');
error_log('APP_ID loaded: ' . (isset($_ENV['GP_APP_ID']) ? 'YES' : 'NO'));
error_log('APP_ID value: ' . ($_ENV['GP_APP_ID'] ?? 'NOT_SET'));
error_log('APP_KEY loaded: ' . (isset($_ENV['GP_APP_KEY']) ? 'YES' : 'NO'));
error_log('APP_KEY length: ' . (isset($_ENV['GP_APP_KEY']) ? strlen($_ENV['GP_APP_KEY']) : 0));
error_log('Environment: ' . ($_ENV['GP_API_ENVIRONMENT'] ?? 'NOT_SET'));
error_log('==================================');

// Set response content type to JSON
header('Content-Type: application/json');

try {
    // Validate required fields
    if (!isset($_POST['payment_token'], $_POST['billing_zip'], $_POST['amount'], $_POST['seller_id'])) {
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

    // Create billing address for AVS verification
    $address = new Address();
    $address->postalCode = Utils::sanitizePostalCode($_POST['billing_zip']);

    // Calculate fee split
    $platformFeeRate = floatval($_POST['platform_fee_rate'] ?? 10.0);
    $calculator = new SplitCalculator($platformFeeRate);
    $splitDetails = $calculator->calculateSplit($amount);

    // Add seller information to split details
    $splitDetails['sellerId'] = $sellerId;
    $splitDetails['sellerName'] = $seller['name'];

    // Initialize payment data using tokenized card from frontend SDK
    $card = new CreditCardData();
    $card->token = $_POST['payment_token'];

    // Process the payment transaction with specified amount
    $response = $card->charge($amount)
        ->withAllowDuplicates(true)
        ->withCurrency('USD')
        ->withAddress($address)
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
    exit;
} catch (ApiException $e) {
    // Log detailed error information
    error_log('=== Payment Processing Error ===');
    error_log('Error Message: ' . $e->getMessage());
    error_log('Error Code: ' . $e->getCode());
    error_log('Request Data: ' . json_encode([
        'amount' => $_POST['amount'] ?? 'missing',
        'seller_id' => $_POST['seller_id'] ?? 'missing',
        'token_present' => isset($_POST['payment_token']),
        'token_prefix' => isset($_POST['payment_token']) ? substr($_POST['payment_token'], 0, 10) : 'N/A',
    ]));
    error_log('Stack Trace: ' . $e->getTraceAsString());
    error_log('===============================');

    // Handle payment processing errors
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Payment processing failed',
        'error' => [
            'code' => 'API_ERROR',
            'details' => $e->getMessage(),
            'timestamp' => date('Y-m-d H:i:s')
        ]
    ]);
    exit;
} catch (\Exception $e) {
    // Log general error information
    error_log('=== General Server Error ===');
    error_log('Error Type: ' . get_class($e));
    error_log('Error Message: ' . $e->getMessage());
    error_log('Error Code: ' . $e->getCode());
    error_log('File: ' . $e->getFile() . ':' . $e->getLine());
    error_log('Stack Trace: ' . $e->getTraceAsString());
    error_log('==========================');

    // Handle general errors
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Internal server error',
        'error' => [
            'code' => 'SERVER_ERROR',
            'details' => $e->getMessage(),
            'timestamp' => date('Y-m-d H:i:s')
        ]
    ]);
    exit;
}
