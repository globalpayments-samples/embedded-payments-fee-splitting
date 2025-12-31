<?php

declare(strict_types=1);

/**
 * Configuration Endpoint
 *
 * This script provides the public API key for client-side tokenization.
 * The public key is safe to expose to the browser as it has restricted
 * permissions and can only create single-use payment tokens.
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

    // Return public API key for client-side tokenization
    // This key is safe to expose to the browser (restricted permissions)
    echo json_encode([
        'success' => true,
        'data' => [
            'publicApiKey' => $_ENV['GP_PUBLIC_API_KEY'],
        ],
    ]);
    exit;
} catch (Exception $e) {
    // Handle configuration errors
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error loading configuration: ' . $e->getMessage()
    ]);
    exit;
}
