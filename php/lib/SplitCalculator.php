<?php

declare(strict_types=1);

namespace MarketplaceFee;

/**
 * Fee Split Calculator
 *
 * Calculates marketplace fee splits for transactions
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   MarketplaceFee
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */
class SplitCalculator
{
    private const PROCESSING_FEE_RATE = 0.029;  // 2.9%
    private const PROCESSING_FEE_FIXED = 0.30;   // $0.30

    private float $platformFeeRate;

    /**
     * Constructor
     *
     * @param float $platformFeeRate Platform fee percentage (5-25)
     *
     * @throws \InvalidArgumentException If platform fee rate is out of range
     */
    public function __construct(float $platformFeeRate = 10.0)
    {
        if ($platformFeeRate < 5 || $platformFeeRate > 25) {
            throw new \InvalidArgumentException('Platform fee must be between 5% and 25%');
        }
        $this->platformFeeRate = $platformFeeRate;
    }

    /**
     * Calculate fee split for transaction amount
     *
     * @param float $amount Transaction amount
     *
     * @return array Split details with all fees and payout
     * @throws \InvalidArgumentException If amount is less than minimum
     */
    public function calculateSplit(float $amount): array
    {
        if ($amount < 0.50) {
            throw new \InvalidArgumentException('Amount must be at least $0.50');
        }

        // Calculate processing fee
        $processingFee = ($amount * self::PROCESSING_FEE_RATE) + self::PROCESSING_FEE_FIXED;
        $processingFee = round($processingFee, 2);

        // Calculate platform fee
        $platformFee = $amount * ($this->platformFeeRate / 100);
        $platformFee = round($platformFee, 2);

        // Calculate seller payout
        $sellerPayout = $amount - $processingFee - $platformFee;
        $sellerPayout = round($sellerPayout, 2);

        // Verify math (handle rounding discrepancies)
        $total = $processingFee + $platformFee + $sellerPayout;
        if (abs($total - $amount) > 0.01) {
            $sellerPayout = round($amount - $processingFee - $platformFee, 2);
        }

        return [
            'amount' => $amount,
            'processingFee' => $processingFee,
            'processingFeeRate' => self::PROCESSING_FEE_RATE * 100,
            'processingFeeFixed' => self::PROCESSING_FEE_FIXED,
            'platformFee' => $platformFee,
            'platformFeeRate' => $this->platformFeeRate,
            'sellerPayout' => $sellerPayout
        ];
    }
}
