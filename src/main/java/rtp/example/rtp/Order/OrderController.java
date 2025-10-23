package rtp.example.rtp.Order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rtp.example.rtp.OrderExecutionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderExecutionService orderExecutionService;

    public OrderController(OrderService orderService,
                           OrderExecutionService orderExecutionService) {
        this.orderService = orderService;
        this.orderExecutionService = orderExecutionService;
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @GetMapping("/portfolio/{portfolioId}")
    public List<Order> getOrdersByPortfolio(@PathVariable Long portfolioId) {
        return orderService.getOrdersByPortfolio(portfolioId);
    }

    @GetMapping("/pending")
    public List<Order> getPendingOrders() {
        return orderService.getPendingOrders();
    }

    @PostMapping
    public ResponseEntity<OrderCreationResponse> createOrder(@RequestBody OrderCreationRequest request) {
        // Validate order creation request
        ValidationResult validation = validateOrderCreation(request);
        if (!validation.isValid()) {
            return ResponseEntity.badRequest()
                    .body(new OrderCreationResponse(false, validation.getMessage(), null));
        }

        Order order = new Order(
                request.getPortfolioId(),
                request.getStockSymbol(),
                request.getOrderType(),
                request.getPriceType(),
                request.getQuantity(),
                request.getLimitPrice()
        );

        Order createdOrder = orderService.createOrder(order);

        return ResponseEntity.ok(new OrderCreationResponse(
                true,
                "Order created successfully",
                createdOrder
        ));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<OrderExecutionService.OrderExecutionResult> executeOrder(@PathVariable Long id) {
        OrderExecutionService.OrderExecutionResult result = orderExecutionService.executeOrder(id);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/execute-pending")
    public ResponseEntity<BatchExecutionResult> executePendingOrders() {
        List<Order> pendingOrders = orderService.getPendingOrders();
        int successful = 0;
        int failed = 0;
        StringBuilder messages = new StringBuilder();

        for (Order order : pendingOrders) {
            OrderExecutionService.OrderExecutionResult result = orderExecutionService.executeOrder(order.getId());
            if (result.isSuccess()) {
                successful++;
            } else {
                failed++;
                messages.append("Order ").append(order.getId())
                        .append(": ").append(result.getMessage()).append("; ");
            }
        }

        return ResponseEntity.ok(new BatchExecutionResult(
                successful,
                failed,
                successful + " orders executed successfully" +
                        (failed > 0 ? ", " + failed + " failed: " + messages : "")
        ));
    }

    @PutMapping("/{id}/status")
    public Order updateOrderStatus(@PathVariable Long id, @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateOrderStatus(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrder(@PathVariable Long id) {
        Order order = orderService.getOrder(id);
        if (order.getStatus() == OrderStatus.FILLED) {
            return ResponseEntity.badRequest().body("Cannot delete filled orders");
        }
        orderService.deleteOrder(id);
        return ResponseEntity.ok("Order deleted successfully");
    }

    private ValidationResult validateOrderCreation(OrderCreationRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return new ValidationResult(false, "Quantity must be positive");
        }

        if (request.getPriceType() == PriceType.LIMIT && request.getLimitPrice() == null) {
            return new ValidationResult(false, "Limit price is required for limit orders");
        }

        if (request.getPriceType() == PriceType.LIMIT &&
                request.getLimitPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return new ValidationResult(false, "Limit price must be positive");
        }

        return new ValidationResult(true, "Valid");
    }

    // Request/Response DTOs
    public static class OrderCreationRequest {
        private Long portfolioId;
        private String stockSymbol;
        private OrderType orderType;
        private PriceType priceType;
        private Integer quantity;
        private java.math.BigDecimal limitPrice;

        // Getters and setters
        public Long getPortfolioId() { return portfolioId; }
        public void setPortfolioId(Long portfolioId) { this.portfolioId = portfolioId; }
        public String getStockSymbol() { return stockSymbol; }
        public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
        public OrderType getOrderType() { return orderType; }
        public void setOrderType(OrderType orderType) { this.orderType = orderType; }
        public PriceType getPriceType() { return priceType; }
        public void setPriceType(PriceType priceType) { this.priceType = priceType; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public java.math.BigDecimal getLimitPrice() { return limitPrice; }
        public void setLimitPrice(java.math.BigDecimal limitPrice) { this.limitPrice = limitPrice; }
    }

    public static class OrderCreationResponse {
        private final boolean success;
        private final String message;
        private final Order order;

        public OrderCreationResponse(boolean success, String message, Order order) {
            this.success = success;
            this.message = message;
            this.order = order;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Order getOrder() { return order; }
    }

    public static class OrderStatusUpdateRequest {
        private OrderStatus status;

        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
    }

    public static class BatchExecutionResult {
        private final int successful;
        private final int failed;
        private final String message;

        public BatchExecutionResult(int successful, int failed, String message) {
            this.successful = successful;
            this.failed = failed;
            this.message = message;
        }

        public int getSuccessful() { return successful; }
        public int getFailed() { return failed; }
        public String getMessage() { return message; }
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