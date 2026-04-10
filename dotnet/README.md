# .NET — Embedded Payments Fee Splitting

ASP.NET Core implementation of embedded payments processing with automatic fee splitting using the Global Payments GP-API. Card data is submitted directly to the server — no hosted fields or client-side tokenization required.

## Requirements

- .NET 9.0+
- Global Payments developer account with GP-API credentials

## Project Structure

```
dotnet/
├── Program.cs              # ASP.NET Core minimal API — POST /process-embedded-payments-payment
├── wwwroot/
│   └── index.html          # Payment form frontend (served as static file)
├── Services/
│   ├── SellerManager.cs    # Loads and validates sellers
│   └── SplitCalculator.cs  # Fee split logic
├── Models/
│   ├── Seller.cs           # Seller entity
│   └── SplitDetails.cs     # Fee split result model
├── data/
│   └── sellers.json        # Mock seller registry
├── *.csproj                # GlobalPayments.Api + dotenv.net
├── appsettings.json
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Restore dependencies**
```bash
dotnet restore
```

**2. Configure credentials**
```bash
cp .env.sample .env
```

Edit `.env`:
```env
GP_APP_ID=your_app_id_here
GP_APP_KEY=your_app_key_here
GP_API_ENVIRONMENT=TEST
PLATFORM_FEE_RATE=10
```

**3. Start the server**
```bash
dotnet run
# Open http://localhost:8000
```

Or use the convenience script:
```bash
./run.sh
```

## Environment Variables

| Variable | Description | Required | Example |
|----------|-------------|----------|---------|
| `GP_APP_ID` | GP-API application ID | yes | `P21Reaz4vIdxKiGB9sRY1lzuM8aK` |
| `GP_APP_KEY` | GP-API application key | yes | `oCGX7NwcNREGTjsq` |
| `GP_API_ENVIRONMENT` | `TEST` or `PRODUCTION` | no | `TEST` |
| `PLATFORM_FEE_RATE` | Default platform fee % (5–25) | no | `10` |

## SDK Configuration

Configured once at startup in `Program.cs`:

```csharp
using GlobalPayments.Api;
using GlobalPayments.Api.Entities.Enums;
using dotenv.net;

DotEnv.Load();

ServicesContainer.ConfigureService(new GpApiConfig
{
    AppId = Environment.GetEnvironmentVariable("GP_APP_ID"),
    AppKey = Environment.GetEnvironmentVariable("GP_APP_KEY"),
    Environment = Entities.Enums.Environment.TEST,
    Channel = Channel.CardNotPresent,
    Country = "US"
});
```

## API Endpoint

### POST /process-embedded-payments-payment

Processes a charge and returns a transaction ID with fee split breakdown.

**Request fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `card_name` | string | yes | Cardholder name |
| `card_number` | string | yes | Card number (spaces stripped) |
| `card_expiry` | string | yes | Expiry in `MM/YY` format |
| `card_cvv` | string | yes | CVV / security code |
| `billing_zip` | string | yes | Billing postal code |
| `amount` | string | yes | Amount (minimum `"0.50"`) |
| `seller_id` | string | yes | Seller ID from `data/sellers.json` |
| `platform_fee_rate` | decimal | no | Platform fee % 5–25 (default: env value or `10`) |

**Example request:**
```json
{
  "card_name": "Jane Doe",
  "card_number": "4263982640269299",
  "card_expiry": "12/26",
  "card_cvv": "123",
  "billing_zip": "12345",
  "amount": "100.00",
  "seller_id": "seller_001",
  "platform_fee_rate": 10
}
```

**Success response (200):**
```json
{
  "success": true,
  "message": "Payment successful! Transaction ID: TXN_ABC123",
  "data": {
    "transactionId": "TXN_ABC123",
    "amount": 100.00,
    "currency": "USD",
    "splitDetails": {
      "amount": 100.00,
      "processingFee": 3.20,
      "platformFee": 10.00,
      "sellerPayout": 86.80,
      "sellerId": "seller_001",
      "sellerName": "Tech Gadgets Store"
    }
  }
}
```

**Error response (400):**
```json
{
  "success": false,
  "message": "Payment processing failed",
  "error": {
    "code": "API_ERROR",
    "details": "Error message"
  }
}
```

## Fee Calculation

```csharp
// SplitCalculator logic
decimal processingFee = (amount * 0.029m) + 0.30m;
decimal platformFee   = amount * (platformFeeRate / 100m);
decimal sellerPayout  = amount - processingFee - platformFee;
```

| Component | Formula | Example ($100.00, 10%) |
|-----------|---------|------------------------|
| Processing Fee | `(amount × 2.9%) + $0.30` | $3.20 |
| Platform Fee | `amount × platformFeeRate` | $10.00 |
| Seller Payout | `amount − processingFee − platformFee` | $86.80 |

## Payment Processing Flow

```csharp
// 1. Validate fields and seller
if (!_sellerManager.IsValidSeller(sellerId)) { /* 400 */ }
var seller = _sellerManager.GetSellerById(sellerId);

