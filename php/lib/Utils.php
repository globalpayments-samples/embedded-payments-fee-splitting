<?php

declare(strict_types=1);

namespace MarketplaceFee;

/**
 * Utility Functions for Marketplace Operations
 *
 * Provides shared utility methods used across payment processing scripts.
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   GlobalPayments_Sample
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */
class Utils
{
    /**
     * Sanitizes postal code for payment processing
     *
     * Removes all characters except alphanumeric and hyphens, then limits
     * to 10 characters as required by payment processors. This ensures
     * postal codes are in a format acceptable for AVS verification.
     *
     * @param string|null $postalCode Postal code to sanitize
     *
     * @return string Sanitized postal code (alphanumeric and hyphens only, max 10 chars)
     */
    public static function sanitizePostalCode(?string $postalCode): string
    {
        if ($postalCode === null) {
            return '';
        }

        $sanitized = preg_replace('/[^a-zA-Z0-9-]/', '', $postalCode);
        return substr($sanitized, 0, 10);
    }
}
