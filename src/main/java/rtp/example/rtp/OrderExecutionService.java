package rtp.example.rtp;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rtp.example.rtp.Order.*;
import rtp.example.rtp.Portfolio.Portfolio;
import rtp.example.rtp.Portfolio.PortfolioService;
import rtp.example.rtp.PortfolioCalculationService;
import rtp.example.rtp.Positions.Position;
import rtp.example.rtp.Positions.PositionService;
import rtp.example.rtp.Stock.Stock;
import rtp.example.rtp.Stock.StockService;
import rtp.example.rtp.Transaction.Transaction;
import rtp.example.rtp.Transaction.TransactionService;
import rtp.example.rtp.Transaction.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrderExecutionService {

    private final OrderService orderService;
    private final PortfolioService portfolioService;
    private final PositionService positionService;
    private final StockService stockService;
    private final TransactionService transactionService;
    private final PortfolioCalculationService portfolioCalculationService;
    private final RealTimeStockDataService realTimeStockDataService;

    public OrderExecutionService(OrderService orderService,
                                 PortfolioService portfolioService,
                                 PositionService positionService,
                                 StockService stockService,
                                 TransactionService transactionService,
                                 PortfolioCalculationService portfolioCalculationService, RealTimeStockDataService realTimeStockDataService) {
        this.orderService = orderService;
        this.portfolioService = portfolioService;
        this.positionService = positionService;
        this.stockService = stockService;
        this.transactionService = transactionService;
        this.portfolioCalculationService = portfolioCalculationService;
        this.realTimeStockDataService = realTimeStockDataService;
    }

    private static final Logger logger = LoggerFactory.getLogger(OrderExecutionService.class);

    @Transactional
    public OrderExecutionResult executeOrder(Long orderId) {
        Order order = orderService.getOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            return new OrderExecutionResult(false, "Order is not in PENDING status");
        }

        try {
            // Get current stock price for execution
            Stock stock = stockService.getStock(order.getStockSymbol());
            BigDecimal executionPrice = determineExecutionPrice(order, stock.getCurrentPrice());

            if (executionPrice == null) {
                order.setStatus(OrderStatus.CANCELLED);
                orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
                return new OrderExecutionResult(false, "Order cannot be executed at current market conditions");
            }

            // Validate the order can be executed
            ValidationResult validation = validateOrderExecution(order, executionPrice);
            if (!validation.isValid()) {
                order.setStatus(OrderStatus.CANCELLED);
                orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
                return new OrderExecutionResult(false, validation.getMessage());
            }

            // Execute the order
            executeOrderTransaction(order, executionPrice);

            // Update order status
            order.setStatus(OrderStatus.FILLED);
            order.setFilledPrice(executionPrice);
            order.setFilledAt(LocalDateTime.now());
            orderService.updateOrderStatus(orderId, OrderStatus.FILLED);

            // Recalculate portfolio values
            portfolioCalculationService.recalculatePortfolio(order.getPortfolioId());

            return new OrderExecutionResult(true, "Order executed successfully", executionPrice);

        } catch (OptimisticLockException ole) {
            logger.warn("Optimistic lock conflict when executing order {}", orderId);
            return new OrderExecutionResult(false, "Order is being processed concurrently. Please try again.");
        } catch (Exception e) {
            order.setStatus(OrderStatus.CANCELLED);
            orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
            return new OrderExecutionResult(false, "Error executing order: " + e.getMessage());
        }
    }

    private BigDecimal determineExecutionPrice(Order order, BigDecimal currentMarketPrice) {
        try {
            StockPrice realTimePrice = realTimeStockDataService.getCurrentStockPrice(order.getStockSymbol());
            currentMarketPrice = realTimePrice.getPrice();
        } catch (Exception e) {
            logger.warn("Failed to get real-time price for {}, using cached price", order.getStockSymbol());
            // fallback to passed currentMarketPrice
        }
        if (order.getPriceType() == PriceType.MARKET) {
            return currentMarketPrice;
        } else { // LIMIT order
            if (order.getOrderType() == OrderType.BUY) {
                return currentMarketPrice.compareTo(order.getLimitPrice()) <= 0 ? currentMarketPrice : null;
            } else { // SELL
                return currentMarketPrice.compareTo(order.getLimitPrice()) >= 0 ? currentMarketPrice : null;
            }
        }
    }

    private ValidationResult validateOrderExecution(Order order, BigDecimal executionPrice) {
        Portfolio portfolio = portfolioService.getPortfolio(order.getPortfolioId());
        BigDecimal totalCost = executionPrice.multiply(new BigDecimal(order.getQuantity()));

        if (order.getOrderType() == OrderType.BUY) {
            // Check if user has enough cash balance
            if (portfolio.getCashBalance().compareTo(totalCost) < 0) {
                return new ValidationResult(false, "Insufficient cash balance");
            }
        } else { // SELL
            // Check if user has enough shares to sell
            Optional<Position> position = positionService.getPositionByPortfolioAndStock(
                    order.getPortfolioId(), order.getStockSymbol());

            if (position.isEmpty() || position.get().getQuantity() < order.getQuantity()) {
                return new ValidationResult(false, "Insufficient shares to sell");
            }
        }

        return new ValidationResult(true, "Valid");
    }

    private void executeOrderTransaction(Order order, BigDecimal executionPrice) {
        Portfolio portfolio = portfolioService.getPortfolio(order.getPortfolioId());
        BigDecimal totalAmount = executionPrice.multiply(new BigDecimal(order.getQuantity()));

        if (order.getOrderType() == OrderType.BUY) {
            executeBuyOrder(order, portfolio, executionPrice, totalAmount);
        } else {
            executeSellOrder(order, portfolio, executionPrice, totalAmount);
        }

        // Create transaction record
        Transaction transaction = new Transaction(
                order.getId(),
                order.getPortfolioId(),
                order.getStockSymbol(),
                order.getOrderType() == OrderType.BUY ? TransactionType.BUY : TransactionType.SELL,
                order.getQuantity(),
                executionPrice
        );
        transactionService.createTransaction(transaction);
    }

    private void executeBuyOrder(Order order, Portfolio portfolio, BigDecimal executionPrice, BigDecimal totalAmount) {
        // Update cash balance
        BigDecimal newCashBalance = portfolio.getCashBalance().subtract(totalAmount);
        portfolio.setCashBalance(newCashBalance);
        portfolioService.updatePortfolio(portfolio);

        // Update or create position
        Optional<Position> existingPosition = positionService.getPositionByPortfolioAndStock(
                order.getPortfolioId(), order.getStockSymbol());

        if (existingPosition.isPresent()) {
            // Update existing position with average price calculation
            Position position = existingPosition.get();
            BigDecimal currentValue = position.getAveragePrice().multiply(new BigDecimal(position.getQuantity()));
            BigDecimal newValue = executionPrice.multiply(new BigDecimal(order.getQuantity()));
            BigDecimal totalValue = currentValue.add(newValue);

            int newQuantity = position.getQuantity() + order.getQuantity();
            BigDecimal newAveragePrice = totalValue.divide(new BigDecimal(newQuantity), 4, RoundingMode.HALF_UP);

            position.setQuantity(newQuantity);
            position.setAveragePrice(newAveragePrice);
            positionService.updatePosition(position);
        } else {
            // Create new position
            Position newPosition = new Position(
                    order.getPortfolioId(),
                    order.getStockSymbol(),
                    order.getQuantity(),
                    executionPrice
            );
            positionService.createPosition(newPosition);
        }
    }

    private void executeSellOrder(Order order, Portfolio portfolio, BigDecimal executionPrice, BigDecimal totalAmount) {
        // Update cash balance
        BigDecimal newCashBalance = portfolio.getCashBalance().add(totalAmount);
        portfolio.setCashBalance(newCashBalance);
        portfolioService.updatePortfolio(portfolio);

        // Update position
        Position position = positionService.getPositionByPortfolioAndStock(
                order.getPortfolioId(), order.getStockSymbol()).orElseThrow();

        int newQuantity = position.getQuantity() - order.getQuantity();

        if (newQuantity == 0) {
            // Remove position if quantity becomes zero
            positionService.deletePosition(position.getId());
        } else {
            // Update position quantity (average price remains the same)
            position.setQuantity(newQuantity);
            positionService.updatePosition(position);
        }
    }

    // Utility classes
    public static class OrderExecutionResult {
        private final boolean success;
        private final String message;
        private final BigDecimal executionPrice;

        public OrderExecutionResult(boolean success, String message) {
            this(success, message, null);
        }

        public OrderExecutionResult(boolean success, String message, BigDecimal executionPrice) {
            this.success = success;
            this.message = message;
            this.executionPrice = executionPrice;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BigDecimal getExecutionPrice() { return executionPrice; }
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
}