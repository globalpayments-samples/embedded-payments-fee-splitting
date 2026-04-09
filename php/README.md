# PHP — Embedded Payments Fee Splitting

PHP implementation of embedded payments processing with automatic fee splitting using the Global Payments GP-API. Card data is submitted directly to the server — no hosted fields or client-side tokenization required.

## Requirements

- PHP 8.0+
- Composer
- Global Payments developer account with GP-API credentials

## Project Structure

```
php/
├── process-embedded-payments-payment.php  # POST /process-embedded-payments-payment
├── router.php                             # PHP built-in server router
├── index.html                             # Payment form frontend
├── composer.json                          # globalpayments/php-sdk + phpdotenv
├── lib/
│   ├── SellerManager.php                  # Loads and validates sellers
│   ├── SplitCalculator.php                # Fee split logic
│   └── Utils.php                          # Input helpers
├── data/
│   └── sellers.json                       # Mock seller registry
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Install dependencies**
```bash
composer install
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
php -S localhost:8000 router.php
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

## API Endpoint

### POST /process-embedded-payments-payment

Processes a charge and returns a transaction ID with fee split breakdown.

**Request body (JSON or form-data):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `card_name` | string | yes | Cardholder name |
| `card_number` | string | yes | Card number |
| `card_expiry` | string | yes | Expiry in `MM/YY` format |
| `card_cvv` | string | yes | CVV / security code |
| `billing_zip` | string | yes | Billing postal code |
| `amount` | string | yes | Amount (minimum `"0.50"`) |
| `seller_id` | string | yes | Seller ID from `data/sellers.json` |
| `platform_fee_rate` | float | no | Platform fee % 5–25 (default: env value or `10`) |

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

```php
// SplitCalculator logic
$processingFee = ($amount * 0.029) + 0.30;
$platformFee   = $amount * ($platformFeeRate / 100);
$sellerPayout  = $amount - $processingFee - $platformFee;
```

| Component | Formula | Example ($100.00, 10%) |
|-----------|---------|------------------------|
| Processing Fee | `(amount × 2.9%) + $0.30` | $3.20 |
| Platform Fee | `amount × platformFeeRate` | $10.00 |
| Seller Payout | `amount − processingFee − platformFee` | $86.80 |

## Payment Processing Flow

```php
// 1. Validate required fields and seller
SellerManager::isValidSeller($sellerId);
$seller = SellerManager::getSellerById($sellerId);

// 2. Calculate fee split
$calculator = new SplitCalculator($platformFeeRate);
$splitDetails = $calculator->calculateSplit($amount);

// 3. Build CreditCardData from raw card fields
$card = new CreditCardData();
$card->cardHolderName = $cardName;
$card->number = str_replace(' ', '', $cardNumber);
$card->expMonth = $expiryMonth;  // MM
$card->expYear  = '20' . $expiryYear;  // YYYY
$card->cvn = $cardCvv;

// 4. Charge through GP-API
$address = new Address();
$address->postalCode = sanitizePostalCode($billingZip);

$response = $card->charge($amount)
    ->withAllowDuplicates(true)
    ->withCurrency('USD')
    ->withAddress($address)
    ->execute();
```

## Test Cards

| Brand | Card Number | CVV | Expiry |
|-------|-------------|-----|--------|
| Visa | 4263970000005262 | 123 | Any future date |
| Mastercard | 5425230000004415 | 123 | Any future date |
| Discover | 6011000000000087 | 123 | Any future date |
| Amex | 374101000000608 | 1234 | Any future date |

## Docker

```bash
docker build -t embedded-fee-splitting-php .
docker run -p 8003:8000 \
  -e GP_APP_ID=your_app_id \
  -e GP_APP_KEY=your_app_key \
  -e GP_API_ENVIRONMENT=TEST \
  embedded-fee-splitting-php
# Open http://localhost:8003
```

Or via docker-compose from the project root:
```bash
docker-compose up php
```

## Troubleshooting

**"Invalid seller selected" (400)**
`seller_id` must match a key in `data/sellers.json`. The payment form populates the dropdown automatically — manually submitted requests must use exact seller IDs from the file.

**"Missing required fields" (400)**
All of `card_name`, `card_number`, `card_expiry`, `card_cvv`, `billing_zip`, `amount`, and `seller_id` are required. Missing any one will return 400 without hitting GP-API.

**"Payment processing failed" — GP-API error**
Confirm `GP_APP_ID` and `GP_APP_KEY` in `.env` match a valid TEST environment account. Use test card numbers — the TEST environment does not process real cards. Restart `php -S` after editing `.env`.

**Expiry date rejected**
`card_expiry` must be `MM/YY` — two-digit month, slash, two-digit year. The backend prepends `20` to the year automatically. Formats like `1226` or `12/2026` will fail.

**`composer install` fails**
Requires PHP 8.0+ and Composer 2.x. Confirm with `php -v` and `composer --version`. Missing extensions `ext-curl` or `ext-json` will cause install failures — install via your OS package manager (e.g., `apt install php8-curl php8-json`).

**Seller payout is negative**
If `platform_fee_rate` is set very high relative to a small amount, payout can go negative after the processing fee is deducted. Validate the fee rate range for API callers; the form enforces 5–25%.
