<?php

declare(strict_types=1);

namespace EmbeddedPayments;

use GlobalPayments\Api\Services\PayFacService;

/**
 * SplitFunds Service
 *
 * Handles ProPay SplitFunds transactions to transfer funds from
 * the platform's ProPay account to the seller's ProPay account
 * after a successful payment.
 *
 * PHP version 7.4 or higher
 *
 * @category  Payment_Processing
 * @package   EmbeddedPayments
 * @author    Global Payments
 * @license   MIT License
 * @link      https://github.com/globalpayments
 */
class SplitFundsService
{
    private string $platformAccountNumber;

    /**
     * Creates a new SplitFundsService instance
     */
    public function __construct()
    {
        $this->platformAccountNumber = $_ENV['PLATFORM_PROPAY_ACCOUNT'] ?? '718580391';
    }

    /**
     * Executes a SplitFunds transaction to transfer funds to the seller
     *
     * @param string $sellerProPayAccount The seller's ProPay account number
     * @param float  $amount              The amount to transfer
     * @param string $transactionId       The original payment transaction ID
     *
     * @return array Result with success, transNum, and errorMessage keys
     */
    public function executeSplit(
        string $sellerProPayAccount,
        float $amount,
        string $transactionId
    ): array {
        try {
            $response = PayFacService::splitFunds()
                ->withAccountNumber($this->platformAccountNumber)
                ->withReceivingAccountNumber($sellerProPayAccount)
                ->withAmount(number_format($amount, 2, '.', ''))
                ->withGlobaltransId($transactionId)
                ->withGlobalTransSource('portico')
                ->execute();

            return [
                'success' => $response->responseCode === '00',
                'transNum' => $response->payFacData->transNum ?? null,
                'errorMessage' => null
            ];
        } catch (\Exception $e) {
            return [
                'success' => false,
                'transNum' => null,
                'errorMessage' => $e->getMessage()
            ];
        }
    }
}
