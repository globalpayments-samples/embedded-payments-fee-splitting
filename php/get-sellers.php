<?php

declare(strict_types=1);

/**
 * Sellers List Endpoint
 *
 * This script returns the list of available sellers for the marketplace.
 * Used by the frontend to populate the seller selection dropdown.
 *
 * PHP version 7.4 or higher
 *
 * @category  Data_Management
 * @package   GlobalPayments_Marketplace
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */

require_once 'vendor/autoload.php';

use MarketplaceFee\SellerManager;

// Set response content type to JSON
header('Content-Type: application/json');

try {
    // Retrieve all sellers from the seller manager
    $sellers = SellerManager::getAllSellers();

    // Return success response with seller list
    echo json_encode([
        'success' => true,
        'sellers' => $sellers
    ]);
} catch (\Exception $e) {
    // Handle errors
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Error retrieving sellers',
        'error' => $e->getMessage()
    ]);
}
