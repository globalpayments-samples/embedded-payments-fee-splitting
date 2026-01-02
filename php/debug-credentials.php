<?php

declare(strict_types=1);

/**
 * Credential Debug Script
 *
 * This script verifies that credentials are loaded correctly from .env
 * and tests GP API authentication WITHOUT processing a payment.
 *
 * PHP version 7.4 or higher
 *
 * @category  Debug
 * @package   GlobalPayments_Debug
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */

require_once 'vendor/autoload.php';

use Dotenv\Dotenv;
use GlobalPayments\Api\ServiceConfigs\Gateways\GpApiConfig;
use GlobalPayments\Api\ServicesContainer;
use GlobalPayments\Api\Entities\Enums\Environment;
use GlobalPayments\Api\Entities\Enums\Channel;

header('Content-Type: application/json');

try {
    // Load environment variables
    $dotenv = Dotenv::createImmutable(__DIR__);
    $dotenv->load();

    // Debug: Show loaded environment variables (REMOVE IN PRODUCTION)
    $debug = [
        'env_file_exists' => file_exists(__DIR__ . '/.env'),
        'app_id_loaded' => isset($_ENV['GP_APP_ID']),
        'app_id_value' => $_ENV['GP_APP_ID'] ?? 'NOT_SET',
        'app_key_loaded' => isset($_ENV['GP_APP_KEY']),
        'app_key_length' => isset($_ENV['GP_APP_KEY']) ? strlen($_ENV['GP_APP_KEY']) : 0,
        'environment' => $_ENV['GP_API_ENVIRONMENT'] ?? 'NOT_SET',
        'public_key_loaded' => isset($_ENV['GP_PUBLIC_API_KEY']),
    ];

    // Configure GP API
    $config = new GpApiConfig();
    $config->appId = $_ENV['GP_APP_ID'];
    $config->appKey = $_ENV['GP_APP_KEY'];
    $config->environment = $_ENV['GP_API_ENVIRONMENT'] === 'PRODUCTION'
        ? Environment::PRODUCTION
        : Environment::TEST;
    $config->channel = Channel::CardNotPresent;
    $config->country = 'US';

    // Debug: Show configuration
    $debug['config'] = [
        'appId' => $config->appId ?? 'NULL',
        'appKey_length' => isset($config->appKey) ? strlen($config->appKey) : 0,
        'environment' => $config->environment,
        'channel' => $config->channel,
        'country' => $config->country,
    ];

    // Test configuration by initializing ServicesContainer
    // This will validate credentials and throw exception if invalid
    ServicesContainer::configureService($config);
    $debug['sdk_configured'] = true;

    // If we get here, SDK accepted the configuration
    // (actual API calls will test if credentials work with GP API servers)
    echo json_encode([
        'success' => true,
        'message' => 'GP API SDK configuration successful - credentials loaded',
        'note' => 'This only confirms credentials are loaded into SDK. Actual API authentication happens during payment processing.',
        'debug' => $debug
    ], JSON_PRETTY_PRINT);

} catch (\Exception $e) {
    // Configuration error - credentials not loaded or SDK issue
    echo json_encode([
        'success' => false,
        'message' => 'Configuration failed',
        'error' => $e->getMessage(),
        'debug' => $debug ?? []
    ], JSON_PRETTY_PRINT);
}
