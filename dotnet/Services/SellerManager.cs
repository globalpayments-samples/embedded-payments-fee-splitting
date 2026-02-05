namespace EmbeddedPaymentsFee.Services;

using System.Text.Json;
using EmbeddedPaymentsFee.Models;

public class SellerManager
{
    private static SellersData? _sellersData;
    private static readonly object _lock = new();

    public static List<Seller> GetAllSellers()
    {
        LoadSellers();
        return _sellersData?.Sellers ?? new List<Seller>();
    }

    public static Seller? GetSellerById(string id)
    {
        LoadSellers();
        return _sellersData?.Sellers.FirstOrDefault(s => s.Id == id);
    }

    public static bool IsValidSeller(string id)
    {
        return GetSellerById(id) != null;
    }

    private static void LoadSellers()
    {
        if (_sellersData != null) return;

        lock (_lock)
        {
            if (_sellersData != null) return;

            var jsonPath = Path.Combine(Directory.GetCurrentDirectory(), "data", "mock-sellers.json");
            if (!File.Exists(jsonPath))
                throw new FileNotFoundException("Seller data file not found");

            var json = File.ReadAllText(jsonPath);
            var options = new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true
            };
            _sellersData = JsonSerializer.Deserialize<SellersData>(json, options);
        }
    }
}
