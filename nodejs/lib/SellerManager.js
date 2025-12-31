import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/**
 * SellerManager - Manages seller data for marketplace
 *
 * Provides seller lookup and validation functionality using
 * mock seller data from JSON file.
 *
 * @class
 */
class SellerManager {
    static sellersData = null;

    /**
     * Loads seller data from JSON file
     *
     * @private
     * @static
     */
    static loadSellers() {
        if (this.sellersData) {
            return;
        }

        const jsonPath = path.join(__dirname, '..', 'data', 'mock-sellers.json');
        if (!fs.existsSync(jsonPath)) {
            throw new Error('Seller data file not found');
        }

        const json = fs.readFileSync(jsonPath, 'utf8');
        this.sellersData = JSON.parse(json);
    }

    /**
     * Gets all sellers
     *
     * @static
     * @returns {Array<Object>} Array of all seller objects
     */
    static getAllSellers() {
        this.loadSellers();
        return this.sellersData.sellers;
    }

    /**
     * Gets seller details by ID
     *
     * @static
     * @param {string} id - Seller identifier
     * @returns {Object|null} Seller object or null if not found
     */
    static getSellerById(id) {
        this.loadSellers();
        return this.sellersData.sellers.find(s => s.id === id) || null;
    }

    /**
     * Validates if a seller ID exists
     *
     * @static
     * @param {string} id - Seller identifier to validate
     * @returns {boolean} True if seller exists
     */
    static isValidSeller(id) {
        return this.getSellerById(id) !== null;
    }
}

export default SellerManager;
