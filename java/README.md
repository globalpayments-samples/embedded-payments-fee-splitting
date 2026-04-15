# Java — Embedded Payments Fee Splitting

Jakarta EE/Servlet implementation of embedded payments processing with automatic fee splitting using the Global Payments GP-API. Card data is submitted directly to the server — no hosted fields or client-side tokenization required.

## Requirements

- Java 17+
- Maven 3.8+
- Global Payments developer account with GP-API credentials

## Project Structure

```
java/
├── src/
│   └── main/
│       ├── java/com/globalpayments/example/
│       │   ├── ProcessPaymentServlet.java  # POST /process-embedded-payments-payment
│       │   ├── services/
│       │   │   ├── SellerManager.java      # Loads and validates sellers
│       │   │   └── SplitCalculator.java    # Fee split logic
│       │   └── models/
│       │       ├── Seller.java             # Seller entity
│       │       └── SplitDetails.java       # Fee split result model
│       └── webapp/
│           ├── index.html                  # Payment form frontend
│           └── WEB-INF/web.xml             # Servlet configuration
├── data/
│   └── sellers.json                        # Mock seller registry
├── pom.xml                                 # com.globalpayments:java-sdk dependency
├── .env.sample
├── Dockerfile
├── run.sh
├── .devcontainer/
└── .codesandbox/
```

## Setup

**1. Build the project**
```bash
mvn clean package
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
mvn jetty:run
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

Export credentials before running:
```bash
export GP_APP_ID=your_app_id
export GP_APP_KEY=your_app_key
mvn jetty:run
```

## SDK Configuration

Configured at servlet initialization in `ProcessPaymentServlet.java`:

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

## API Endpoint

### POST /process-embedded-payments-payment

Processes a charge and returns a transaction ID with fee split breakdown.

**Request fields (JSON body):**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `card_name` | string | yes | Cardholder name |
| `card_number` | string | yes | Card number (spaces stripped) |
| `card_expiry` | string | yes | Expiry in `MM/YY` format |
| `card_cvv` | string | yes | CVV / security code |
| `billing_zip` | string | yes | Billing postal code |
| `amount` | string | yes | Amount (minimum `"0.50"`) |
| `seller_id` | string | yes | Seller ID from `data/sellers.json` |
| `platform_fee_rate` | double | no | Platform fee % 5–25 (default: env value or `10`) |

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

```java
// SplitCalculator logic
BigDecimal processingFee = amount.multiply(new BigDecimal("0.029"))
    .add(new BigDecimal("0.30"));
BigDecimal platformFee   = amount.multiply(platformFeeRate.divide(new BigDecimal("100")));
BigDecimal sellerPayout  = amount.subtract(processingFee).subtract(platformFee);
```

| Component | Formula | Example ($100.00, 10%) |
|-----------|---------|------------------------|
| Processing Fee | `(amount × 2.9%) + $0.30` | $3.20 |
| Platform Fee | `amount × platformFeeRate` | $10.00 |
| Seller Payout | `amount − processingFee − platformFee` | $86.80 |

## Payment Processing Flow

```java
// 1. Validate fields and seller
if (!sellerManager.isValidSeller(sellerId)) { /* 400 */ }
Seller seller = sellerManager.getSellerById(sellerId);

// 2. Calculate fee split
SplitCalculator calculator = new SplitCalculator(platformFeeRate);
SplitDetails splitDetails = calculator.calculateSplit(amount);

// 3. Parse expiry and build CreditCardData
String[] expiryParts = cardExpiry.split("/");
CreditCardData card = new CreditCardData();
card.setCardHolderName(cardName);
card.setNumber(cardNumber.replaceAll("\\s", ""));
card.setExpMonth(Integer.parseInt(expiryParts[0]));
card.setExpYear(Integer.parseInt("20" + expiryParts[1]));
card.setCvn(cardCvv);

// 4. Process charge
Address address = new Address();
address.setPostalCode(sanitizePostalCode(billingZip));

Transaction response = card.charge(amount)
    .withAllowDuplicates(true)
    .withCurrency("USD")
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
docker build -t embedded-fee-splitting-java .
docker run -p 8004:8000 \
  -e GP_APP_ID=your_app_id \
  -e GP_APP_KEY=your_app_key \
  -e GP_API_ENVIRONMENT=TEST \
  embedded-fee-splitting-java
# Open http://localhost:8004
```

Or via docker-compose from the project root:
```bash
docker-compose up java
```

## Troubleshooting

**"Invalid seller selected" (400)**
`seller_id` must match an entry in `data/sellers.json`. The form populates the seller dropdown automatically — direct API callers must use exact seller IDs from the file.

**"Missing required fields" (400)**
All of `card_name`, `card_number`, `card_expiry`, `card_cvv`, `billing_zip`, `amount`, and `seller_id` are required. Confirm all fields are present in the JSON request body.

**"Payment processing failed" — GP-API error**
Confirm `GP_APP_ID` and `GP_APP_KEY` are exported as shell environment variables before running `mvn jetty:run`. System environment variables take precedence over `.env` values in Java. Use test card numbers — the TEST environment does not process real cards.

**Maven build fails**
Requires Java 17+ and Maven 3.8+. Confirm with `java -version` and `mvn -v`. If the `com.globalpayments:java-sdk` dependency fails to resolve, run `mvn clean package -U` to force a fresh dependency download.

**Port conflict on startup**
The project configures Jetty to use port 8000. If that port is occupied, stop the conflicting process with `lsof -i :8000` or update the port configuration in `pom.xml` under the Jetty plugin settings.

**Expiry date rejected**
`card_expiry` must be `MM/YY` — two digits, slash, two digits. The backend splits on `/` and prepends `20` to the year. Formats like `12/2026` or `1226` will fail parsing.
