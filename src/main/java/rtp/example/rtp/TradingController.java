package rtp.example.rtp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rtp.example.rtp.Order.OrderType;
import rtp.example.rtp.Order.PriceType;

@RestController
@RequestMapping("/api/v1/trading")
public class TradingController {

    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/buy")
    public ResponseEntity<TradingService.TradingResult> buyStock(@Valid @RequestBody BuyStockRequest request) {
        TradingService.TradingResult result = tradingService.buyStock(
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getQuantity(),
                request.getPriceType(),
                request.getLimitPrice()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sell")
    public ResponseEntity<TradingService.TradingResult> sellStock(@Valid @RequestBody SellStockRequest request) {
        TradingService.TradingResult result = tradingService.sellStock(
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getQuantity(),
                request.getPriceType(),
                request.getLimitPrice()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sell-all")
    public ResponseEntity<TradingService.TradingResult> sellAllShares(@Valid @RequestBody SellAllRequest request) {
        TradingService.TradingResult result = tradingService.sellAllShares(
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getPriceType(),
                request.getLimitPrice()
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/quote")
    public ResponseEntity<TradingService.TradingQuote> getQuote(
            @RequestParam @NotBlank String stockSymbol,
            @RequestParam @Positive Integer quantity,
            @RequestParam @NotNull OrderType orderType) {

        TradingService.TradingQuote quote = tradingService.getQuote(stockSymbol, quantity, orderType);
        return ResponseEntity.ok(quote);
    }

    @GetMapping("/can-afford")
    public ResponseEntity<AffordabilityResponse> canAffordOrder(
            @RequestParam @NotNull Long portfolioId,
            @RequestParam @NotBlank String stockSymbol,
            @RequestParam @Positive Integer quantity,
            @RequestParam @NotNull OrderType orderType) {

        boolean canAfford = tradingService.canAffordOrder(portfolioId, stockSymbol, quantity, orderType);
        return ResponseEntity.ok(new AffordabilityResponse(canAfford,
                canAfford ? "Order can be afforded" : "Insufficient funds or shares"));
    }

    // Request DTOs with validation annotations
    public static class BuyStockRequest {
        @NotNull(message = "Portfolio ID is required")
        private Long portfolioId;

        @NotBlank(message = "Stock symbol is required")
        private String stockSymbol;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;

        @NotNull(message = "Price type is required")
        private PriceType priceType;

        private java.math.BigDecimal limitPrice;

        // Getters and setters
        public Long getPortfolioId() { return portfolioId; }
        public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
        public String getStockSymbol() { return stockSymbol; }
        public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public PriceType getPriceType() { return priceType; }
        public void setPriceType(PriceType priceType) { this.priceType = priceType; }
        public java.math.BigDecimal getLimitPrice() { return limitPrice; }
        public void setLimitPrice(java.math.BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    }

    public static class SellStockRequest {
        @NotNull(message = "Portfolio ID is required")
        private Long portfolioId;

        @NotBlank(message = "Stock symbol is required")
        private String stockSymbol;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;

        @NotNull(message = "Price type is required")
        private PriceType priceType;

        private java.math.BigDecimal limitPrice;

        // Getters and setters
        public Long getPortfolioId() { return portfolioId; }
        public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
        public String getStockSymbol() { return stockSymbol; }
        public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public PriceType getPriceType() { return priceType; }
        public void setPriceType(PriceType priceType) { this.priceType = priceType; }
        public java.math.BigDecimal getLimitPrice() { return limitPrice; }
        public void setLimitPrice(java.math.BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    }

    public static class SellAllRequest {
        @NotNull(message = "Portfolio ID is required")
        private Long portfolioId;

        @NotBlank(message = "Stock symbol is required")
        private String stockSymbol;

        @NotNull(message = "Price type is required")
        private PriceType priceType;

        private java.math.BigDecimal limitPrice;

        // Getters and setters
        public Long getPortfolioId() { return portfolioId; }
        public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
        public String getStockSymbol() { return stockSymbol; }
        public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
        public PriceType getPriceType() { return priceType; }
        public void setPriceType(PriceType priceType) { this.priceType = priceType; }
        public java.math.BigDecimal getLimitPrice() { return limitPrice; }
        public void setLimitPrice(java.math.BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    }

    public static class AffordabilityResponse {
        private final boolean canAfford;
        private final String message;

        public AffordabilityResponse(boolean canAfford, String message) {
            this.canAfford = canAfford;
            this.message = message;
        }

        public boolean isCanAfford() { return canAfford; }
        public String getMessage() { return message; }
    }
}
