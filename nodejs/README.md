# Node.js — Embedded Payments Fee Splitting

Node.js/Express implementation of embedded payments processing with automatic fee splitting using the Global Payments GP-API. Card data is submitted directly to the server — no hosted fields or client-side tokenization required.

## Requirements

- Node.js 18+
- npm
- Global Payments developer account with GP-API credentials

## Project Structure

```
nodejs/
├── server.js           # Express server — POST /process-embedded-payments-payment
├── index.html          # Payment form frontend
├── package.json        # globalpayments-api + dotenv + multer
├── lib/
│   ├── SellerManager.js   # Loads and validates sellers
│   └── SplitCalculator.js # Fee split logic
├── data/
│   └── sellers.json       # Mock seller registry
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Install dependencies**
```bash
npm install
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
PORT=8000
```

**3. Start the server**
```bash
npm start
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
| `PORT` | Server port | no | `8000` |

## SDK Configuration

```javascript
import {
    ServicesContainer,
    GpApiConfig,
    Channel,
} from 'globalpayments-api';
import * as dotenv from 'dotenv';

dotenv.config();

const config = new GpApiConfig();
config.appId = process.env.GP_APP_ID;
config.appKey = process.env.GP_APP_KEY;
config.environment = process.env.GP_API_ENVIRONMENT === 'PRODUCTION'
    ? 'production'
    : 'test';
config.channel = Channel.CardNotPresent;
config.country = 'US';
ServicesContainer.configureService(config);
```

## API Endpoint

### POST /process-embedded-payments-payment

Processes a charge and returns a transaction ID with fee split breakdown. Accepts both `application/json` and `multipart/form-data`.

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
| `platform_fee_rate` | float | no | Platform fee % 5–25 (default: env value or `10`) |

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

Error codes: `PAYMENT_DECLINED`, `API_ERROR`, `SERVER_ERROR`

## Fee Calculation

```javascript
// SplitCalculator logic
const processingFee = (amount * 0.029) + 0.30;
const platformFee   = amount * (platformFeeRate / 100);
const sellerPayout  = amount - processingFee - platformFee;
```

| Component | Formula | Example ($100.00, 10%) |
|-----------|---------|------------------------|
| Processing Fee | `(amount × 2.9%) + $0.30` | $3.20 |
| Platform Fee | `amount × platformFeeRate` | $10.00 |
| Seller Payout | `amount − processingFee − platformFee` | $86.80 |

## Payment Processing Flow

```javascript
// 1. Validate fields and seller
if (!SellerManager.isValidSeller(seller_id)) { /* 400 */ }
const seller = SellerManager.getSellerById(seller_id);

// 2. Calculate fee split
const calculator = new SplitCalculator(platformFeeRate);
const splitDetails = calculator.calculateSplit(amountNum);

// 3. Parse expiry and build CreditCardData
const [expiryMonth, expiryYearShort] = card_expiry.split('/');
const expiryYear = '20' + expiryYearShort;

const card = new CreditCardData();
card.cardHolderName = card_name;
card.number = card_number.replace(/\s/g, '');
card.expMonth = expiryMonth.padStart(2, '0');
card.expYear = expiryYear;
card.cvn = card_cvv;

// 4. Process charge
const address = new Address();
address.postalCode = sanitizePostalCode(billing_zip);

const response = await card.charge(amountNum)
    .withAllowDuplicates(true)
    .withCurrency('USD')
    .withAddress(address)
    .execute();
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
docker build -t embedded-fee-splitting-nodejs .
docker run -p 8001:8000 \
  -e GP_APP_ID=your_app_id \
  -e GP_APP_KEY=your_app_key \
  -e GP_API_ENVIRONMENT=TEST \
  embedded-fee-splitting-nodejs
# Open http://localhost:8001
```

Or via docker-compose from the project root:
```bash
docker-compose up nodejs
```

## Troubleshooting

**"Invalid seller selected" (400)**
`seller_id` must match a key in `data/sellers.json`. The payment form populates the seller dropdown automatically — direct API callers must use exact seller IDs from the file.

**"Missing required fields" (400)**
All of `card_name`, `card_number`, `card_expiry`, `card_cvv`, `billing_zip`, `amount`, and `seller_id` are required. The endpoint accepts both JSON body and `multipart/form-data`.

**"Payment processing failed" — GP-API error**
Confirm `GP_APP_ID` and `GP_APP_KEY` in `.env` are for the TEST environment. Restart the server after editing `.env` — environment variables are read at boot. Check the console for the full error message from GP-API.

**`import` syntax error on startup**
The project uses ES module syntax. Confirm `"type": "module"` is present in `package.json` and you are running Node.js 18+. Check with `node --version`.

**Expiry date rejected**
`card_expiry` must be `MM/YY` with a forward slash. The backend splits on `/` — formats without it will return a 400 validation error.

**Seller payout is negative**
For very small amounts or large fee rates, payout can go negative. Validate `platform_fee_rate` at the API boundary for non-form callers; the form enforces 5–25%.
