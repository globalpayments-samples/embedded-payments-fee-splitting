namespace EmbeddedPayments.Models;

public class Seller
{
    public string Id { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public double PlatformFeeRate { get; set; }
    public string Description { get; set; } = string.Empty;
    public string ProPayAccountNumber { get; set; } = string.Empty;
}

public class SellersData
{
    public List<Seller> Sellers { get; set; } = new();
}
