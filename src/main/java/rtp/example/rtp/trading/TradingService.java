package rtp.example.rtp.trading;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rtp.example.rtp.order.*;
import rtp.example.rtp.portfolio.Portfolio;
import rtp.example.rtp.portfolio.PortfolioService;
import rtp.example.rtp.positions.Position;
import rtp.example.rtp.positions.PositionService;
import rtp.example.rtp.stock.Stock;
import rtp.example.rtp.stock.StockService;
import rtp.example.rtp.data.StockPrice;

import java.math.BigDecimal;
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
                          StockService stockService,
                          RealTimeStockDataService realTimeStockDataService) {
        this.orderService = orderService;
        this.orderExecutionService = orderExecutionService;
        this.portfolioService = portfolioService;
        this.portfolioCalculationService = portfolioCalculationService;
        this.positionService = positionService;
        this.stockService = stockService;
        this.realTimeStockDataService = realTimeStockDataService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TradingResult buyStock(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        // Validate inputs - throw exceptions that GlobalExceptionHandler will catch
        validateBuyOrder(portfolioId, stockSymbol, quantity, priceType, limitPrice);

        // Create buy order
        Order order = new Order(portfolioId, stockSymbol, OrderType.BUY, priceType, quantity, limitPrice);
        Order createdOrder = orderService.createOrder(order);

        // Execute order immediately (for market orders) or leave pending (for limit orders)
        if (priceType == PriceType.MARKET) {
            OrderExecutionService.OrderExecutionResult executionResult = orderExecutionService.executeOrder(createdOrder.getId());
            if (executionResult.isSuccess()) {
                return new TradingResult(true, "Buy order executed successfully", createdOrder);
            } else {
                throw new RuntimeException("Failed to execute buy order: " + executionResult.getMessage());
            }
        } else {
            return new TradingResult(true, "Limit buy order created and pending", createdOrder);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TradingResult sellStock(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        // Validate inputs - throw exceptions that GlobalExceptionHandler will catch
        validateSellOrder(portfolioId, stockSymbol, quantity, priceType, limitPrice);

        // Create sell order
        Order order = new Order(portfolioId, stockSymbol, OrderType.SELL, priceType, quantity, limitPrice);
        Order createdOrder = orderService.createOrder(order);

        // Execute order immediately (for market orders) or leave pending (for limit orders)
        if (priceType == PriceType.MARKET) {
            OrderExecutionService.OrderExecutionResult executionResult = orderExecutionService.executeOrder(createdOrder.getId());
            if (executionResult.isSuccess()) {
                return new TradingResult(true, "Sell order executed successfully", createdOrder);
            } else {
                throw new RuntimeException("Failed to execute sell order: " + executionResult.getMessage());
            }
        } else {
            return new TradingResult(true, "Limit sell order created and pending", createdOrder);
        }
    }

    @Transactional
    public TradingResult sellAllShares(Long portfolioId, String stockSymbol, PriceType priceType, BigDecimal limitPrice) {
        Optional<Position> position = positionService.getPositionByPortfolioAndStock(portfolioId, stockSymbol);
        if (position.isEmpty()) {
            throw new EntityNotFoundException("No position found for stock: " + stockSymbol);
        }

        return sellStock(portfolioId, stockSymbol, position.get().getQuantity(), priceType, limitPrice);
    }

    public TradingQuote getQuote(String stockSymbol, Integer quantity, OrderType orderType) {
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("Order type cannot be null");
        }

        Stock stock = stockService.getStock(stockSymbol);
        BigDecimal currentPrice = realTimeStockDataService.getCurrentStockPrice(stockSymbol).getPrice();
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
    }

    public boolean canAffordOrder(Long portfolioId, String stockSymbol, Integer quantity, OrderType orderType) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("Order type cannot be null");
        }

        Portfolio portfolio = portfolioService.getPortfolio(portfolioId);

        if (orderType == OrderType.BUY) {
            Stock stock = stockService.getStock(stockSymbol);
            BigDecimal totalCost = stock.getCurrentPrice().multiply(new BigDecimal(quantity));
            return portfolio.getCashBalance().compareTo(totalCost) >= 0;
        } else { // SELL
            Optional<Position> position = positionService.getPositionByPortfolioAndStock(portfolioId, stockSymbol);
            return position.isPresent() && position.get().getQuantity() >= quantity;
        }
    }

    private void validateBuyOrder(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (priceType == null) {
            throw new IllegalArgumentException("Price type cannot be null");
        }
        if (priceType == PriceType.LIMIT && (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Limit price must be positive for limit orders");
        }

        // These will throw EntityNotFoundException if not found
        Portfolio portfolio = portfolioService.getPortfolio(portfolioId);
        Stock stock = stockService.getStock(stockSymbol);

        BigDecimal estimatedCost = priceType == PriceType.LIMIT ? limitPrice : stock.getCurrentPrice();
        BigDecimal totalCost = estimatedCost.multiply(new BigDecimal(quantity));

        if (portfolio.getCashBalance().compareTo(totalCost) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance");
        }
    }

    private void validateSellOrder(Long portfolioId, String stockSymbol, Integer quantity, PriceType priceType, BigDecimal limitPrice) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (priceType == null) {
            throw new IllegalArgumentException("Price type cannot be null");
        }
        if (priceType == PriceType.LIMIT && (limitPrice == null || limitPrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Limit price must be positive for limit orders");
        }

        Optional<Position> position = positionService.getPositionByPortfolioAndStock(portfolioId, stockSymbol);
        if (position.isEmpty() || position.get().getQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient shares to sell");
        }
    }

    public TradingQuote getRealTimeQuote(String stockSymbol, Integer quantity, OrderType orderType) {
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("Order type cannot be null");
        }

        // Get real-time price data - this may throw StockDataException
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
}