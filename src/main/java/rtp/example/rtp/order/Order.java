package rtp.example.rtp.order;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {
    @Version
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long portfolioId;

    @Column(nullable = false)
    private String stockSymbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType; // BUY, SELL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceType priceType; // MARKET, LIMIT

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal limitPrice; // Only for LIMIT orders

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(precision = 19, scale = 4)
    private BigDecimal filledPrice; // Price when executed

    private LocalDateTime createdAt;
    private LocalDateTime filledAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Order() {}

    public Order(Long portfolioId, String stockSymbol, OrderType orderType,
                 PriceType priceType, Integer quantity, BigDecimal limitPrice) {
        this.portfolioId = portfolioId;
        this.stockSymbol = stockSymbol;
        this.orderType = orderType;
        this.priceType = priceType;
        this.quantity = quantity;
        this.limitPrice = limitPrice;
    }

    // Getters and setters
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(Long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getFilledPrice() {
        return filledPrice;
    }

    public void setFilledPrice(BigDecimal filledPrice) {
        this.filledPrice = filledPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(LocalDateTime filledAt) {
        this.filledAt = filledAt;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
