namespace EmbeddedPayments.Services;

using EmbeddedPayments.Models;

public class SplitCalculator
{
    private const double PROCESSING_FEE_RATE = 0.029;
    private const double PROCESSING_FEE_FIXED = 0.30;

    private readonly double _platformFeeRate;

    public SplitCalculator(double platformFeeRate = 10.0)
    {
        if (platformFeeRate < 5 || platformFeeRate > 25)
            throw new ArgumentException("Platform fee must be between 5% and 25%");

        _platformFeeRate = platformFeeRate;
    }

    public SplitDetails CalculateSplit(double amount)
    {
        if (amount < 0.50)
            throw new ArgumentException("Amount must be at least $0.50");

        var processingFee = Math.Round((amount * PROCESSING_FEE_RATE) + PROCESSING_FEE_FIXED, 2);
        var platformFee = Math.Round(amount * (_platformFeeRate / 100), 2);
        var sellerPayout = Math.Round(amount - processingFee - platformFee, 2);

        var total = processingFee + platformFee + sellerPayout;
        if (Math.Abs(total - amount) > 0.01)
        {
            sellerPayout = Math.Round(amount - processingFee - platformFee, 2);
        }

        return new SplitDetails
        {
            Amount = amount,
            ProcessingFee = processingFee,
            ProcessingFeeRate = PROCESSING_FEE_RATE * 100,
            ProcessingFeeFixed = PROCESSING_FEE_FIXED,
            PlatformFee = platformFee,
            PlatformFeeRate = _platformFeeRate,
            SellerPayout = sellerPayout
        };
    }
}
