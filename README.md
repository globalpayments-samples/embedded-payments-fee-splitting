# Embedded Payments — Fee Splitting

Complete implementation of an embedded payments integration with automatic fee splitting across 4 programming languages. Uses the Global Payments GP-API to process card payments and calculate per-transaction splits between a platform and its sellers — all without hosted payment pages or client-side tokenization.

This project targets marketplace and platform use cases where the platform takes a configurable fee and pays out the remainder to the seller on each transaction.

## Available Implementations

| Language | Framework | SDK | Port | Preview |
|----------|-----------|-----|------|---------|
| [**PHP**](./php/) | Built-in Server | globalpayments/php-sdk | 8003 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/php) |
| [**Node.js**](./nodejs/) | Express.js | globalpayments-api | 8001 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/nodejs) |
| [**.NET**](./dotnet/) | ASP.NET Core | GlobalPayments.Api | 8006 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/dotnet) |
| [**Java**](./java/) | Jakarta Servlet | com.globalpayments:java-sdk | 8004 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/java) |

## How It Works

```
Browser                          Backend                         GP-API
   │                                │                               │
   │  [User fills payment form]     │                               │
   │  [Selects seller + amount]     │                               │
   │                                │                               │
   │── POST /process-embedded-payments-payment ──────────────────>  │
   │   card_name, card_number       │                               │
   │   card_expiry, card_cvv        │                               │
   │   billing_zip, amount          │── SellerManager.validate() ──>│ (local)
   │   seller_id, platform_fee_rate │── SplitCalculator.split() ───>│ (local)
   │                                │                               │
   │                                │── card.charge(amount) ───────>│
   │                                │   withCurrency('USD')         │
   │                                │   withAddress(billingZip)     │
   │                                │<─ { transactionId } ──────────│
   │                                │                               │
   │<─ { transactionId,             │                               │
   │     splitDetails } ────────────│                               │
```

## Fee Splitting

Each transaction is split into three components, calculated server-side:

| Component | Formula | Example ($100.00) |
|-----------|---------|-------------------|
| Processing Fee | `(amount × 2.9%) + $0.30` | $3.20 |
| Platform Fee | `amount × platformFeeRate` | $10.00 (10%) |
| Seller Payout | `amount − processingFee − platformFee` | $86.80 |

The `platform_fee_rate` defaults to 10% but can be passed per-request (valid range: 5–25%).

## Use Cases

| Scenario | How to Configure |
|----------|-----------------|
| Fixed platform fee | Set `PLATFORM_FEE_RATE=10` in `.env` |
| Per-transaction rate | Pass `platform_fee_rate` in request body |
| Marketplace checkout | Populate `seller_id` from seller list |
| Platform as primary | Keep seller list in `data/sellers.json` |

## Prerequisites

