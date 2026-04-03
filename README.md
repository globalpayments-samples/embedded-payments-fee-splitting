# Embedded Payments with Fee Splitting

A comprehensive multi-language demonstration of embedded payment processing with automatic fee splitting using the Global Payments GP API. This example showcases how platforms and marketplaces can process payments on behalf of sellers while automatically calculating and distributing processing fees, platform fees, and seller payouts across multiple programming languages.

## 🚀 Features

### Core Payment Capabilities
- **Embedded Payment Processing** - Process payments on behalf of sellers/merchants via GP API
- **Automatic Fee Splitting** - Real-time calculation of processing fees, platform fees, and seller payouts
- **Configurable Platform Fees** - Adjustable platform fee rate (5-25%, default 10%)
- **Seller Management** - Mock seller data with validation and payout tracking
- **Direct Card Processing** - Server-side card data handling via GP API SDK

### Development & Testing
- **GP API Sandbox** - Full sandbox environment for development and testing
- **Mock Seller Data** - Pre-configured sellers with unique identifiers and fee rates
- **Comprehensive Web Interface** - Payment form with seller selection and real-time fee display
- **Consistent API Design** - Identical endpoints and behavior across all implementations

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

```
Processing Fee = (Amount × 2.9%) + $0.30
Platform Fee   = Amount × (Platform Fee Rate / 100)
Seller Payout  = Amount - Processing Fee - Platform Fee
```

### Example Calculation

For a $100.00 transaction with 10% platform fee:

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

### Key Components

| Component | Description |
|-----------|-------------|
| `SellerManager` | Manages seller data, validation, and lookup |
| `SplitCalculator` | Calculates processing fees, platform fees, and seller payouts |
| `mock-sellers.json` | Sample seller data for development |

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

Processes an embedded payment with automatic fee splitting.

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

**Error response:**
```json
{
  "success": false,
  "message": "Payment processing failed",
  "error": {
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
