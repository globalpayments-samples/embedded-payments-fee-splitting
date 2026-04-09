<<<<<<< Updated upstream
# Embedded Payments with Fee Splitting

A comprehensive multi-language demonstration of embedded payment processing with automatic fee splitting using the Global Payments GP API. This example showcases how platforms and marketplaces can process payments on behalf of sellers while automatically calculating and distributing processing fees, platform fees, and seller payouts across multiple programming languages.

## 🚀 Features

### Core Payment Capabilities
- **Embedded Payment Processing** - Process payments on behalf of sellers/merchants via GP API
- **Automatic Fee Splitting** - Real-time calculation of processing fees, platform fees, and seller payouts
- **Configurable Platform Fees** - Adjustable platform fee rate (5-25%, default 10%)
- **Seller Management** - Mock seller data with validation and payout tracking
- **Direct Card Processing** - Server-side card data handling via GP API SDK
=======
# Embedded Payments — Fee Splitting

Complete implementation of an embedded payments integration with automatic fee splitting across 4 programming languages. Uses the Global Payments GP-API to process card payments and calculate per-transaction splits between a platform and its sellers — all without hosted payment pages or client-side tokenization.

This project targets marketplace and platform use cases where the platform takes a configurable fee and pays out the remainder to the seller on each transaction.
>>>>>>> Stashed changes

### Development & Testing
- **GP API Sandbox** - Full sandbox environment for development and testing
- **Mock Seller Data** - Pre-configured sellers with unique identifiers and fee rates
- **Comprehensive Web Interface** - Payment form with seller selection and real-time fee display
- **Consistent API Design** - Identical endpoints and behavior across all implementations

<<<<<<< Updated upstream
### Technical Features
- **Fee Calculation Engine** - `SplitCalculator` class with standard processing fee formula (2.9% + $0.30)
- **Seller Validation** - `SellerManager` class for seller data management and lookup
- **Environment Configuration** - Secure credential management with .env files
- **Structured Error Handling** - Categorized error codes (`PAYMENT_DECLINED`, `API_ERROR`, `SERVER_ERROR`)

## 🌐 Available Implementations

Each implementation provides identical functionality with language-specific best practices:

| Language | Framework | Requirements | Status |
|----------|-----------|--------------|--------|
| **[PHP](./php/)** - ([Preview](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/php)) | Native PHP | PHP 7.4+, Composer | ✅ Complete |
| **[Node.js](./nodejs/)** - ([Preview](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/nodejs)) | Express.js | Node.js 18+, npm | ✅ Complete |
| **[.NET](./dotnet/)** - ([Preview](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/dotnet)) | ASP.NET Core | .NET 9.0+ | ✅ Complete |
| **[Java](./java/)** - ([Preview](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/java)) | Jakarta EE | Java 11+, Maven | ✅ Complete |

## 🏗️ Architecture Overview

### Frontend Architecture
- **Payment Form** - Unified form with seller selection, amount input, and fee preview
- **Real-Time Fee Display** - Client-side fee calculation preview before payment submission
- **Seller Selection** - Dropdown with mock seller data for marketplace simulation
- **Responsive Design** - Clean interface with card input fields and billing information

### Backend Architecture
- **RESTful API Design** - Consistent endpoints across all implementations
- **Direct Card Processing** - Card data sent from frontend, charged via GP API SDK
- **Fee Split Calculation** - `SplitCalculator` computes all fee components per transaction
- **Seller Validation** - `SellerManager` verifies seller existence and retrieves payout details

### How It Works

```
Frontend (index.html) → POST /process-embedded-payments-payment → Backend
    ├── Validates seller (SellerManager)
    ├── Calculates fees (SplitCalculator)
    ├── Processes payment (GP API SDK)
    └── Returns transaction ID + split details
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/process-embedded-payments-payment` | Process payment with automatic fee splitting |

## 🚀 Quick Start

