<?php

declare(strict_types=1);

/**
 * Access Token Generation Endpoint
 *
 * This script generates restricted access tokens for client-side card tokenization.
 * The token is used by the frontend Drop-In UI to securely tokenize card data
 * without exposing server credentials.
 *
 * PHP version 7.4 or higher
 *
 * @category  Authentication
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

    // Generate nonce for security
    $nonce = bin2hex(random_bytes(16));

    // Prepare request data for access token
    // For Drop-In UI, we need PMT_POST_Create_Single permission for card tokenization
    $requestData = [
        'app_id' => $_ENV['GP_APP_ID'],
        'nonce' => $nonce,
        'secret' => hash('sha512', $nonce . $_ENV['GP_APP_KEY']),
        'grant_type' => 'client_credentials',
        'seconds_to_expire' => 600,
        'permissions' => ['PMT_POST_Create_Single']
    ];

    // Determine API endpoint based on environment
    $apiEndpoint = ($_ENV['GP_ENVIRONMENT'] ?? 'sandbox') === 'production'
        ? 'https://apis.globalpay.com/ucp/accesstoken'
        : 'https://apis.sandbox.globalpay.com/ucp/accesstoken';

    // Make API request using cURL
    $ch = curl_init($apiEndpoint);
    curl_setopt_array($ch, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => json_encode($requestData),
        CURLOPT_HTTPHEADER => [
            'Content-Type: application/json',
            'X-GP-Version: 2021-03-22'
        ]
    ]);

    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

    if (curl_errno($ch)) {
        throw new Exception('cURL error: ' . curl_error($ch));
    }

    curl_close($ch);

    if ($httpCode !== 200) {
        $errorData = json_decode($response, true);
        throw new Exception('Failed to generate access token: ' . ($errorData['error'] ?? 'Unknown error'));
    }

    $data = json_decode($response, true);

    if (!isset($data['token'])) {
        throw new Exception('Token not found in API response');
    }

    echo json_encode([
        'success' => true,
        'token' => $data['token']
    ]);

} catch (Exception $e) {
    // Handle token generation errors
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => $e->getMessage()
    ]);
}
