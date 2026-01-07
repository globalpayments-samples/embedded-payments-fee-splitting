# Global Payments SDK Starter Template

This starter template provides a customizable foundation for Global Payments SDK integration across multiple programming languages. Each implementation includes basic SDK setup, configuration management, and placeholder endpoints that you can modify for your specific payment use cases.

## Available Implementations

- [.NET Core](./dotnet/) - ASP.NET Core web application
- [Go](./go/) - Go HTTP server application
- [Java](./java/) - Jakarta EE servlet-based web application
- [Node.js](./nodejs/) - Express.js web application
- [PHP](./php/) - PHP web application
- [Python](./python/) - Flask web application

## Template Features

- **SDK Configuration** - Basic setup with environment variables
- **Placeholder Endpoints** - Ready-to-customize API endpoints
- **Error Handling** - Basic error handling structure
- **Client Integration** - HTML form with hosted fields tokenization
- **Multiple Languages** - Consistent structure across all implementations
- **Drop-in UI Support** - Access token-based authentication with automatic refresh

## Drop-in UI Implementation

This template uses the **Drop-in UI** approach with access token-based authentication for enhanced security.

### Key Features

- **Access Token Authentication** - Uses temporary, restricted access tokens instead of static public API keys
- **Automatic Token Refresh** - Tokens auto-refresh every 8 minutes (before 10-minute expiry)
- **Enhanced Security** - Restricted permissions (`PMT_POST_Create_Single`) limit token capabilities
- **Dynamic Environment** - Automatically configures sandbox/production mode based on backend settings (.NET only)
- **Enhanced Form Features** - Includes cardholder name requirement, field validation, and card type restrictions

### Implementation Details

**Frontend (All Implementations):**
- Fetches access token from `/get-access-token` endpoint (POST request)
- Configures Global Payments SDK with `accessToken`, `apiVersion: '2021-03-22'`, and `env`
- Implements token refresh every 8 minutes via `setInterval`
- Enhanced card form with `requireCardHolderName`, `fieldValidation`, and `allowedCardTypes`

**Backend Endpoints:**
- `GET /config` - Returns public API key (maintained for backward compatibility)
- `POST /get-access-token` - Generates restricted access token for client-side tokenization

**Token Generation Process:**
1. Generate cryptographic nonce
2. Hash with SHA512 (nonce + app key)
3. Request token from Global Payments with `PMT_POST_Create_Single` permission
4. Return token with 10-minute expiration

### Migration from Public API Key

Previous implementations used:
```javascript
GlobalPayments.configure({ publicApiKey: config.data.publicApiKey });
```

New Drop-in UI approach uses:
```javascript
GlobalPayments.configure({
    accessToken: tokenData.token,
    apiVersion: '2021-03-22',
    env: 'sandbox'  // or 'production'
});
```

### Implementations Updated

- ✅ **Node.js** - Full Drop-in UI with token refresh
- ✅ **.NET** - Full Drop-in UI with dynamic environment detection
- ✅ **Java** - Full Drop-in UI with token refresh
- ⚠️ **PHP** - Not updated (kept for backward compatibility examples)

## Customization Options

Each template includes:

1. **Basic SDK Setup**
   - Environment variable configuration
   - Service URL configuration
   - API key management

2. **Starter Endpoints**
   - GET `/config` - Configuration endpoint
   - POST `/process-payment` - Payment processing template
   - Commented examples for additional endpoints (authorize, capture, refund, etc.)

3. **Ready-to-Modify Structure**
   - TODO comments for customization points
   - Example payment logic you can adapt
   - Placeholder functions for various payment flows

## Quick Start

1. **Copy the template** - Copy this directory to start your new project
2. **Choose your language** - Navigate to any implementation directory (nodejs, python, php, java, dotnet, go)
3. **Set up credentials** - Copy `.env.sample` to `.env` and add your Global Payments API keys
4. **Run the server** - Execute `./run.sh` to install dependencies and start the server
5. **Customize** - Modify the code for your specific payment use case

## Use Cases You Can Build

This template can be adapted for various payment scenarios:

- **Basic Charges** - Simple one-time payments
- **Authorization/Capture** - Two-step payment processing
- **Subscriptions** - Recurring payment processing
- **Refunds** - Payment reversal functionality
- **Multi-step Checkouts** - Complex payment flows
- **Payment Methods** - Credit cards, ACH, alternative payments

## Prerequisites

- Global Payments account with API credentials
- Development environment for your chosen language
- Package manager (npm, pip, composer, maven, dotnet, go mod)

## Customization Guide

### Adding New Endpoints

Each implementation includes commented examples for common payment operations:

```javascript
// Authorization only
app.post('/authorize', ...)

// Capture authorized payment  
app.post('/capture', ...)

// Process refund
app.post('/refund', ...)

// Get transaction details
app.get('/transaction/:id', ...)
```

### Modifying Payment Logic

1. Update the `/process-payment` endpoint for your specific flow
2. Add validation for your required fields
3. Customize error handling and responses
4. Add logging and monitoring as needed

### Production Considerations

Enhance the template for production use with:
- Input validation and sanitization
- Comprehensive error handling and logging
- Security headers and rate limiting
- PCI compliance measures
- Monitoring and alerting
