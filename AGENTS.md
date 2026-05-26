# Embedded Payments — Fee Splitting

> Process card-not-present GP-API payments with server-side fee splitting across PHP, Node.js, .NET, and Java.

## Critical Patterns

1. **Use GP-API app credentials, not the root Docker keys.** All four backends configure `GpApiConfig`/the GP-API SDK with `GP_APP_ID` and `GP_APP_KEY` (`php/process-embedded-payments-payment.php` via `configureSdk()`, `nodejs/server.js`, `dotnet/Program.cs` via `ConfigureGlobalPaymentsSDK()`, `java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java` via `init()`). The root [`docker-compose.yml`](docker-compose.yml) still references `PUBLIC_API_KEY`/`SECRET_API_KEY`, `/config` health checks, and missing `python/` and `go/` services, so treat it as stale until it is repaired.
2. **`platform_fee_rate` is request-driven; `PLATFORM_FEE_RATE` is not wired up.** Every backend falls back to `10.0` in code when the request omits `platform_fee_rate`, but none of the runtime handlers read `PLATFORM_FEE_RATE` from the environment. If you change the default split, update all four handlers and their docs together.
3. **Seller IDs live in two places.** Each frontend hardcodes the seller dropdown in `loadSellers()` (`php/index.html`, `nodejs/index.html`, `dotnet/wwwroot/index.html`, `java/src/main/webapp/index.html`), while each backend validates against `data/mock-sellers.json` through `SellerManager`. If you add or rename a seller in only one place, checkout succeeds in the UI but the POST fails with `Invalid seller selected`.
4. **Duplicate-submit behavior is inconsistent today.** Node.js, .NET, and Java call `.withAllowDuplicates(true)` before `execute()`, but PHP does not. If you are comparing implementations or trying to make repeated identical test charges succeed everywhere, align the PHP handler with the others first.

## Repository Structure

### PHP (built-in server)
- [`php/process-embedded-payments-payment.php`](php/process-embedded-payments-payment.php) — SDK setup in `configureSdk()`; top-level POST handler parses `$_POST`, validates input, computes the split, and charges the card.
- [`php/router.php`](php/router.php) — built-in router for `/` and `/process-embedded-payments-payment`.
- [`php/lib/SellerManager.php`](php/lib/SellerManager.php) — `loadSellers()`, `getAllSellers()`, `getSellerById()`, `isValidSeller()`.
- [`php/lib/SplitCalculator.php`](php/lib/SplitCalculator.php) — constructor enforces the 5–25% range; `calculateSplit()` returns the fee breakdown.
- [`php/lib/Utils.php`](php/lib/Utils.php) — `sanitizePostalCode()` for AVS-safe ZIP/postal input.
- [`php/data/mock-sellers.json`](php/data/mock-sellers.json) — mock seller registry; there is no database.
- [`php/index.html`](php/index.html) — `loadSellers()`, `initializeEmbeddedPaymentsForm()`, `displaySplitResults()`, `resetPaymentForm()`.

### Node.js (Express)
- [`nodejs/server.js`](nodejs/server.js) — GP-API setup, `sanitizePostalCode()`, and the anonymous `app.post('/process-embedded-payments-payment', ...)` handler at line ~61.
- [`nodejs/lib/SellerManager.js`](nodejs/lib/SellerManager.js) — `loadSellers()`, `getAllSellers()`, `getSellerById()`, `isValidSeller()`.
- [`nodejs/lib/SplitCalculator.js`](nodejs/lib/SplitCalculator.js) — constructor validates the fee range; `calculateSplit()` returns the split object used in the response.
- [`nodejs/data/mock-sellers.json`](nodejs/data/mock-sellers.json) — mock seller registry.
- [`nodejs/index.html`](nodejs/index.html) — `loadSellers()`, `initializeEmbeddedPaymentsForm()`, `displaySplitResults()`, `resetPaymentForm()`.

