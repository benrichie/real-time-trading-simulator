package rtp.example.rtp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    private String symbol; // "AAPL", "GOOGL"

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, precision = 19, scale =4 )
    private BigDecimal currentPrice; // NEVER Double for money!

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        this.lastUpdated = LocalDateTime.now();
    }

    //No-arg contrustor(JPA requirement)
    public Stock() {}

    public Stock(String symbol, String companyName, BigDecimal currentPrice){
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stock)) return false;
        Stock stock = (Stock) o;
        return Objects.equals(symbol, stock.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }



}
