<?php

declare(strict_types=1);

/**
 * Configuration Endpoint
 *
 * This script provides configuration information for the client-side SDK,
 * including GP API environment settings for Drop-In UI integration.
 *
 * PHP version 7.4 or higher
 *
 * @category  Configuration
 * @package   GlobalPayments_Sample
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */

require_once 'vendor/autoload.php';

use Dotenv\Dotenv;

try {
    // Load environment variables from .env file
    $dotenv = Dotenv::createImmutable(__DIR__);
    $dotenv->load();

    // Set response content type to JSON
    header('Content-Type: application/json');

    // Return GP API configuration for frontend
    echo json_encode([
        'success' => true,
        'data' => [
            'environment' => $_ENV['GP_API_ENVIRONMENT'] ?? 'TEST',
            'apiVersion' => '2021-03-22',
            'channel' => $_ENV['CHANNEL'] ?? 'CardNotPresent',
            'country' => $_ENV['COUNTRY'] ?? 'US',
            'platformFeeRate' => floatval($_ENV['PLATFORM_FEE_RATE'] ?? 10.0)
        ],
    ]);
} catch (Exception $e) {
    // Handle configuration errors
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error loading configuration: ' . $e->getMessage()
    ]);
}
