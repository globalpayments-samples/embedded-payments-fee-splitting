<?php

declare(strict_types=1);

namespace EmbeddedPayments;

/**
 * Fee Split Calculator
 *
 * Calculates embedded payment fee splits for transactions
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   EmbeddedPayments
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */
class SplitCalculator
{
    private float $platformFeeRate;

    /**
     * Constructor
     *
     * @param float $platformFeeRate Platform fee percentage (3-5)
     *
     * @throws \InvalidArgumentException If platform fee rate is out of range
     */
    public function __construct(float $platformFeeRate = 3.0)
    {
        if ($platformFeeRate < 3 || $platformFeeRate > 5) {
            throw new \InvalidArgumentException('Platform fee must be between 3% and 5%');
        }
        $this->platformFeeRate = $platformFeeRate;
    }

    /**
     * Calculate fee split for transaction amount
     *
     * @param float $amount Transaction amount
     *
     * @return array Split details with platform fee and payout
     * @throws \InvalidArgumentException If amount is less than minimum
     */
    public function calculateSplit(float $amount): array
    {
        if ($amount < 0.50) {
            throw new \InvalidArgumentException('Amount must be at least $0.50');
        }

        // Calculate platform fee
        $platformFee = $amount * ($this->platformFeeRate / 100);
        $platformFee = round($platformFee, 2);

        // Calculate seller payout
        $sellerPayout = $amount - $platformFee;
        $sellerPayout = round($sellerPayout, 2);

        return [
            'amount' => $amount,
            'platformFee' => $platformFee,
            'platformFeeRate' => $this->platformFeeRate,
            'sellerPayout' => $sellerPayout
        ];
    }
}