- Global Payments developer account
- GP-API credentials (`GP_APP_ID` and `GP_APP_KEY`) from the [developer portal](https://developer.globalpay.com/)
- Docker, or runtime for your chosen language (PHP 8.0+, Node.js 18+, .NET 9+, Java 17+)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/globalpayments-samples/embedded-payments-fee-splitting.git
cd embedded-payments-fee-splitting
```

### 2. Choose a Language and Configure

```bash
cd php   # or nodejs, dotnet, java
cp .env.sample .env
```

Edit `.env`:
```env
GP_APP_ID=your_app_id_here
GP_APP_KEY=your_app_key_here
GP_API_ENVIRONMENT=TEST
PLATFORM_FEE_RATE=10
```

### 3. Install, Build, and Run

**PHP:**
```bash
composer install
php -S localhost:8000 router.php
# Open http://localhost:8000
```

**Node.js:**
```bash
npm install && npm start
# Open http://localhost:8000
```

**.NET:**
```bash
dotnet restore && dotnet run
# Open http://localhost:8000
```

**Java:**
```bash
mvn clean package && mvn jetty:run
# Open http://localhost:8000
```

### 4. Test a Payment

1. Open the app in your browser
2. Enter an amount (minimum $0.50)
3. Select a seller from the dropdown
4. Fill in a test card number
5. Submit and verify the fee split breakdown in the response

## Docker Setup

```bash
cp php/.env.sample .env   # all language services share the same variables
```

Edit `.env` with your GP-API credentials, then:

```bash
docker-compose up
```

| Service | External Port | URL |
|---------|--------------|-----|
| nodejs  | 8001 | http://localhost:8001 |
| php     | 8003 | http://localhost:8003 |
| java    | 8004 | http://localhost:8004 |
| dotnet  | 8006 | http://localhost:8006 |

Run a single service:
```bash
docker-compose up nodejs
```

## API Endpoint

### POST /process-embedded-payments-payment

Processes a charge and returns the transaction ID with a full fee split breakdown.

**Request fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `card_name` | string | yes | Cardholder name |
| `card_number` | string | yes | Card number (spaces stripped automatically) |
| `card_expiry` | string | yes | Expiry date in `MM/YY` format |
| `card_cvv` | string | yes | CVV / security code |
| `billing_zip` | string | yes | Billing postal code |
| `amount` | string | yes | Transaction amount (minimum `"0.50"`) |
| `seller_id` | string | yes | Seller identifier (must match a record in `data/sellers.json`) |
| `platform_fee_rate` | float | no | Platform fee percentage 5–25 (default: env `PLATFORM_FEE_RATE` or `10`) |

**Example request:**
```json
{
  "card_name": "Jane Doe",
  "card_number": "4263970000005262",
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

**Validation error (400):**
```json
{
  "success": false,
  "message": "Missing required fields"
}
```

**Payment/API error (400):**
```json
{
  "success": false,
  "message": "Payment processing failed",
  "error": {
    "code": "API_ERROR",
    "details": "Error message details"
  }
}
```

Error codes: `PAYMENT_DECLINED`, `API_ERROR`, `SERVER_ERROR`

## Payment Processing Flow

Each language implementation follows the same server-side sequence:

**Step 1 — Validate input**
```
All required fields present.
seller_id must exist in SellerManager.
amount must be ≥ $0.50.
card_expiry must be MM/YY format.
```

**Step 2 — Calculate fee split**
```
SplitCalculator.calculateSplit(amount, platformFeeRate):
  processingFee = (amount × 0.029) + 0.30
  platformFee   = amount × (platformFeeRate / 100)
  sellerPayout  = amount − processingFee − platformFee
```

**Step 3 — Build CreditCardData**
```
CreditCardData:
  cardHolderName = card_name
  number         = card_number (spaces stripped)
  expMonth       = MM (from card_expiry)
  expYear        = YYYY (20 + YY from card_expiry)
  cvn            = card_cvv
```

**Step 4 — Charge through GP-API**
```
card.charge(amount)
    .withCurrency('USD')
    .withAddress(billingZip)
    .execute()
Returns transactionId on success.
```

**Step 5 — Return combined result**
```
Response includes transactionId + full splitDetails breakdown.
```

## SDK Configuration

Each language initializes the GP-API SDK identically at startup:

**PHP:**
```php
use GlobalPayments\Api\ServiceConfigs\Gateways\GpApiConfig;
use GlobalPayments\Api\ServicesContainer;
use GlobalPayments\Api\Entities\Enums\Environment;
use GlobalPayments\Api\Entities\Enums\Channel;

$config = new GpApiConfig();
$config->appId = $_ENV['GP_APP_ID'];
$config->appKey = $_ENV['GP_APP_KEY'];
$config->environment = Environment::TEST;
$config->channel = Channel::CardNotPresent;
$config->country = 'US';

ServicesContainer::configureService($config);
```

**Node.js:**
```javascript
import { ServicesContainer, GpApiConfig, Channel } from 'globalpayments-api';

const config = new GpApiConfig();
config.appId = process.env.GP_APP_ID;
config.appKey = process.env.GP_APP_KEY;
config.environment = process.env.GP_API_ENVIRONMENT === 'PRODUCTION' ? 'production' : 'test';
config.channel = Channel.CardNotPresent;
config.country = 'US';
ServicesContainer.configureService(config);
```

**.NET:**
```csharp
using GlobalPayments.Api;
using GlobalPayments.Api.Entities.Enums;

ServicesContainer.ConfigureService(new GpApiConfig
{
    AppId = Environment.GetEnvironmentVariable("GP_APP_ID"),
    AppKey = Environment.GetEnvironmentVariable("GP_APP_KEY"),
    Environment = Entities.Enums.Environment.TEST,
    Channel = Channel.CardNotPresent,
    Country = "US"
});
```

**Java:**
```java
import com.global.api.ServicesContainer;
import com.global.api.serviceConfigs.GpApiConfig;
import com.global.api.entities.enums.Environment;
import com.global.api.entities.enums.Channel;

GpApiConfig config = new GpApiConfig();
config.setAppId(System.getenv("GP_APP_ID"));
config.setAppKey(System.getenv("GP_APP_KEY"));
config.setEnvironment(Environment.TEST);
config.setChannel(Channel.CardNotPresent);
config.setCountry("US");
ServicesContainer.configureService(config);
```

## Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `GP_APP_ID` | GP-API application ID | yes | — |
| `GP_APP_KEY` | GP-API application key | yes | — |
| `GP_API_ENVIRONMENT` | `TEST` or `PRODUCTION` | no | `TEST` |
| `PLATFORM_FEE_RATE` | Default platform fee percentage (5–25) | no | `10` |

Obtain credentials from your [Global Payments developer account](https://developer.globalpay.com/).

## Test Cards

| Brand | Card Number | CVV | Expiry |
|-------|-------------|-----|--------|
| Visa | 4263970000005262 | 123 | Any future date |
| Mastercard | 5425230000004415 | 123 | Any future date |
| Discover | 6011000000000087 | 123 | Any future date |
| Amex | 374101000000608 | 1234 | Any future date |

Additional test cards: [developer.globalpay.com/resources/test-cards](https://developer.globalpay.com/resources/test-cards)

## Project Structure

```
embedded-payments-fee-splitting/
├── docker-compose.yml      # Multi-service Docker config
├── README.md               # This file
├── LICENSE
├── php/                    # PHP implementation (Docker: 8003)
│   ├── process-embedded-payments-payment.php
│   ├── router.php
│   ├── composer.json
│   ├── lib/                # SellerManager, SplitCalculator, Utils
│   ├── data/               # sellers.json
│   ├── .env.sample
│   └── README.md
├── nodejs/                 # Node.js implementation (Docker: 8001)
│   ├── server.js
│   ├── package.json
│   ├── lib/                # SellerManager.js, SplitCalculator.js
│   ├── data/               # sellers.json
│   ├── .env.sample
│   └── README.md
├── dotnet/                 # .NET implementation (Docker: 8006)
│   ├── Program.cs
│   ├── *.csproj
│   ├── Services/           # SellerManager, SplitCalculator
│   ├── Models/             # Seller, SplitDetails
│   ├── data/               # sellers.json
│   ├── .env.sample
│   └── README.md
└── java/                   # Java implementation (Docker: 8004)
    ├── src/
    │   └── main/java/com/globalpayments/example/
    │       ├── ProcessPaymentServlet.java
    │       ├── services/   # SellerManager, SplitCalculator
    │       └── models/     # Seller, SplitDetails
    ├── data/               # sellers.json
    ├── pom.xml
    ├── .env.sample
    └── README.md
```

## Security Notes

This project uses **backend card processing** — card data is sent from the browser directly to your server, bypassing client-side tokenization. This approach gives full control over the processing flow but increases PCI scope.

For production use:
- **HTTPS is required** — never transmit card data over HTTP
- **Do not log card numbers, CVVs, or expiry dates** at any step
- **PCI DSS compliance** is the deployer's responsibility in this model
- Consider hosted payment pages or client-side tokenization to reduce PCI scope

## Troubleshooting

**"Invalid seller selected" (400)**
`seller_id` must exactly match a key in `data/sellers.json`. Use the seller dropdown in the form, which is populated from the configured seller list.

**"Amount must be at least $0.50" (400)**
GP-API enforces a minimum charge of $0.50. Confirm the `amount` field is ≥ 0.50.

**"Missing required fields" (400)**
All of `card_name`, `card_number`, `card_expiry`, `card_cvv`, `billing_zip`, `amount`, and `seller_id` are required. `platform_fee_rate` is optional.

**"Payment processing failed" — GP-API error**
Confirm `GP_APP_ID` and `GP_APP_KEY` are set correctly for the TEST environment. GP-API credentials from the developer portal start with app ID and app key values. Use test card numbers from the table above — TEST environment does not charge real cards.

**Expiry format rejected**
`card_expiry` must be `MM/YY` with a forward slash separator. Two-digit year only — the backend prepends `20` automatically. `12/2026` or `122026` will fail validation.

**Seller payout is negative**
If `platform_fee_rate` is very high relative to a small amount, payout can go negative after fees. The fee rate is capped at 25% in the form, but validate server-side for direct API callers.

**`mvn jetty:run` port conflict (Java)**
Jetty defaults to port 8080. The project run script maps to 8000. If 8080 is occupied, stop the conflicting process with `lsof -i :8080` or update the port in `pom.xml`.

**`dotnet run` fails to resolve packages**
Requires .NET 9+. Confirm with `dotnet --version`. Run `dotnet restore` to pull fresh packages. If `GlobalPayments.Api` fails to resolve, clear the NuGet cache: `dotnet nuget locals all --clear`.

## Per-Language Documentation

- [PHP README](./php/README.md)
- [Node.js README](./nodejs/README.md)
- [.NET README](./dotnet/README.md)
- [Java README](./java/README.md)

## External Resources

- [Global Payments Developer Portal](https://developer.globalpay.com/)
- [GP-API Documentation](https://developer.globalpay.com/ecommerce)
- [Test Cards](https://developer.globalpay.com/resources/test-cards)

## License

[MIT](./LICENSE)
