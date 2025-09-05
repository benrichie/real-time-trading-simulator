package rtp.example.rtp.Stock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rtp.example.rtp.RealTimeStockDataService;
import rtp.example.rtp.StockPrice;
import rtp.example.rtp.StockDataException;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;
    private final RealTimeStockDataService realTimeStockDataService;

    public StockController(StockService stockService, RealTimeStockDataService realTimeStockDataService){
        this.stockService = stockService;
        this.realTimeStockDataService = realTimeStockDataService;
    }

    @GetMapping
    public ResponseEntity<?> getAllStocks(){
        return ResponseEntity.ok(stockService.getAllStocks());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getStock(@PathVariable String symbol){
        try {
            return ResponseEntity.ok(stockService.getStock(symbol.toUpperCase()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stock not found: " + symbol);
        }
    }

    // POST now takes symbol/companyName only and populates currentPrice from external API (plan steps 1-4)
    @PostMapping
    public ResponseEntity<?> createStock(@RequestBody CreateStockRequest req) {
        // Step 2: validate & normalize
        if (req == null || req.getSymbol() == null || req.getSymbol().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Symbol is required");
        }
        String normalized = req.getSymbol().trim().toUpperCase();
        String companyName = (req.getCompanyName() == null || req.getCompanyName().trim().isEmpty())
                ? normalized
                : req.getCompanyName().trim();

        //check if stock already exists
        if (stockService.existsBySymbol(normalized)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Stock already exists: " + normalized);
        }

        try {
            // Step 3: fetch current price from the external API via RealTimeStockDataService
            StockPrice price = realTimeStockDataService.getCurrentStockPrice(normalized);

            // Step 4: construct Stock entity with fetched price and persist
            Stock stock = new Stock(normalized, companyName, price.getPrice());
            Stock saved = stockService.saveStock(stock);

            // Optional: start tracking symbol for ongoing updates
            realTimeStockDataService.trackSymbol(normalized);

            // Step 5: return 201 Created with the saved stock
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (StockDataException e) {
            // External API unavailable or no data for symbol -> 503 Service Unavailable
            String msg = "Unable to fetch price for " + normalized + ": " + e.getMessage();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(msg);
        } catch (Exception e) {
            // Any other error (e.g., DB constraint) -> 400 Bad Request with message
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to create stock " + normalized + ": " + e.getMessage());
        }
    }

    @PutMapping("/{symbol}")
    public ResponseEntity<?> updateStock(@PathVariable String symbol, @RequestBody Stock stock){
        stock.setSymbol(symbol.toUpperCase());
        try {
            return ResponseEntity.ok(stockService.saveStock(stock));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stock not found: " + symbol);
        }
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<?> deleteStock(@PathVariable String symbol){
        try {
            stockService.deleteStock(symbol.toUpperCase());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stock not found: " + symbol);
        }
    }

    // Existing real-time price endpoint returns a lightweight DTO
    @GetMapping("/{symbol}/price")
    public ResponseEntity<?> getCurrentPrice(@PathVariable String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Symbol is required");
        }
        String normalized = symbol.trim().toUpperCase();

        try {
            StockPrice price = realTimeStockDataService.getCurrentStockPrice(normalized);
            PriceResponse resp = new PriceResponse(
                    price.getSymbol(),
                    price.getPrice(),
                    price.getChangeAmount(),
                    price.getChangePercent(),
                    price.getTimestamp(),
                    price.getSource()
            );
            return ResponseEntity.ok(resp);
        } catch (StockDataException e) {
            String msg = "Price data not available for " + normalized + ": " + e.getMessage();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(msg);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error fetching price for " + normalized);
        }
    }

    // DTO for create request (step 1)
    public static class CreateStockRequest {
        private String symbol;
        private String companyName;

        public CreateStockRequest() {}

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
    }

    // Lightweight DTO to return price info
    public static class PriceResponse {
        private final String symbol;
        private final java.math.BigDecimal price;
        private final java.math.BigDecimal changeAmount;
        private final java.math.BigDecimal changePercent;
        private final java.time.LocalDateTime timestamp;
        private final String source;

        public PriceResponse(String symbol, java.math.BigDecimal price,
                             java.math.BigDecimal changeAmount, java.math.BigDecimal changePercent,
                             java.time.LocalDateTime timestamp, String source) {
            this.symbol = symbol;
            this.price = price;
            this.changeAmount = changeAmount;
            this.changePercent = changePercent;
            this.timestamp = timestamp;
            this.source = source;
        }

        public String getSymbol() { return symbol; }
        public java.math.BigDecimal getPrice() { return price; }
        public java.math.BigDecimal getChangeAmount() { return changeAmount; }
        public java.math.BigDecimal getChangePercent() { return changePercent; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public String getSource() { return source; }
    }
}