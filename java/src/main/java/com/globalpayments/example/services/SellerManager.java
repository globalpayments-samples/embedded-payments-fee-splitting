package com.globalpayments.example.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalpayments.example.models.Seller;
import com.globalpayments.example.models.SellersData;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manages seller data for marketplace operations.
 *
 * <p>Provides seller lookup and validation using mock data
 * loaded from JSON configuration file. Implements lazy loading
 * with thread-safe double-checked locking pattern.</p>
 *
 * @author Global Payments
 * @version 1.0
 */
public class SellerManager {
    private static SellersData sellersData;
    private static final Object lock = new Object();

    /**
     * Retrieves all registered sellers.
     *
     * @return List of all Seller objects
     */
    public static List<Seller> getAllSellers() {
        loadSellers();
        return sellersData.getSellers();
    }

    /**
     * Retrieves seller details by ID.
     *
     * @param id Seller identifier
     * @return Seller object with details, or null if not found
     */
    public static Seller getSellerById(String id) {
        loadSellers();
        return sellersData.getSellers().stream()
            .filter(s -> s.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Validates if a seller exists in the system.
     *
     * @param id Seller identifier to validate
     * @return true if seller exists, false otherwise
     */
    public static boolean isValidSeller(String id) {
        return getSellerById(id) != null;
    }

    /**
     * Loads seller data from JSON file using lazy initialization.
     * Thread-safe implementation using double-checked locking pattern.
     *
     * @throws RuntimeException if seller data file cannot be loaded
     */
    private static void loadSellers() {
        if (sellersData != null) {
            return;
        }

        synchronized (lock) {
            if (sellersData != null) {
                return;
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                File jsonFile = new File("data/mock-sellers.json");
                sellersData = mapper.readValue(jsonFile, SellersData.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load seller data", e);
            }
        }
    }
}
