<?php

declare(strict_types=1);

/**
 * Router for PHP built-in server
 *
 * Maps clean URLs to PHP endpoint files:
 * - /config → config.php
 * - /process-marketplace-payment → process-marketplace-payment.php
 * - /process-payment → process-payment.php
 */

$requestUri = $_SERVER['REQUEST_URI'];
$requestMethod = $_SERVER['REQUEST_METHOD'];
$path = parse_url($requestUri, PHP_URL_PATH);

// Remove leading/trailing slashes and normalize
$path = trim($path, '/');

// Serve index.html for root requests
if (empty($path)) {
    if (file_exists('index.html')) {
        header('Content-Type: text/html; charset=UTF-8');
        readfile('index.html');
        return true;
    }
    http_response_code(404);
    return false;
}

// Serve static files directly (CSS, JS, images, fonts)
$staticExtensions = ['css', 'js', 'png', 'jpg', 'jpeg', 'gif', 'ico', 'svg', 'woff', 'woff2', 'ttf', 'eot', 'otf'];
$pathInfo = pathinfo($path);
if (isset($pathInfo['extension']) && in_array(strtolower($pathInfo['extension']), $staticExtensions)) {
    if (file_exists($path)) {
        return false; // Let PHP serve the file with appropriate MIME type
    }
    http_response_code(404);
    return true;
}

// API endpoint routing
$endpointMap = [
    'config' => [
        'file' => 'config.php',
        'methods' => ['GET', 'POST']
    ],
    'get-access-token' => [
        'file' => 'get-access-token.php',
        'methods' => ['POST']
    ],
    'process-marketplace-payment' => [
        'file' => 'process-marketplace-payment.php',
        'methods' => ['POST']
    ],
    'process-payment' => [
        'file' => 'process-payment.php',
        'methods' => ['POST']
    ]
];

// Check if path matches an endpoint
if (isset($endpointMap[$path])) {
    $endpoint = $endpointMap[$path];

    // Validate HTTP method
    if (!in_array($requestMethod, $endpoint['methods'])) {
        http_response_code(405);
        header('Content-Type: application/json');
        echo json_encode([
            'success' => false,
            'message' => 'Method not allowed',
            'allowedMethods' => $endpoint['methods']
        ]);
        return true;
    }

    // Include the endpoint file
    if (file_exists($endpoint['file'])) {
        require_once $endpoint['file'];
        return true;
    }

    // File not found
    http_response_code(500);
    header('Content-Type: application/json');
    echo json_encode([
        'success' => false,
        'message' => 'Endpoint file not found',
        'file' => $endpoint['file']
    ]);
    return true;
}

// Handle 404 for unknown paths
http_response_code(404);
header('Content-Type: application/json');
echo json_encode([
    'success' => false,
    'message' => 'Endpoint not found',
    'path' => '/' . $path,
    'availableEndpoints' => array_keys($endpointMap)
]);
return true;