### Prerequisites
- Global Payments GP API account with sandbox credentials ([Sign up here](https://developer.globalpay.com/))
- Development environment for your chosen language
- Package manager (npm, composer, maven, or dotnet)

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/globalpayments-samples/embedded-payments-fee-splitting.git
   cd embedded-payments-fee-splitting
   ```

2. **Choose your implementation**
   ```bash
   cd php  # or nodejs, dotnet, java
   ```

3. **Configure environment**
   ```bash
   cp .env.sample .env
   # Edit .env with your GP API credentials:
   # GP_APP_ID=your_gp_api_app_id_here
   # GP_APP_KEY=your_gp_api_app_key_here
   # GP_API_ENVIRONMENT=TEST
   # PLATFORM_FEE_RATE=10.0
   ```

4. **Install dependencies and run**
   ```bash
   ./run.sh
   ```

   Or manually per language:
   ```bash
   # PHP
   composer install && php -S localhost:8000

   # Node.js
   npm install && npm start

   # .NET
   dotnet restore && dotnet run

   # Java
   mvn clean compile cargo:run
   ```

5. **Access the application**
   Open [http://localhost:8000](http://localhost:8000) in your browser

## 🧪 Development & Testing

### Test Cards (GP API Sandbox)

| Card | Number | CVV | Expiry |
|------|--------|-----|--------|
| **Visa (Approved)** | 4263970000005262 | 123 | Any future date |
| **Visa (Declined)** | 4000120000001154 | 123 | Any future date |

### Fee Calculation Formula
=======
| Language | Framework | SDK | Port | Preview |
|----------|-----------|-----|------|---------|
| [**PHP**](./php/) | Built-in Server | globalpayments/php-sdk | 8003 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/php) |
| [**Node.js**](./nodejs/) | Express.js | globalpayments-api | 8001 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/nodejs) |
| [**.NET**](./dotnet/) | ASP.NET Core | GlobalPayments.Api | 8006 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/dotnet) |
| [**Java**](./java/) | Jakarta Servlet | com.globalpayments:java-sdk | 8004 | [Open in CodeSandbox](https://githubbox.com/globalpayments-samples/embedded-payments-fee-splitting/tree/main/java) |

## How It Works
>>>>>>> Stashed changes

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

<<<<<<< Updated upstream
| Description | Calculation | Amount |
|-------------|-------------|--------|
| Transaction Amount | — | $100.00 |
| Processing Fee | ($100 × 2.9%) + $0.30 | -$3.20 |
| Platform Fee | $100 × 10% | -$10.00 |
| **Seller Payout** | $100 - $3.20 - $10 | **$86.80** |

### Mock Sellers

| Seller ID | Name | Description |
|-----------|------|-------------|
| `seller_001` | Tech Gadgets Store | Electronics and gadgets |
| `seller_002` | Fashion Boutique | Clothing and accessories |
| `seller_003` | Home Goods Co | Furniture and home decor |
=======
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
>>>>>>> Stashed changes

### 2. Choose a Language and Configure

<<<<<<< Updated upstream
| Component | Description |
|-----------|-------------|
| `SellerManager` | Manages seller data, validation, and lookup |
| `SplitCalculator` | Calculates processing fees, platform fees, and seller payouts |
| `mock-sellers.json` | Sample seller data for development |
=======
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
>>>>>>> Stashed changes

## 💳 Payment Flow

1. Buyer selects a seller and enters payment amount on the frontend
2. Frontend displays real-time fee breakdown (processing fee, platform fee, seller payout)
3. Buyer enters card details and billing zip code
4. Frontend sends card data + seller + amount to `POST /process-embedded-payments-payment`
5. Backend validates seller via `SellerManager`
6. Backend calculates fee split via `SplitCalculator`
7. Backend charges the card via GP API SDK
8. Returns transaction ID with complete split details

## 🔧 API Reference

### POST /process-embedded-payments-payment

Processes a charge and returns the transaction ID with a full fee split breakdown.

<<<<<<< Updated upstream
**Request:**
```json
{
  "card_name": "Jane Smith",
  "card_number": "4263970000005262",
  "card_expiry": "12/28",
  "card_cvv": "123",
  "amount": "100.00",
  "seller_id": "seller_001",
  "platform_fee_rate": "10.0",
  "billing_zip": "12345"
}
```

**Success response:**
=======
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
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
**Error response:**
=======
**Validation error (400):**
```json
{
  "success": false,
  "message": "Missing required fields"
}
```

**Payment/API error (400):**
>>>>>>> Stashed changes
```json
{
  "success": false,
  "message": "Payment processing failed",
  "error": {
<<<<<<< Updated upstream
    "code": "PAYMENT_DECLINED",
    "details": "Transaction declined"
  }
}
```

### Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `GP_APP_ID` | Global Payments API App ID | (required) |
| `GP_APP_KEY` | Global Payments API App Key | (required) |
| `GP_API_ENVIRONMENT` | API environment (`TEST` or `PRODUCTION`) | `TEST` |
| `PLATFORM_FEE_RATE` | Default platform fee percentage (5-25%) | `10.0` |

## 🔧 Customization

### Extending Functionality
Each implementation provides a solid foundation for:
- **Custom Fee Structures** - Modify processing fee formula or platform fee ranges
- **Seller Onboarding** - Replace mock sellers with database-backed seller management
- **Payout Processing** - Integrate actual fund disbursement to seller accounts
- **Transaction History** - Add reporting and analytics for fee tracking
- **Multi-Currency Support** - Extend beyond USD for international marketplaces

### Production Considerations
Before deploying to production:
- **Security** - Store credentials in `.env`, never commit to version control
- **HTTPS** - Always use HTTPS in production environments
- **PCI Compliance** - Card data touches the server; production requires full PCI DSS compliance
- **Logging** - Add secure logging with PII protection
- **Error Handling** - Implement comprehensive error recovery and notifications

## 🤝 Contributing

This project serves as a reference implementation for GP API embedded payments integration. When contributing:
- Maintain consistency across all language implementations
- Follow each language's best practices and conventions
- Ensure thorough testing in the sandbox environment
- Update documentation to reflect any changes

## 📄 License

MIT License — see [LICENSE](./LICENSE) for details.

## 🆘 Support

- **Global Payments Developer Portal**: [https://developer.globalpay.com/](https://developer.globalpay.com/)
- **GP API Reference**: [https://developer.globalpay.com/api](https://developer.globalpay.com/api)
- **SDK Documentation**: Language-specific SDK guides in each implementation directory

---

**Note**: This is a demonstration application for development and testing purposes. For production use, implement additional security measures, error handling, and compliance requirements specific to your use case.
=======
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
>>>>>>> Stashed changes
