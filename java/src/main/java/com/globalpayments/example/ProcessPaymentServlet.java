package com.globalpayments.example;

import com.global.api.ServicesContainer;
import com.global.api.entities.Address;
import com.global.api.entities.Transaction;
import com.global.api.entities.enums.Channel;
import com.global.api.entities.enums.Environment;
import com.global.api.entities.exceptions.ApiException;
import com.global.api.entities.exceptions.ConfigurationException;
import com.global.api.paymentMethods.CreditCardData;
import com.global.api.serviceConfigs.GpApiConfig;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import com.globalpayments.example.models.Seller;
import com.globalpayments.example.models.SplitDetails;
import com.globalpayments.example.services.SellerManager;
import com.globalpayments.example.services.SplitCalculator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Card Payment Processing Servlet
 *
 * This servlet demonstrates card payment processing using the Global Payments SDK.
 * It provides endpoints for configuration and payment processing, handling
 * tokenized card data to ensure secure payment processing.
 *
 * Endpoints:
 * - GET /config: Returns the public API key for client-side tokenization
 * - POST /process-payment: Processes card payments using tokenized data
 * - POST /process-marketplace-payment: Processes marketplace payments with fee splitting
 *
 * @author Global Payments
 * @version 1.0
 */

@MultipartConfig
@WebServlet(urlPatterns = {"/process-payment", "/process-marketplace-payment", "/config", "/get-access-token"})
public class ProcessPaymentServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private final Dotenv dotenv = Dotenv.load();
    
    /**
     * Initializes the servlet and configures the Global Payments SDK.
     * This must be called before processing any payments.
     *
     * @throws ServletException if there's an error initializing the servlet
     */
    @Override
    public void init() throws ServletException {
        try {
            // Configure the Global Payments SDK with credentials and settings
            GpApiConfig config = new GpApiConfig();
            config.setAppId(dotenv.get("GP_APP_ID"));
            config.setAppKey(dotenv.get("GP_APP_KEY"));
            config.setEnvironment("production".equals(dotenv.get("GP_ENVIRONMENT"))
                ? Environment.PRODUCTION
                : Environment.TEST);
            config.setChannel(Channel.CardNotPresent);
            config.setCountry("US");

            ServicesContainer.configureService(config);
        } catch (ConfigurationException e) {
            // Log configuration errors and propagate as ServletException
            throw new ServletException("Failed to configure Global Payments SDK", e);
        }
    }

    /**
     * Generates a random nonce for access token requests
     *
     * @return A hexadecimal string representing the nonce
     */
    private String generateNonce() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    /**
     * Hashes the nonce and app key using SHA-512
     *
     * @param nonce The nonce to hash
     * @param appKey The app key to include in the hash
     * @return A hexadecimal string representing the SHA-512 hash
     * @throws NoSuchAlgorithmException if SHA-512 is not available
     */
    private String hashSecret(String nonce, String appKey) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] hash = digest.digest((nonce + appKey).getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Converts a byte array to a hexadecimal string
     *
     * @param bytes The byte array to convert
     * @return A hexadecimal string representation
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Handles GET requests to /config endpoint.
     * Returns the public API key needed for client-side tokenization.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getServletPath().equals("/config")) {
            response.setContentType("application/json");
            String publicKey = dotenv.get("PUBLIC_API_KEY");
            String jsonResponse = String.format(
                "{\"success\":true,\"data\":{\"publicApiKey\":\"%s\"}}",
                publicKey
            );
            response.getWriter().write(jsonResponse);
        }
    }

    /**
     * Sanitizes postal code input by removing invalid characters.
     * Only allows alphanumeric characters and hyphens, limited to 10 characters.
     *
     * @param postalCode The postal code to sanitize, can be null
     * @return A sanitized postal code containing only alphanumeric characters
     *         and hyphens, limited to 10 characters. Returns empty string if input is null.
     */
    private String sanitizePostalCode(String postalCode) {
        if (postalCode == null) {
            return "";
        }
        String sanitized = postalCode.replaceAll("[^a-zA-Z0-9-]", "");
        return sanitized.length() > 10 ? sanitized.substring(0, 10) : sanitized;
    }

    /**
     * Handles POST requests to /process-payment and /get-access-token endpoints.
     * Processes card payments using tokenized card data or generates access tokens.
     *
     * @param request The HTTP request containing payment details or access token request
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        // Route to marketplace payment handler
        if (request.getServletPath().equals("/process-marketplace-payment")) {
            processMarketplacePayment(request, response);
            return;
        }

        // Handle access token generation
        if (request.getServletPath().equals("/get-access-token")) {
            try {
                String nonce = generateNonce();
                String secret = hashSecret(nonce, dotenv.get("GP_APP_KEY"));

                JSONObject tokenRequest = new JSONObject();
                tokenRequest.put("app_id", dotenv.get("GP_APP_ID"));
                tokenRequest.put("nonce", nonce);
                tokenRequest.put("secret", secret);
                tokenRequest.put("grant_type", "client_credentials");
                tokenRequest.put("seconds_to_expire", 600);
                tokenRequest.put("permissions", new String[]{"PMT_POST_Create_Single"});

                String apiEndpoint = "production".equals(dotenv.get("GP_ENVIRONMENT"))
                    ? "https://apis.globalpay.com/ucp/accesstoken"
                    : "https://apis.sandbox.globalpay.com/ucp/accesstoken";

                URL url = new URL(apiEndpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("X-GP-Version", "2021-03-22");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(tokenRequest.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder responseBody = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseBody.append(line);
                    }
                    br.close();

                    JSONObject responseData = new JSONObject(responseBody.toString());
                    JSONObject successResponse = new JSONObject();
                    successResponse.put("success", true);
                    successResponse.put("token", responseData.getString("token"));

                    response.getWriter().write(successResponse.toString());
                } else {
                    throw new Exception("Failed to generate access token");
                }
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getMessage());
                response.getWriter().write(errorResponse.toString());
            }
            return;
        }

        // Handle payment processing
        try {
            // Validate and extract payment information
            String paymentToken = request.getParameter("payment_token");
            String billingZip = request.getParameter("billing_zip");
            String amountStr = request.getParameter("amount");

            if (paymentToken == null || billingZip == null || amountStr == null ||
                paymentToken.trim().isEmpty() || billingZip.trim().isEmpty() || amountStr.trim().isEmpty()) {
                throw new ApiException("Missing required fields");
            }

            // Validate and parse amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ApiException("Amount must be a positive number");
                }
            } catch (NumberFormatException e) {
                throw new ApiException("Invalid amount format");
            }

            // Initialize payment data using tokenized card information
            CreditCardData card = new CreditCardData();
            card.setToken(paymentToken);

            // Create billing address for AVS verification
            Address address = new Address();
            address.setPostalCode(sanitizePostalCode(billingZip));

            // Process the payment transaction using the provided amount
            Transaction transaction = card.charge(amount)
                    .withAllowDuplicates(true)
                    .withCurrency("USD")
                    .withAddress(address)
                    .execute();

            // Verify transaction was successful
            if (!"00".equals(transaction.getResponseCode()) && !"SUCCESS".equals(transaction.getResponseCode())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String errorResponse = String.format(
                    "{\"success\":false,\"message\":\"Payment processing failed\",\"error\":{\"code\":\"PAYMENT_DECLINED\",\"details\":\"%s\"}}",
                    transaction.getResponseMessage()
                );
                response.getWriter().write(errorResponse);
                return;
            }

            // Return success response with transaction ID
            String successResponse = String.format(
                "{\"success\":true,\"message\":\"Payment successful! Transaction ID: %s\",\"data\":{\"transactionId\":\"%s\"}}", 
                transaction.getTransactionId(),
                transaction.getTransactionId()
            );
            response.getWriter().write(successResponse);

        } catch (ApiException e) {
            // Handle payment processing errors
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String errorResponse = String.format(
                "{\"success\":false,\"message\":\"Payment processing failed\",\"error\":{\"code\":\"API_ERROR\",\"details\":\"%s\"}}", 
                e.getMessage()
            );
            response.getWriter().write(errorResponse);
        }
    }

    /**
     * Processes marketplace payments with fee splitting.
     *
     * @param request The HTTP request containing payment and seller details
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    private void processMarketplacePayment(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        try {
            // Extract parameters
            String token = request.getParameter("payment_token");
            String zip = request.getParameter("billing_zip");
            String amountStr = request.getParameter("amount");
            String sellerId = request.getParameter("seller_id");
            String feeRateStr = request.getParameter("platform_fee_rate");

            // Enhanced validation - check for both null AND empty strings
            boolean hasToken = token != null && !token.trim().isEmpty();
            boolean hasZip = zip != null && !zip.trim().isEmpty();
            boolean hasAmount = amountStr != null && !amountStr.trim().isEmpty();
            boolean hasSellerId = sellerId != null && !sellerId.trim().isEmpty();

            if (!hasToken || !hasZip || !hasAmount || !hasSellerId) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Missing required fields\"}");
                return;
            }

            BigDecimal amount = new BigDecimal(amountStr);

            // Validate seller
            if (!SellerManager.isValidSeller(sellerId)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid seller selected\"}");
                return;
            }

            Seller seller = SellerManager.getSellerById(sellerId);
            double platformFeeRate = feeRateStr != null ? Double.parseDouble(feeRateStr) : 10.0;

            // Calculate split
            SplitCalculator calculator = new SplitCalculator(platformFeeRate);
            SplitDetails splitDetails = calculator.calculateSplit(amount.doubleValue());
            splitDetails.setSellerId(sellerId);
            splitDetails.setSellerName(seller.getName());

            // Process payment
            CreditCardData card = new CreditCardData();
            card.setToken(token);

            Address address = new Address();
            address.setPostalCode(sanitizePostalCode(zip));

            Transaction transaction = card.charge(amount)
                    .withAllowDuplicates(true)
                    .withCurrency("USD")
                    .withAddress(address)
                    .execute();

            if (!"00".equals(transaction.getResponseCode()) && !"SUCCESS".equals(transaction.getResponseCode())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String errorResponse = String.format(
                    "{\"success\":false,\"message\":\"Payment processing failed\",\"error\":{\"code\":\"PAYMENT_DECLINED\",\"details\":\"%s\"}}",
                    transaction.getResponseMessage()
                );
                response.getWriter().write(errorResponse);
                return;
            }

            // Return success response with split details
            String successResponse = String.format(
                "{\"success\":true,\"message\":\"Payment successful! Transaction ID: %s\",\"data\":{\"transactionId\":\"%s\",\"amount\":%s,\"splitDetails\":{\"amount\":%.2f,\"processingFee\":%.2f,\"processingFeeRate\":%.2f,\"processingFeeFixed\":%.2f,\"platformFee\":%.2f,\"platformFeeRate\":%.2f,\"sellerPayout\":%.2f,\"sellerId\":\"%s\",\"sellerName\":\"%s\"}}}",
                transaction.getTransactionId(),
                transaction.getTransactionId(),
                amountStr,
                splitDetails.getAmount(),
                splitDetails.getProcessingFee(),
                splitDetails.getProcessingFeeRate(),
                splitDetails.getProcessingFeeFixed(),
                splitDetails.getPlatformFee(),
                splitDetails.getPlatformFeeRate(),
                splitDetails.getSellerPayout(),
                splitDetails.getSellerId(),
                splitDetails.getSellerName()
            );
            response.getWriter().write(successResponse);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String errorResponse = String.format(
                "{\"success\":false,\"message\":\"Internal server error\",\"error\":{\"code\":\"SERVER_ERROR\",\"details\":\"%s\"}}",
                e.getMessage()
            );
            response.getWriter().write(errorResponse);
        }
    }
}
