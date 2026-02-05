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
import com.globalpayments.example.models.Seller;
import com.globalpayments.example.models.SplitDetails;
import com.globalpayments.example.services.SellerManager;
import com.globalpayments.example.services.SplitCalculator;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Embedded Payments Processing Servlet
 *
 * This servlet demonstrates embedded payments processing with fee splitting
 * using the Global Payments SDK. It handles card data from the frontend,
 * validates seller information, and processes payments with automatic fee split calculation.
 *
 * @author Global Payments
 * @version 1.0
 */

@MultipartConfig
@WebServlet(urlPatterns = {"/process-embedded-payments-payment"})
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
            GpApiConfig config = new GpApiConfig();
            config.setAppId(dotenv.get("GP_APP_ID"));
            config.setAppKey(dotenv.get("GP_APP_KEY"));
            config.setEnvironment("PRODUCTION".equals(dotenv.get("GP_API_ENVIRONMENT"))
                ? Environment.PRODUCTION
                : Environment.TEST);
            config.setChannel(Channel.CardNotPresent);
            config.setCountry("US");

            ServicesContainer.configureService(config);
        } catch (ConfigurationException e) {
            throw new ServletException("Failed to configure Global Payments SDK", e);
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
     * Handles POST requests to /process-embedded-payments-payment endpoint.
     * Processes embedded payments with fee splitting using card data.
     *
     * @param request The HTTP request containing payment details
     * @param response The HTTP response
     * @throws ServletException If there's an error in servlet processing
     * @throws IOException If there's an I/O error
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        try {
            // Extract parameters
            String cardName = request.getParameter("card_name");
            String cardNumber = request.getParameter("card_number");
            String cardExpiry = request.getParameter("card_expiry");
            String cardCvv = request.getParameter("card_cvv");
            String zip = request.getParameter("billing_zip");
            String amountStr = request.getParameter("amount");
            String sellerId = request.getParameter("seller_id");
            String feeRateStr = request.getParameter("platform_fee_rate");

            // Validate required fields
            boolean hasCardName = cardName != null && !cardName.trim().isEmpty();
            boolean hasCardNumber = cardNumber != null && !cardNumber.trim().isEmpty();
            boolean hasCardExpiry = cardExpiry != null && !cardExpiry.trim().isEmpty();
            boolean hasCardCvv = cardCvv != null && !cardCvv.trim().isEmpty();
            boolean hasZip = zip != null && !zip.trim().isEmpty();
            boolean hasAmount = amountStr != null && !amountStr.trim().isEmpty();
            boolean hasSellerId = sellerId != null && !sellerId.trim().isEmpty();

            if (!hasCardName || !hasCardNumber || !hasCardExpiry || !hasCardCvv ||
                !hasZip || !hasAmount || !hasSellerId) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Missing required fields\"}");
                return;
            }

            // Validate and parse amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr);
                if (amount.compareTo(new BigDecimal("0.50")) < 0) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("{\"success\":false,\"message\":\"Amount must be at least $0.50\"}");
                    return;
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid amount format\"}");
                return;
            }

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

            // Parse expiry date (MM/YY format)
            String[] expiryParts = cardExpiry.split("/");
            if (expiryParts.length != 2) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"success\":false,\"message\":\"Invalid expiry date format. Use MM/YY\"}");
                return;
            }

            Integer expiryMonth = Integer.parseInt(expiryParts[0]);
            Integer expiryYear = Integer.parseInt("20" + expiryParts[1]);

            // Initialize payment data with card details
            CreditCardData card = new CreditCardData();
            card.setCardHolderName(cardName);
            card.setNumber(cardNumber.replace(" ", ""));
            card.setExpMonth(expiryMonth);
            card.setExpYear(expiryYear);
            card.setCvn(cardCvv);

            Address address = new Address();
            address.setPostalCode(sanitizePostalCode(zip));

            // Process payment
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
                "{\"success\":true,\"message\":\"Payment successful! Transaction ID: %s\",\"data\":{\"transactionId\":\"%s\",\"amount\":%s,\"currency\":\"USD\",\"splitDetails\":{\"amount\":%.2f,\"processingFee\":%.2f,\"processingFeeRate\":%.2f,\"processingFeeFixed\":%.2f,\"platformFee\":%.2f,\"platformFeeRate\":%.2f,\"sellerPayout\":%.2f,\"sellerId\":\"%s\",\"sellerName\":\"%s\"}}}",
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

        } catch (ApiException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            String errorResponse = String.format(
                "{\"success\":false,\"message\":\"Payment processing failed\",\"error\":{\"code\":\"API_ERROR\",\"details\":\"%s\"}}",
                e.getMessage()
            );
            response.getWriter().write(errorResponse);
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
