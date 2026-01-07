using GlobalPayments.Api;
using GlobalPayments.Api.Entities;
using GlobalPayments.Api.PaymentMethods;
using GlobalPayments.Api.Services;
using dotenv.net;
using MarketplaceFee.Services;
using MarketplaceFee.Models;

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
    /// Generates a random nonce for access token requests
    /// </summary>
    /// <returns>A hexadecimal string representing the nonce</returns>
    private static string GenerateNonce()
    {
        using var rng = System.Security.Cryptography.RandomNumberGenerator.Create();
        var bytes = new byte[16];
        rng.GetBytes(bytes);
        return BitConverter.ToString(bytes).Replace("-", "").ToLower();
    }

    /// <summary>
    /// Hashes the nonce and app key using SHA-512
    /// </summary>
    /// <param name="nonce">The nonce to hash</param>
    /// <param name="appKey">The app key to include in the hash</param>
    /// <returns>A hexadecimal string representing the SHA-512 hash</returns>
    private static string HashSecret(string nonce, string appKey)
    {
        using var sha512 = System.Security.Cryptography.SHA512.Create();
        var bytes = System.Text.Encoding.UTF8.GetBytes(nonce + appKey);
        var hash = sha512.ComputeHash(bytes);
        return BitConverter.ToString(hash).Replace("-", "").ToLower();
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
            Environment = "production".Equals(GetEnvVar("GP_ENVIRONMENT"))
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
        // Configure HTTP endpoints
        app.MapGet("/config", () => Results.Ok(new
        {
            success = true,
            data = new {
                publicApiKey = GetEnvVar("PUBLIC_API_KEY")
            }
        }));

        app.MapPost("/get-access-token", async () =>
        {
            try
            {
                var nonce = GenerateNonce();
                var secret = HashSecret(nonce, GetEnvVar("GP_APP_KEY"));

                var tokenRequest = new
                {
                    app_id = GetEnvVar("GP_APP_ID"),
                    nonce = nonce,
                    secret = secret,
                    grant_type = "client_credentials",
                    seconds_to_expire = 600,
                    permissions = new[] { "PMT_POST_Create_Single" }
                };

                var apiEndpoint = "production".Equals(GetEnvVar("GP_ENVIRONMENT"))
                    ? "https://apis.globalpay.com/ucp/accesstoken"
                    : "https://apis.sandbox.globalpay.com/ucp/accesstoken";

                using var httpClient = new HttpClient();
                httpClient.DefaultRequestHeaders.Add("X-GP-Version", "2021-03-22");

                var content = new StringContent(
                    System.Text.Json.JsonSerializer.Serialize(tokenRequest),
                    System.Text.Encoding.UTF8,
                    "application/json"
                );

                var response = await httpClient.PostAsync(apiEndpoint, content);
                var responseBody = await response.Content.ReadAsStringAsync();
                var data = System.Text.Json.JsonSerializer.Deserialize<Dictionary<string, object>>(responseBody);

                var environment = "production".Equals(GetEnvVar("GP_ENVIRONMENT")) ? "production" : "sandbox";

                return Results.Ok(new
                {
                    success = true,
                    token = data["token"].ToString(),
                    environment = environment
                });
            }
            catch (Exception ex)
            {
                return Results.StatusCode(500);
            }
        });

        ConfigurePaymentEndpoint(app);
        ConfigureMarketplaceEndpoint(app);
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
    /// Configures the payment processing endpoint that handles card transactions.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigurePaymentEndpoint(WebApplication app)
    {
        app.MapPost("/process-payment", async (HttpContext context) =>
        {
            // Parse form data from the request
            var form = await context.Request.ReadFormAsync();
            var billingZip = form["billing_zip"].ToString();
            var token = form["payment_token"].ToString();
            var amountStr = form["amount"].ToString();

            // Validate required fields are present
            if (string.IsNullOrEmpty(token) || string.IsNullOrEmpty(billingZip) || string.IsNullOrEmpty(amountStr))
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Missing required fields"
                    }
                });
            }

            // Validate and parse amount
            if (!decimal.TryParse(amountStr, out var amount) || amount <= 0)
            {
                return Results.BadRequest(new {
                    success = false,
                    message = "Payment processing failed",
                    error = new {
                        code = "VALIDATION_ERROR",
                        details = "Amount must be a positive number"
                    }
                });
            }

            // Initialize payment data using tokenized card information
            var card = new CreditCardData
            {
                Token = token
            };

            // Create billing address for AVS verification
            var address = new Address
            {
                PostalCode = SanitizePostalCode(billingZip)
            };

            try
            {
                // Process the payment transaction using the provided amount
                var response = card.Charge(amount)
                    .WithAllowDuplicates(true)
                    .WithCurrency("USD")
                    .WithAddress(address)
                    .Execute();

                // Verify transaction was successful
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

                // Return success response with transaction ID
                return Results.Ok(new
                {
                    success = true,
                    message = $"Payment successful! Transaction ID: {response.TransactionId}",
                    data = new {
                        transactionId = response.TransactionId
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

    /// <summary>
    /// Configures the marketplace payment processing endpoint with automatic fee splitting.
    /// </summary>
    /// <param name="app">The web application to configure</param>
    private static void ConfigureMarketplaceEndpoint(WebApplication app)
    {
        app.MapPost("/process-marketplace-payment", async (HttpContext context) =>
        {
            // Parse form data from the request
            var form = await context.Request.ReadFormAsync();
            var token = form["payment_token"].ToString();
            var billingZip = form["billing_zip"].ToString();
            var amountStr = form["amount"].ToString();
            var sellerId = form["seller_id"].ToString();
            var platformFeeRateStr = form["platform_fee_rate"].ToString();

            // Validate required fields
            if (string.IsNullOrEmpty(token) ||
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

            // Initialize payment data using tokenized card information
            var card = new CreditCardData
            {
                Token = token
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
