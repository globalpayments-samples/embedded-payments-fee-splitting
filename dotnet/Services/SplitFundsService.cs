namespace EmbeddedPayments.Services;

using GlobalPayments.Api.Services;

public class SplitFundsService
{
    private readonly string _platformAccountNumber;

    public SplitFundsService()
    {
        _platformAccountNumber = Environment.GetEnvironmentVariable("PLATFORM_PROPAY_ACCOUNT") ?? "718580391";
    }

    public (bool Success, string? TransNum, string? ErrorMessage) ExecuteSplit(
        string sellerProPayAccount,
        decimal amount,
        string transactionId)
    {
        try
        {
            var response = PayFacService.SplitFunds()
                .WithAccountNumber(_platformAccountNumber)
                .WithReceivingAccountNumber(sellerProPayAccount)
                .WithAmount(amount.ToString("F2"))
                .WithGlobaltransId(transactionId)
                .WithGlobalTransSource("portico")
                .Execute();

            return (response.ResponseCode == "00", response.PayFacData?.TransNum, null);
        }
        catch (Exception ex)
        {
            return (false, null, ex.Message);
        }
    }
}
