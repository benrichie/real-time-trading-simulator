package rtp.example.rtp.order;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rtp.example.rtp.portfolio.PortfolioService;
import rtp.example.rtp.user.User;
import rtp.example.rtp.user.UserRepository;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        PortfolioService portfolioService,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.portfolioService = portfolioService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user.getId();
    }

    private void verifyOrderOwnership(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Verify the user owns the portfolio for this order
        portfolioService.getPortfolio(order.getPortfolioId());
    }

    // Admin only - restrict in controller
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrder(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        verifyOrderOwnership(id);

        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + id));
    }

    public List<Order> getOrdersByPortfolio(Long portfolioId) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }

        // This will verify ownership
        portfolioService.getPortfolio(portfolioId);

        return orderRepository.findByPortfolioId(portfolioId);
    }

    public List<Order> getPendingOrders() {
        // Get current user's portfolios and filter pending orders
        Long currentUserId = getCurrentUserId();

        // Get all pending orders
        List<Order> allPendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        // Filter to only include orders for user's portfolios
        return allPendingOrders.stream()
                .filter(order -> {
                    try {
                        portfolioService.getPortfolio(order.getPortfolioId());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();
    }

    public Order createOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        if (order.getPortfolioId() == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }

        // Verify user owns the portfolio
        portfolioService.getPortfolio(order.getPortfolioId());

        return orderRepository.save(order);
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        verifyOrderOwnership(orderId);

        Order order = getOrder(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        verifyOrderOwnership(id);

        orderRepository.deleteById(id);
    }
}