package rtp.example.rtp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_prices")
public class StockPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(precision = 19, scale = 4)
    private BigDecimal changeAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal changePercent;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String source; // "ALPHA_VANTAGE", "FINNHUB", etc.

    // Constructors
    public StockPrice() {}

    public StockPrice(String symbol, BigDecimal price, BigDecimal changeAmount,
                      BigDecimal changePercent, Long volume, String source) {
        this.symbol = symbol;
        this.price = price;
        this.changeAmount = changeAmount;
        this.changePercent = changePercent;
        this.volume = volume;
        this.source = source;
        this.timestamp = LocalDateTime.now();
    }
}