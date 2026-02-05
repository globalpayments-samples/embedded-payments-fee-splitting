using GlobalPayments.Api;
using GlobalPayments.Api.Entities;
using GlobalPayments.Api.PaymentMethods;
using GlobalPayments.Api.Services;
using dotenv.net;
using EmbeddedPaymentsFee.Services;
using EmbeddedPaymentsFee.Models;

namespace CardPaymentSample;

/// <summary>
/// Card Payment Processing Application
/// 
/// This application demonstrates card payment processing using the Global Payments SDK.
/// It provides endpoints for configuration and payment processing, handling tokenized
/// card data to ensure secure payment processing.
/// </summary>
public class Program
{
    public static void Main(string[] args)
    {
        // Load environment variables from .env file
        DotEnv.Load();

        var builder = WebApplication.CreateBuilder(args);
        
        var app = builder.Build();

        // Configure the SDK on startup
        ConfigureGlobalPaymentsSDK();

        // Configure API endpoints BEFORE static files
        ConfigureEndpoints(app);

        // Configure static file serving - MUST come after API endpoints
        app.UseDefaultFiles();
        app.UseStaticFiles();

        var port = System.Environment.GetEnvironmentVariable("PORT") ?? "8000";
        app.Urls.Add($"http://0.0.0.0:{port}");

        app.Run();
    }

    /// <summary>
    /// Gets an environment variable and strips inline comments
    /// </summary>
    /// <param name="key">The environment variable key</param>
    /// <returns>The environment variable value with comments removed</returns>
    private static string GetEnvVar(string key)
    {
        var value = System.Environment.GetEnvironmentVariable(key) ?? string.Empty;
        var commentIndex = value.IndexOf('#');
        if (commentIndex >= 0)
        {
            value = value[..commentIndex];
        }
        return value.Trim();
    }


    /// <summary>
    /// Configures the Global Payments SDK with necessary credentials and settings.
    /// This must be called before processing any payments.
    /// </summary>
    private static void ConfigureGlobalPaymentsSDK()
    {
        var config = new GpApiConfig
        {
            AppId = GetEnvVar("GP_APP_ID"),
            AppKey = GetEnvVar("GP_APP_KEY"),
            Environment = "PRODUCTION".Equals(GetEnvVar("GP_API_ENVIRONMENT"))
                ? GlobalPayments.Api.Entities.Environment.PRODUCTION
                : GlobalPayments.Api.Entities.Environment.TEST,
            Channel = GlobalPayments.Api.Entities.Channel.CardNotPresent,
            Country = "US"
        };
        ServicesContainer.ConfigureService(config);
    }

    /// <summary>
    /// Configures the application's HTTP endpoints for payment processing.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigureEndpoints(WebApplication app)
    {
        ConfigureEmbeddedPaymentsEndpoint(app);
    }

    /// <summary>
    /// Sanitizes postal code input by removing invalid characters.
    /// </summary>
    /// <param name="postalCode">The postal code to sanitize. Can be null.</param>
    /// <returns>
    /// A sanitized postal code containing only alphanumeric characters and hyphens,
    /// limited to 10 characters. Returns empty string if input is null or empty.
    /// </returns>
    private static string SanitizePostalCode(string postalCode)
    {
        if (string.IsNullOrEmpty(postalCode)) return string.Empty;
        
        // Remove any characters that aren't alphanumeric or hyphen
        var sanitized = new string(postalCode.Where(c => char.IsLetterOrDigit(c) || c == '-').ToArray());
        
        // Limit length to 10 characters
        return sanitized.Length > 10 ? sanitized[..10] : sanitized;
    }

    /// <summary>
    /// Configures the embedded payments processing endpoint with automatic fee splitting.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigureEmbeddedPaymentsEndpoint(WebApplication app)
    {
        app.MapPost("/process-embedded-payments-payment", async (HttpContext context) =>
        {
            // Parse form data from the request
            var form = await context.Request.ReadFormAsync();
            var cardName = form["card_name"].ToString();
            var cardNumber = form["card_number"].ToString();
            var cardExpiry = form["card_expiry"].ToString();
            var cardCvv = form["card_cvv"].ToString();
            var billingZip = form["billing_zip"].ToString();
            var amountStr = form["amount"].ToString();
            var sellerId = form["seller_id"].ToString();
            var platformFeeRateStr = form["platform_fee_rate"].ToString();

            // Validate required fields
            if (string.IsNullOrEmpty(cardName) ||
                string.IsNullOrEmpty(cardNumber) ||
                string.IsNullOrEmpty(cardExpiry) ||
                string.IsNullOrEmpty(cardCvv) ||
                string.IsNullOrEmpty(billingZip) ||
                string.IsNullOrEmpty(amountStr) ||
                string.IsNullOrEmpty(sellerId))
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Missing required fields"
                });
            }

            // Validate and parse amount
            if (!decimal.TryParse(amountStr, out var amount) || amount < 0.50m)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Amount must be at least $0.50"
                });
            }

            // Validate seller
            if (!SellerManager.IsValidSeller(sellerId))
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Invalid seller selected"
                });
            }

            var seller = SellerManager.GetSellerById(sellerId);

            // Parse platform fee rate (default to 10.0 if not provided)
            if (!double.TryParse(platformFeeRateStr, out var platformFeeRate))
            {
                platformFeeRate = 10.0;
            }

            // Calculate fee split
            var calculator = new SplitCalculator(platformFeeRate);
            var splitDetails = calculator.CalculateSplit((double)amount);
            splitDetails.SellerId = sellerId;
            splitDetails.SellerName = seller.Name;

            // Parse expiry date (MM/YY format)
            var expiryParts = cardExpiry.Split('/');
            if (expiryParts.Length != 2)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Invalid expiry date format. Use MM/YY"
                });
            }

            var expiryMonth = int.Parse(expiryParts[0].PadLeft(2, '0'));
            var expiryYear = int.Parse("20" + expiryParts[1]);

            // Initialize payment data with card details
            var card = new CreditCardData
            {
                CardHolderName = cardName,
                Number = cardNumber.Replace(" ", ""),
                ExpMonth = expiryMonth,
                ExpYear = expiryYear,
                Cvn = cardCvv
            };

            // Create billing address for AVS verification
            var address = new Address
            {
                PostalCode = SanitizePostalCode(billingZip)
            };

            try
            {
                // Process the payment transaction
                var response = card.Charge(amount)
                    .WithAllowDuplicates(true)
                    .WithCurrency("USD")
                    .WithAddress(address)
                    .Execute();

                // Check for null response
                if (response == null)
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Payment processing failed",
                        error = new {
                            code = "NULL_RESPONSE",
                            details = "Payment gateway returned null response"
                        }
                    });
                }

                // Verify transaction was successful (GP API returns 'SUCCESS' or '00')
                if (response.ResponseCode != "00" && response.ResponseCode != "SUCCESS")
                {
                    return Results.BadRequest(new {
                        success = false,
                        message = "Payment processing failed",
                        error = new {
                            code = "PAYMENT_DECLINED",
                            details = response.ResponseMessage
                        }
                    });
                }

                // Return success response with transaction ID and split details
                return Results.Ok(new
                {
                    success = true,
                    message = $"Payment successful! Transaction ID: {response.TransactionId}",
                    data = new {
                        transactionId = response.TransactionId,
                        amount = amount,
                        currency = "USD",
                        splitDetails = splitDetails
                    }
                });
            }
            catch (ApiException ex)
            {
                // Handle payment processing errors
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "API_ERROR",
                        details = ex.Message
                    }
                });
            }
        });
    }
}