// 2. Calculate fee split
var calculator = new SplitCalculator(platformFeeRate);
var splitDetails = calculator.CalculateSplit(amount);

// 3. Parse expiry and build CreditCardData
var expiryParts = cardExpiry.Split('/');
var card = new CreditCardData
{
    CardHolderName = cardName,
    Number = cardNumber.Replace(" ", ""),
    ExpMonth = expiryParts[0].PadLeft(2, '0'),
    ExpYear = "20" + expiryParts[1],
    Cvn = cardCvv
};

// 4. Process charge
var address = new Address { PostalCode = SanitizePostalCode(billingZip) };
var response = await card.Charge(amount)
    .WithAllowDuplicates(true)
    .WithCurrency("USD")
    .WithAddress(address)
    .Execute();
```

## Test Cards

| Brand | Card Number | CVV | Expiry |
|-------|-------------|-----|--------|
| Visa | 4263982640269299 | 123 | Any future date |
| Mastercard | 5425233424241200 | 123 | Any future date |
| Discover | 6011000000000087 | 123 | Any future date |
| Amex | 374101000000608 | 1234 | Any future date |

## Docker

```bash
docker build -t embedded-fee-splitting-dotnet .
docker run -p 8006:8000 \
  -e ASPNETCORE_URLS=http://+:8000 \
  -e GP_APP_ID=your_app_id \
  -e GP_APP_KEY=your_app_key \
  -e GP_API_ENVIRONMENT=TEST \
  embedded-fee-splitting-dotnet
# Open http://localhost:8006
```

Or via docker-compose from the project root:
```bash
docker-compose up dotnet
```

## Troubleshooting

**"Invalid seller selected" (400)**
`seller_id` must match an entry in `data/sellers.json`. The form populates the seller dropdown automatically — direct API callers must use exact seller IDs.

**"Missing required fields" (400)**
All of `card_name`, `card_number`, `card_expiry`, `card_cvv`, `billing_zip`, `amount`, and `seller_id` are required. Confirm all fields are included in the request body.

**"Payment processing failed" — GP-API error**
Confirm `GP_APP_ID` and `GP_APP_KEY` in `.env` are valid TEST credentials. The SDK reads environment variables at startup via `DotEnv.Load()` — restart `dotnet run` after editing `.env`. Check the console for the raw error from GP-API.

**Static files not served (index.html 404)**
The frontend is served from `wwwroot/`. Ensure `app.UseStaticFiles()` and `app.UseDefaultFiles()` are both present in `Program.cs` and that `wwwroot/index.html` exists.

**`dotnet run` fails to resolve packages**
Requires .NET 9.0+. Confirm with `dotnet --version`. Run `dotnet restore` to pull fresh packages. If `GlobalPayments.Api` fails, clear the NuGet cache: `dotnet nuget locals all --clear`.

**Expiry date rejected**
`card_expiry` must be `MM/YY` — two digits, slash, two digits. The backend splits on `/` and prepends `20` to the year. Formats like `12/2026` or `1226` will fail parsing.
