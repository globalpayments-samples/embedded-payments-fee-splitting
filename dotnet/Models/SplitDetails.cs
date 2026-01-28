namespace EmbeddedPayments.Models;

public class SplitDetails
{
    public double Amount { get; set; }
    public double PlatformFee { get; set; }
    public double PlatformFeeRate { get; set; }
    public double SellerPayout { get; set; }
    public string SellerId { get; set; } = string.Empty;
    public string SellerName { get; set; } = string.Empty;
    public string? TransactionId { get; set; }
    public string? SplitTransactionId { get; set; }
}
