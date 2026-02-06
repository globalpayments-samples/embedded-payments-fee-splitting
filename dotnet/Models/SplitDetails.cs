namespace EmbeddedPaymentsFee.Models;

public class SplitDetails
{
    public double Amount { get; set; }
    public double ProcessingFee { get; set; }
    public double ProcessingFeeRate { get; set; }
    public double ProcessingFeeFixed { get; set; }
    public double PlatformFee { get; set; }
    public double PlatformFeeRate { get; set; }
    public double SellerPayout { get; set; }
    public string SellerId { get; set; } = string.Empty;
    public string SellerName { get; set; } = string.Empty;
}
