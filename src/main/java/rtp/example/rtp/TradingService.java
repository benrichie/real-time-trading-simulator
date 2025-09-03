package rtp.example.rtp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rtp.example.rtp.Order.*;
import rtp.example.rtp.Portfolio.Portfolio;
import rtp.example.rtp.PortfolioCalculationService;
import rtp.example.rtp.Portfolio.PortfolioService;
import rtp.example.rtp.Positions.Position;
import rtp.example.rtp.Positions.PositionService;
import rtp.example.rtp.Stock.Stock;
import rtp.example.rtp.Stock.StockService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TradingService {

    private final OrderService orderService;
    private final OrderExecutionService orderExecutionService;
    private final PortfolioService portfolioService;
    private final PortfolioCalculationService portfolioCalculationService;
    private final PositionService positionService;
    private final StockService stockService;
    private final RealTimeStockDataService realTimeStockDataService;

    public TradingService(OrderService orderService,
                          OrderExecutionService orderExecutionService,
                          PortfolioService portfolioService,
                          PortfolioCalculationService portfolioCalculationService,
                          PositionService positionService,
                          StockService stockService, RealTimeStockDataService realTimeStockDataService) {
        this.orderService = orderService;
        this.orderExecutionService = orderExecutionService;
        this.portfolioService = portfolioService;
        this.portfolioCalculationService = portfolioCalculationService;
        this.positionService = positionService;
        this.stockService = stockService;
        this.realTimeStockDataService = realTimeStockDataService;
    }

    @Transactional
    public TradingResult buyStock(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        try {
            // Validate inputs
            ValidationResult validation = validateBuyOrder(portfolioId, stockSymbol, quantity, priceType, limitPrice);
            if (!validation.isValid()) {
                return new TradingResult(false, validation.getMessage(), null);
            }

            // Create buy order
            Order order = new Order(portfolioId, stockSymbol, OrderType.BUY, priceType, quantity, limitPrice);
            Order createdOrder = orderService.createOrder(order);

            // Execute order immediately (for market orders) or leave pending (for limit orders)
            if (priceType == PriceType.MARKET) {
                OrderExecutionService.OrderExecutionResult executionResult = orderExecutionService.executeOrder(createdOrder.getId());
                if (executionResult.isSuccess()) {
                    return new TradingResult(true, "Buy order executed successfully", createdOrder);
                } else {
                    return new TradingResult(false, "Failed to execute buy order: " + executionResult.getMessage(), createdOrder);
                }
            } else {
                return new TradingResult(true, "Limit buy order created and pending", createdOrder);
            }

        } catch (Exception e) {
            return new TradingResult(false, "Error processing buy order: " + e.getMessage(), null);
        }
    }

    @Transactional
    public TradingResult sellStock(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        try {
            // Validate inputs
            ValidationResult validation = validateSellOrder(portfolioId, stockSymbol, quantity, priceType, limitPrice);
            if (!validation.isValid()) {
                return new TradingResult(false, validation.getMessage(), null);
            }

            // Create sell order
            Order order = new Order(portfolioId, stockSymbol, OrderType.SELL, priceType, quantity, limitPrice);
            Order createdOrder = orderService.createOrder(order);

            // Execute order immediately (for market orders) or leave pending (for limit orders)
            if (priceType == PriceType.MARKET) {
                OrderExecutionService.OrderExecutionResult executionResult = orderExecutionService.executeOrder(createdOrder.getId());
                if (executionResult.isSuccess()) {
                    return new TradingResult(true, "Sell order executed successfully", createdOrder);
                } else {
                    return new TradingResult(false, "Failed to execute sell order: " + executionResult.getMessage(), createdOrder);
                }
            } else {
                return new TradingResult(true, "Limit sell order created and pending", createdOrder);
            }

        } catch (Exception e) {
            return new TradingResult(false, "Error processing sell order: " + e.getMessage(), null);
        }
    }

    @Transactional
    public TradingResult sellAllShares(Long portfolioId, String stockSymbol, PriceType priceType, BigDecimal limitPrice) {
        try {
            Optional<Position> position = positionService.getPositionByPortfolioAndStock(portfolioId, stockSymbol);
            if (position.isEmpty()) {
                return new TradingResult(false, "No position found for " + stockSymbol, null);
            }

            return sellStock(portfolioId, stockSymbol, position.get().getQuantity(), priceType, limitPrice);

        } catch (Exception e) {
            return new TradingResult(false, "Error selling all shares: " + e.getMessage(), null);
        }
    }

    public TradingQuote getQuote(String stockSymbol, Integer quantity, OrderType orderType) {
        try {
            Stock stock = stockService.getStock(stockSymbol);
            BigDecimal currentPrice = stock.getCurrentPrice();
            BigDecimal totalValue = currentPrice.multiply(new BigDecimal(quantity));

            return new TradingQuote(
                    stockSymbol,
                    stock.getCompanyName(),
                    currentPrice,
                    quantity,
                    orderType,
                    totalValue,
                    stock.getLastUpdated()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error getting quote for " + stockSymbol + ": " + e.getMessage());
        }
    }

    public boolean canAffordOrder(Long portfolioId, String stockSymbol, Integer quantity, OrderType orderType) {
        try {
            Portfolio portfolio = portfolioService.getPortfolio(portfolioId);

            if (orderType == OrderType.BUY) {
                Stock stock = stockService.getStock(stockSymbol);
                BigDecimal totalCost = stock.getCurrentPrice().multiply(new BigDecimal(quantity));
                return portfolio.getCashBalance().compareTo(totalCost) >= 0;
            } else { // SELL
                Optional<Position> position = positionService.getPositionByPortfolioAndStock(portfolioId, stockSymbol);
                return position.isPresent() && position.get().getQuantity() >= quantity;
            }

        } catch (Exception e) {
            return false;
        }
    }

    private ValidationResult validateBuyOrder(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        if (quantity <= 0) {
            return new ValidationResult(false, "Quantity must be positive");
        }

        if (priceType == PriceType.LIMIT && (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            return new ValidationResult(false, "Limit price must be positive for limit orders");
        }

        try {
            Portfolio portfolio = portfolioService.getPortfolio(portfolioId);
            Stock stock = stockService.getStock(stockSymbol);

            BigDecimal estimatedCost = priceType == PriceType.LIMIT ? limitPrice : stock.getCurrentPrice();
            BigDecimal totalCost = estimatedCost.multiply(new BigDecimal(quantity));

            if (portfolio.getCashBalance().compareTo(totalCost) < 0) {
                return new ValidationResult(false, "Insufficient cash balance");
            }

        } catch (Exception e) {
            return new ValidationResult(false, "Error validating order: " + e.getMessage());
        }

        return new ValidationResult(true, "Valid");
    }

    private ValidationResult validateSellOrder(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        if (quantity <= 0) {
            return new ValidationResult(false, "Quantity must be positive");
        }

        if (priceType == PriceType.LIMIT && (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            return new ValidationResult(false, "Limit price must be positive for limit orders");
        }

        try {
            Optional<Position> position = positionService.getPositionByPortfolioAndStock(portfolioId, stockSymbol);
            if (position.isEmpty() || position.get().getQuantity() < quantity) {
                return new ValidationResult(false, "Insufficient shares to sell");
            }

        } catch (Exception e) {
            return new ValidationResult(false, "Error validating order: " + e.getMessage());
        }

        return new ValidationResult(true, "Valid");
    }

    // Result DTOs
    public static class TradingResult {
        private final boolean success;
        private final String message;
        private final Order order;

        public TradingResult(boolean success, String message, Order order) {
            this.success = success;
            this.message = message;
            this.order = order;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Order getOrder() { return order; }
    }

    public static class TradingQuote {
        private final String stockSymbol;
        private final String companyName;
        private final BigDecimal currentPrice;
        private final Integer quantity;
        private final OrderType orderType;
        private final BigDecimal totalValue;
        private final java.time.LocalDateTime lastUpdated;

        public TradingQuote(String stockSymbol, String companyName, BigDecimal currentPrice,
                            Integer quantity, OrderType orderType, BigDecimal totalValue,
                            java.time.LocalDateTime lastUpdated) {
            this.stockSymbol = stockSymbol;
            this.companyName = companyName;
            this.currentPrice = currentPrice;
            this.quantity = quantity;
            this.orderType = orderType;
            this.totalValue = totalValue;
            this.lastUpdated = lastUpdated;
        }

        // Getters
        public String getStockSymbol() { return stockSymbol; }
        public String getCompanyName() { return companyName; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public Integer getQuantity() { return quantity; }
        public OrderType getOrderType() { return orderType; }
        public BigDecimal getTotalValue() { return totalValue; }
        public java.time.LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    private static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    public TradingQuote getRealTimeQuote(String stockSymbol, Integer quantity, OrderType orderType) {
        try {
            // Get real-time price data
            StockPrice realTimePrice = realTimeStockDataService.getCurrentStockPrice(stockSymbol);
            Stock stock = stockService.getStock(stockSymbol);

            BigDecimal currentPrice = realTimePrice.getPrice();
            BigDecimal totalValue = currentPrice.multiply(new BigDecimal(quantity));

            // Start tracking this symbol for real-time updates
            realTimeStockDataService.trackSymbol(stockSymbol);

            return new TradingQuote(
                    stockSymbol,
                    stock.getCompanyName(),
                    currentPrice,
                    quantity,
                    orderType,
                    totalValue,
                    realTimePrice.getTimestamp()
            );

        } catch (Exception e) {
            throw new RuntimeException("Error getting real-time quote for " + stockSymbol + ": " + e.getMessage());
        }
    }
}