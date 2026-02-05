<?php

declare(strict_types=1);

namespace EmbeddedPaymentsFee;

/**
 * Seller Manager
 *
 * Manages mock seller data for embedded payments demo
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   EmbeddedPaymentsFee
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */
class SellerManager
{
    private static ?array $sellers = null;

    /**
     * Load sellers from JSON file
     *
     * @return void
     * @throws \RuntimeException If seller data file not found or invalid JSON
     */
    private static function loadSellers(): void
    {
        if (self::$sellers !== null) {
            return;
        }

        $jsonPath = __DIR__ . '/../data/mock-sellers.json';
        if (!file_exists($jsonPath)) {
            throw new \RuntimeException('Seller data file not found');
        }

        $json = file_get_contents($jsonPath);
        $data = json_decode($json, true);

        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new \RuntimeException('Invalid seller data JSON');
        }

        self::$sellers = $data['sellers'] ?? [];
    }

    /**
     * Get all sellers
     *
     * @return array List of all sellers
     */
    public static function getAllSellers(): array
    {
        self::loadSellers();
        return self::$sellers;
    }

    /**
     * Get seller by ID
     *
     * @param string $id Seller ID
     *
     * @return array|null Seller data or null if not found
     */
    public static function getSellerById(string $id): ?array
    {
        self::loadSellers();
        foreach (self::$sellers as $seller) {
            if ($seller['id'] === $id) {
                return $seller;
            }
        }
        return null;
    }

    /**
     * Validate seller exists
     *
     * @param string $id Seller ID to validate
     *
     * @return bool True if seller exists, false otherwise
     */
    public static function isValidSeller(string $id): bool
    {
        return self::getSellerById($id) !== null;
    }
}