### .NET (ASP.NET Core minimal API)
- [`dotnet/Program.cs`](dotnet/Program.cs) — startup and SDK wiring in `ConfigureGlobalPaymentsSDK()`; route registration in `ConfigureEmbeddedPaymentsEndpoint()`; anonymous `app.MapPost(...)` handler at line ~115; `SanitizePostalCode()` for AVS input cleanup.
- [`dotnet/Services/SellerManager.cs`](dotnet/Services/SellerManager.cs) — `LoadSellers()`, `GetAllSellers()`, `GetSellerById()`, `IsValidSeller()`.
- [`dotnet/Services/SplitCalculator.cs`](dotnet/Services/SplitCalculator.cs) — constructor range validation plus `CalculateSplit()`.
- [`dotnet/Models/`](dotnet/Models) — response and seller models.
- [`dotnet/data/mock-sellers.json`](dotnet/data/mock-sellers.json) — mock seller registry.
- [`dotnet/wwwroot/index.html`](dotnet/wwwroot/index.html) — `loadSellers()`, `initializeEmbeddedPaymentsForm()`, `displaySplitResults()`, `resetPaymentForm()`.

### Java (Jakarta Servlet + embedded Tomcat via Cargo)
- [`java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java`](java/src/main/java/com/globalpayments/example/ProcessPaymentServlet.java) — SDK wiring in `init()`, postal cleanup in `sanitizePostalCode()`, request handling in `doPost()`.
- [`java/src/main/java/com/globalpayments/example/services/SellerManager.java`](java/src/main/java/com/globalpayments/example/services/SellerManager.java) — `loadSellers()`, `getAllSellers()`, `getSellerById()`, `isValidSeller()`.
- [`java/src/main/java/com/globalpayments/example/services/SplitCalculator.java`](java/src/main/java/com/globalpayments/example/services/SplitCalculator.java) — constructor range validation plus `calculateSplit()`.
- [`java/src/main/java/com/globalpayments/example/models/`](java/src/main/java/com/globalpayments/example/models) — seller and split DTOs.
- [`java/data/mock-sellers.json`](java/data/mock-sellers.json) — mock seller registry.
- [`java/src/main/webapp/index.html`](java/src/main/webapp/index.html) — `loadSellers()`, `initializeEmbeddedPaymentsForm()`, `displaySplitResults()`, `resetPaymentForm()`.

### Shared
- [`README.md`](README.md) — high-level overview, but several details are out of sync with code.
- [`docker-compose.yml`](docker-compose.yml) — stale multi-service file referencing missing `python/` and `go/` implementations plus the wrong env var names for this repo.
- [`docker-run.sh`](docker-run.sh) — wrapper around the same stale compose setup.
- [`index.html`](index.html) — root copy of the same demo form; this is the page Node serves if you run the root `package.json` script instead of `cd nodejs && ./run.sh`.
- [`package.json`](package.json) — root convenience script that launches `node nodejs/server.js` from the repo root.

## API Surface

All implementations expose the same payment path, but request parsing is not perfectly identical: the browser UIs submit `multipart/form-data`; PHP, .NET, and Java are written around form fields, while Node.js also accepts JSON because `express.json()` is enabled before the route.

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/process-embedded-payments-payment` | Validate seller and amount, compute the split, charge the card through GP-API, and return `transactionId` plus `splitDetails`. |

## Environment Variables

```bash
GP_APP_ID=...              # required in all languages; GP-API app ID from the developer portal
GP_APP_KEY=...             # required in all languages; GP-API app key from the developer portal
GP_API_ENVIRONMENT=TEST    # optional; any value other than PRODUCTION resolves to the test environment
PORT=8000                  # local server port; Node.js and .NET read it directly, and PHP's run.sh also honors it
PLATFORM_FEE_RATE=10.0     # present in every .env.sample, but currently unused by the request handlers
```

All four `.env.sample` files currently match. Backend code reads `GP_APP_ID`, `GP_APP_KEY`, and `GP_API_ENVIRONMENT` everywhere; Node.js and .NET also read `PORT` directly, and PHP's `run.sh` uses `PORT` when starting `php -S`.

## Sandbox Credentials

| Item | Value | Notes |
|------|-------|-------|
| Gateway | GP-API | Confirmed by `GpApiConfig` usage in all four backends and `GP_APP_ID`/`GP_APP_KEY` env vars. |
| Successful Visa test card | `4263970000005262` | Shown in every frontend as the demo card and listed by Global Payments test-card docs for sandbox use. |
| Successful Mastercard test card | `5425230000004415` | GP sandbox test card. |
| Successful Amex test card | `374101000000608` | GP sandbox test card. |
| Credentials source | `https://developer.globalpayments.com/` | Create a developer app there to obtain `GP_APP_ID` and `GP_APP_KEY`. |

