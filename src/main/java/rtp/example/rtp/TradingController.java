package rtp.example.rtp;

import org.apache.coyote.Response;
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
    public ResponseEntity<TradingService.TradingResult> buyStock(@RequestBody BuyStockRequest request) {
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
    public ResponseEntity<TradingService.TradingResult> sellStock(@RequestBody SellStockRequest request) {
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
    public ResponseEntity<TradingService.TradingResult> sellAllShares(@RequestBody SellAllRequest request) {
        TradingService.TradingResult result = tradingService.sellAllShares(
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getPriceType(),
                request.getLimitPrice()
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/quote")
    public ResponseEntity<TradingService.TradingQuote> getQuote(@RequestParam String stockSymbol,
                                                                @RequestParam Integer quantity,
                                                                @RequestParam OrderType orderType) {
        try {
            TradingService.TradingQuote quote = tradingService.getQuote(stockSymbol, quantity, orderType);
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/can-afford")
    public ResponseEntity<AffordabilityResponse> canAffordOrder(@RequestParam Long portfolioId,
                                                                @RequestParam String stockSymbol,
                                                                @RequestParam Integer quantity,
                                                                @RequestParam OrderType orderType) {
        boolean canAfford = tradingService.canAffordOrder(portfolioId, stockSymbol, quantity, orderType);
        return ResponseEntity.ok(new AffordabilityResponse(canAfford,
                canAfford ? "Order can be afforded" : "Insufficient funds or shares"));
    }

    // Request DTOs
    public static class BuyStockRequest {
        private Long portfolioId;
        private String stockSymbol;
        private Integer quantity;
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
        private Long portfolioId;
        private String stockSymbol;
        private Integer quantity;
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
        private Long portfolioId;
        private String stockSymbol;
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