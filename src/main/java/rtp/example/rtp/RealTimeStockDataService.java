package rtp.example.rtp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RealTimeStockDataService {

    private static class stockApiResponse {
        @JsonProperty("c")
        private BigDecimal currentPrice;

        @JsonProperty("d")
        private BigDecimal change;

        @JsonProperty("dp")
        private BigDecimal changePercent;

        @JsonProperty("v")
        private Long volume;

        public BigDecimal currentprice() { return currentPrice; }
        public BigDecimal getChange() { return change; }
        public BigDecimal getChangePercent() { return changePercent; }
        public Long getVolume() { return volume; }
    }

    public static class PriceUpdateMessage {
        private String symbol;
        private BigDecimal price;
        private BigDecimal change;
        private BigDecimal changePercent;
        private LocalDateTime timestamp;

        public PriceUpdateMessage(String symbol, BigDecimal price, BigDecimal change,
                                  BigDecimal changePercent, LocalDateTime timestamp) {
            this.symbol = symbol;
            this.price = price;
            this.change = change;
            this.changePercent = changePercent;
            this.timestamp = timestamp;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getChange() { return change; }
        public BigDecimal getChangePercent() { return changePercent; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

}
