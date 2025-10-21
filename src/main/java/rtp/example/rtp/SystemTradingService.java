package rtp.example.rtp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rtp.example.rtp.Order.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * System-level service for background tasks that operate without user context.
 * This service directly accesses repositories to bypass user-level security checks.
 */
@Service
public class SystemTradingService {

    private static final Logger logger = LoggerFactory.getLogger(SystemTradingService.class);

    private final OrderRepository orderRepository;
    private final OrderExecutionService orderExecutionService;
    private final RealTimeStockDataService realTimeStockDataService;

    public SystemTradingService(OrderRepository orderRepository,
                                OrderExecutionService orderExecutionService,
                                RealTimeStockDataService realTimeStockDataService) {
        this.orderRepository = orderRepository;
        this.orderExecutionService = orderExecutionService;
        this.realTimeStockDataService = realTimeStockDataService;
    }

    /**
     * Background task to process pending limit orders.
     * Runs every 15 seconds without user authentication context.
     */
    @Scheduled(fixedRate = 15000)
    @Async
    public void processPendingLimitOrders() {
        try {
            // Direct repository access - bypasses user security
            List<Order> pendingLimitOrders = orderRepository.findByStatus(OrderStatus.PENDING);

            if (pendingLimitOrders.isEmpty()) {
                return;
            }

            logger.info("Processing {} pending limit orders", pendingLimitOrders.size());

            for (Order order : pendingLimitOrders) {
                if (order.getPriceType() != PriceType.LIMIT) {
                    continue;
                }

                try {
                    processLimitOrder(order);
                } catch (Exception e) {
                    logger.error("Error processing limit order ID {}: {}",
                            order.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in processPendingLimitOrders: {}", e.getMessage(), e);
        }
    }

    private void processLimitOrder(Order order) {
        try {
            // Fetch current real-time price
            StockPrice currentPrice = realTimeStockDataService.getCurrentStockPrice(order.getStockSymbol());
            BigDecimal marketPrice = currentPrice.getPrice();

            boolean shouldExecute = shouldExecuteLimitOrder(order, marketPrice);

            if (shouldExecute) {
                OrderExecutionService.OrderExecutionResult result =
                        orderExecutionService.executeOrder(order.getId());

                if (result.isSuccess()) {
                    logger.info("Successfully executed limit order ID {} at price {}",
                            order.getId(), marketPrice);
                } else {
                    logger.warn("Failed to execute limit order ID {}: {}",
                            order.getId(), result.getMessage());
                }
            }
        } catch (StockDataException e) {
            logger.warn("Could not fetch price for {} when processing order {}: {}",
                    order.getStockSymbol(), order.getId(), e.getMessage());
        }
    }

    private boolean shouldExecuteLimitOrder(Order order, BigDecimal marketPrice) {
        if (order.getOrderType() == OrderType.BUY) {
            // For limit buy: execute if market price <= limit price
            return marketPrice.compareTo(order.getLimitPrice()) <= 0;
        } else if (order.getOrderType() == OrderType.SELL) {
            // For limit sell: execute if market price >= limit price
            return marketPrice.compareTo(order.getLimitPrice()) >= 0;
        }
        return false;
    }
}