## Architecture Summary

**Browser payment flow:** `index.html` form → `FormData` POST to `/process-embedded-payments-payment` → backend validates seller and amount → backend charges card with GP-API SDK → UI renders `splitDetails`.

**Seller lookup flow:** hardcoded seller dropdown in each frontend → backend `SellerManager` loads `data/mock-sellers.json` → request fails if the frontend IDs drift from the JSON file.

**Fee calculation flow:** request `amount` + optional `platform_fee_rate` → `SplitCalculator` computes processing fee, platform fee, and seller payout → backend returns the breakdown alongside the transaction ID.

## Security Notes

This is a demo, not a production-ready payment service. Card PAN, expiry, and CVV are posted directly to the backend, so PCI scope is larger than with tokenization or hosted fields. There is no auth layer, seller storage is flat JSON, and the PHP implementation currently logs credential metadata on every request plus the card BIN on errors in [`php/process-embedded-payments-payment.php`](php/process-embedded-payments-payment.php).

## How to Run

```bash
cd php && ./run.sh       # PHP — :8000 via php -S 0.0.0.0:${PORT:-8000}
cd nodejs && ./run.sh    # Node.js — :8000 via process.env.PORT || 8000
cd dotnet && ./run.sh    # .NET — :8000 via Environment.GetEnvironmentVariable("PORT") ?? "8000"
cd java && ./run.sh      # Java — :8000 via cargo.servlet.port in java/pom.xml
```

Avoid using the root `docker-compose.yml` as the canonical run path until it is fixed for this repo.

## How to Verify

```bash
# Landing page
curl -I http://localhost:8000/
# Expected: HTTP 200 from the static demo page

# Successful payment (works against every language because the UI also uses multipart form data)
curl -X POST http://localhost:8000/process-embedded-payments-payment \
  -F card_name='Jane Doe' \
  -F card_number='4263970000005262' \
  -F card_expiry='12/29' \
  -F card_cvv='123' \
  -F billing_zip='12345' \
  -F amount='100.00' \
  -F seller_id='seller_001' \
  -F platform_fee_rate='10'
# Expected: {"success":true,"data":{"transactionId":"...","splitDetails":{...}}}

# Seller validation failure
curl -X POST http://localhost:8000/process-embedded-payments-payment \
  -F card_name='Jane Doe' \
  -F card_number='4263970000005262' \
  -F card_expiry='12/29' \
  -F card_cvv='123' \
  -F billing_zip='12345' \
  -F amount='100.00' \
  -F seller_id='seller_999'
# Expected: HTTP 400 with "Invalid seller selected"
```

## Making Changes

All four language folders implement the same checkout flow and should stay behaviorally aligned. If you change the request fields, seller IDs, split math, or response schema, apply the change to `php/`, `nodejs/`, `dotnet/`, and `java/` together.

Do not modify only one copy of the frontend: this repo has a root [`index.html`](index.html) plus per-language copies in [`php/index.html`](php/index.html), [`nodejs/index.html`](nodejs/index.html), [`dotnet/wwwroot/index.html`](dotnet/wwwroot/index.html), and [`java/src/main/webapp/index.html`](java/src/main/webapp/index.html).

Do not change seller IDs in only one place: each language has its own `data/mock-sellers.json`, and each frontend hardcodes the same seller list in `loadSellers()`.

Do not touch [`docker-compose.yml`](docker-compose.yml), [`docker-run.sh`](docker-run.sh), or the root [`package.json`](package.json) in isolation without checking whether the change is meant for the shared repo wrapper or a specific language implementation. Also note that only PHP, Node.js, .NET, and Java are present today; Python and Go are referenced by compose scripts but are not in the tree.

## SDK Versions

- PHP: `globalpayments/php-sdk` `^13.1`
- Node.js: `globalpayments-api` `^3.10.6`
- .NET: `GlobalPayments.Api` `9.0.16`
- Java: `com.heartlandpaymentsystems:globalpayments-sdk` `14.2.20`
