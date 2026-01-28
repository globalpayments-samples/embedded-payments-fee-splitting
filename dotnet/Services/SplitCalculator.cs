namespace EmbeddedPayments.Services;

using EmbeddedPayments.Models;

public class SplitCalculator
{
    private readonly double _platformFeeRate;

    public SplitCalculator(double platformFeeRate = 3.0)
    {
        if (platformFeeRate < 3 || platformFeeRate > 5)
            throw new ArgumentException("Platform fee must be between 3% and 5%");

        _platformFeeRate = platformFeeRate;
    }

    public SplitDetails CalculateSplit(double amount)
    {
        if (amount < 0.50)
            throw new ArgumentException("Amount must be at least $0.50");

        var platformFee = Math.Round(amount * (_platformFeeRate / 100), 2);
        var sellerPayout = Math.Round(amount - platformFee, 2);

        return new SplitDetails
        {
            Amount = amount,
            PlatformFee = platformFee,
            PlatformFeeRate = _platformFeeRate,
            SellerPayout = sellerPayout
        };
    }
}
