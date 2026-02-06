# PHP Embedded Payments Fee Splitting Example

This example demonstrates embedded payments processing with automatic fee splitting using PHP and the Global Payments SDK.

## Requirements

- PHP 7.4 or later
- Composer
- Global Payments account and API credentials
- **HTTPS required for production** (PCI DSS compliance)

## Project Structure

- `process-embedded-payments-payment.php` - Embedded payments processor with fee splitting
- `index.html` - Frontend payment form
- `router.php` - Request router for PHP built-in server
- `composer.json` - Project dependencies
- `.env.sample` - Template for environment variables
- `lib/` - Embedded Payments classes (SellerManager, SplitCalculator, Utils)
- `data/` - Mock seller data
- `run.sh` - Convenience script to run the application

## Setup

1. Clone this repository
2. Copy `.env.sample` to `.env`
3. Update `.env` with your Global Payments API credentials:
   ```
   GP_APP_ID=your_gp_api_app_id_here
   GP_APP_KEY=your_gp_api_app_key_here
   GP_API_ENVIRONMENT=TEST
   ```
4. Install dependencies:
   ```bash
   composer install
   ```
5. Run the application:
   ```bash
   ./run.sh
   ```
   Or manually:
   ```bash
   php -S localhost:8000 router.php
   ```
6. Open your browser to http://localhost:8000

## Implementation Details

### Backend Tokenization Approach
This implementation uses **backend tokenization** where:
- Frontend sends card data directly to the PHP backend
- Backend tokenizes and processes payment using Global Payments SDK
- No client-side tokenization or public API keys needed

**Security Note**: This approach requires HTTPS in production and proper PCI DSS compliance measures.

### Embedded Payments Fee Splitting
Automatic fee calculation and splitting:
1. **Processing Fee**: 2.9% + $0.30 (standard payment processor fee)
2. **Platform Fee**: Configurable 5-25% of transaction amount
3. **Seller Payout**: Remaining amount after fees deducted

### Payment Processing Flow
1. User fills out form with card details, amount, and seller
2. Frontend sends all data to `/process-embedded-payments-payment`
3. Backend validates seller and calculates fee split
4. Backend creates CreditCardData with card details
5. Backend processes charge through Global Payments API
6. Backend returns transaction ID and split details
7. Frontend displays success with breakdown

### Error Handling
Comprehensive error handling:
- Field validation before processing
- API exception catching and logging
- User-friendly error messages
- Detailed server-side error logs

## API Endpoints

### POST /process-embedded-payments-payment
Processes an embedded payment with automatic fee splitting.

Request Parameters:
- `card_name` (string, required) - Cardholder name
- `card_number` (string, required) - Card number (no spaces)
- `card_expiry` (string, required) - Expiry date (MM/YY format)
- `card_cvv` (string, required) - CVV code
- `amount` (float, required) - Transaction amount (min $0.50)
- `seller_id` (string, required) - Seller identifier
- `platform_fee_rate` (float, optional) - Platform fee percentage (5-25%, default 10%)
- `billing_zip` (string, required) - Billing postal code

Response (Success):
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
      "platformFee": 10.00,
      "sellerPayout": 86.80,
      "sellerId": "seller_001",
      "sellerName": "Tech Gadgets Store"
    }
  }
}
```

Response (Error):
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

## Security Considerations

**CRITICAL FOR PRODUCTION:**

### PCI DSS Compliance
- **HTTPS Required**: NEVER transmit card data over HTTP
- **No Card Data Storage**: Never log or store complete card numbers, CVV, or PINs
- **Server Security**: Ensure proper server hardening and security patches
- **Network Segmentation**: Isolate payment processing from other systems

### Additional Security Measures
- Input validation and sanitization on all user inputs
- Rate limiting to prevent abuse
- Security headers (CSP, HSTS, X-Frame-Options)
- CSRF protection for form submissions
- Detailed error logging (without exposing sensitive data)
- Regular security audits and penetration testing
- Fraud detection and prevention measures

### Alternative Approach
For simplified PCI compliance, consider:
- Using hosted payment pages
- Implementing client-side tokenization (requires different setup)
- Using payment gateways that handle card data entirely

**Note**: This example is for development/testing only. Production implementations require proper security review and PCI DSS compliance certification.
