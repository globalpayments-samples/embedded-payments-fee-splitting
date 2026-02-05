# GPI Embedded Payments Fee Splitting

This project demonstrates embedded payments processing with automatic fee splitting using the Global Payments SDK. It showcases how platforms and marketplaces can process payments while automatically calculating and distributing fees between the platform and sellers.

## Overview

Embedded payments with fee splitting enables platforms to:
- Process payments on behalf of sellers/merchants
- Automatically calculate processing fees
- Apply configurable platform fees
- Calculate seller payouts in real-time

## Available Implementations

- [Node.js](./nodejs/) - Express.js web application
- [Java](./java/) - Jakarta EE servlet-based web application
- [.NET Core](./dotnet/) - ASP.NET Core web application
- [PHP](./php/) - PHP web application

## How Fee Splitting Works

When a payment is processed, fees are calculated and split as follows:

### Fee Calculation Formula

```
Processing Fee = (Amount × 2.9%) + $0.30
Platform Fee   = Amount × (Platform Fee Rate / 100)
Seller Payout  = Amount - Processing Fee - Platform Fee
```

### Example Calculation

For a $100.00 transaction with 10% platform fee:

| Description | Calculation | Amount |
|-------------|-------------|--------|
| Transaction Amount | - | $100.00 |
| Processing Fee | ($100 × 2.9%) + $0.30 | -$3.20 |
| Platform Fee | $100 × 10% | -$10.00 |
| **Seller Payout** | $100 - $3.20 - $10 | **$86.80** |

### Platform Fee Configuration

- Adjustable between 5-25%
- Default: 10%
- Configured via `PLATFORM_FEE_RATE` environment variable or form input

## Architecture

```
Frontend (index.html) → POST /process-embedded-payments-payment → Backend
    ├── Validates seller (SellerManager)
    ├── Calculates fees (SplitCalculator)
    ├── Processes payment (Global Payments SDK)
    └── Returns transaction ID + split details
```

### Key Components

| Component | Description |
|-----------|-------------|
| `SellerManager` | Manages seller data and validation |
| `SplitCalculator` | Calculates fee splits for transactions |
| `index.html` | Payment form with seller selection and fee display |

## API Endpoint

### POST /process-embedded-payments-payment

Processes an embedded payment with automatic fee splitting.

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `card_name` | string | Yes | Cardholder name |
| `card_number` | string | Yes | Card number (no spaces) |
| `card_expiry` | string | Yes | Expiry date (MM/YY format) |
| `card_cvv` | string | Yes | CVV code |
| `amount` | float | Yes | Transaction amount (min $0.50) |
| `seller_id` | string | Yes | Seller identifier |
| `platform_fee_rate` | float | No | Platform fee percentage (5-25%, default 10%) |
| `billing_zip` | string | Yes | Billing postal code |

**Success Response:**

```json
{
  "success": true,
  "message": "Payment successful! Transaction ID: xxx",
  "data": {
    "transactionId": "xxx",
    "amount": 100.00,
    "currency": "USD",
    "splitDetails": {
      "amount": 100.00,
      "processingFee": 3.20,
      "processingFeeRate": 2.9,
      "processingFeeFixed": 0.30,
      "platformFee": 10.00,
      "platformFeeRate": 10,
      "sellerPayout": 86.80,
      "sellerId": "seller_001",
      "sellerName": "Tech Gadgets Store"
    }
  }
}
```

## Quick Start

1. **Choose your implementation** - Navigate to `nodejs/`, `java/`, `dotnet/`, or `php/`
2. **Configure credentials** - Copy `.env.sample` to `.env` and add your Global Payments API keys:
   ```
   GP_APP_ID=your_gp_api_app_id_here
   GP_APP_KEY=your_gp_api_app_key_here
   GP_API_ENVIRONMENT=TEST
   ```
3. **Run the server** - Execute `./run.sh` to install dependencies and start
4. **Test the form** - Open http://localhost:8000 in your browser

## Project Structure

Each implementation follows this structure:

```
<language>/
├── index.html              # Payment form UI
├── .env.sample             # Environment variable template
├── run.sh                  # Startup script
├── lib/ or services/       # Core classes
│   ├── SellerManager       # Seller data management
│   └── SplitCalculator     # Fee calculation logic
└── data/
    └── mock-sellers.json   # Sample seller data
```

### Platform-Specific Entry Points

| Platform | Entry Point |
|----------|-------------|
| Node.js | `server.js` |
| Java | `ProcessPaymentServlet.java` |
| .NET | `Program.cs` |
| PHP | `process-embedded-payments-payment.php` |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `GP_APP_ID` | Global Payments API App ID | (required) |
| `GP_APP_KEY` | Global Payments API App Key | (required) |
| `GP_API_ENVIRONMENT` | API environment (`TEST` or `PRODUCTION`) | `TEST` |
| `PLATFORM_FEE_RATE` | Default platform fee percentage | `10.0` |

## Security Considerations

**CRITICAL FOR PRODUCTION:**

### PCI DSS Compliance
- **HTTPS Required**: Never transmit card data over HTTP
- **No Card Data Storage**: Never log or store complete card numbers, CVV, or PINs
- **Server Security**: Ensure proper server hardening and security patches

### Additional Security Measures
- Input validation and sanitization
- Rate limiting to prevent abuse
- Security headers (CSP, HSTS, X-Frame-Options)
- CSRF protection for form submissions
- Regular security audits

**Note**: This example is for development/testing only. Production implementations require proper security review and PCI DSS compliance certification.

## Prerequisites

- Global Payments account with API credentials
- Development environment for your chosen language
- Package manager (npm, composer, maven, dotnet)
