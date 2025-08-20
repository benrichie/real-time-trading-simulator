package rtp.example.rtp;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import rtp.example.rtp.Stock.StockService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class RealTimeStockDataService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeStockDataService.class);

    @Value("${stock.api.key}") private String apiKey;
    @Value("${stock.api.url}") private String apiUrl;

    private final StockService stockService;
    private final StockPriceRepository stockPriceRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    private static class StockApiResponse {
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

        public String getSymbol() { return symbol; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getChange() { return change; }
        public BigDecimal getChangePercent() { return changePercent; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    //Constructor
    @Autowired
    public RealTimeStockDataService(StockService stockService, StockPriceRepository stockPriceRepository, SimpMessagingTemplate messagingTemplate, RestTemplate restTemplate) {
        this.stockService = stockService;
        this.stockPriceRepository = stockPriceRepository;
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
    }

}
