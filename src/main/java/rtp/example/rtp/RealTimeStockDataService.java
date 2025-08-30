package rtp.example.rtp;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import rtp.example.rtp.Stock.Stock;
import rtp.example.rtp.Stock.StockService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealTimeStockDataService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeStockDataService.class);

    @Value("${stock.api.key}")
    private String apiKey;
    @Value("${stock.api.url}")
    private String apiUrl;

    private StockService stockService;
    private StockPriceRepository stockPriceRepository;
    private SimpMessagingTemplate messagingTemplate;
    private RestTemplate restTemplate;

    // Track active symbols that need real-time update
    private final Set<String> activeSymbols = ConcurrentHashMap.newKeySet();

    private static class StockApiResponse {
        @JsonProperty("c")
        private BigDecimal currentPrice;

        @JsonProperty("d")
        private BigDecimal change;

        @JsonProperty("dp")
        private BigDecimal changePercent;

        @JsonProperty("v")
        private Long volume;

        public BigDecimal getCurrentPrice() {
            return currentPrice;
        }

        public BigDecimal getChange() {
            return change;
        }

        public BigDecimal getChangePercent() {
            return changePercent;
        }

        public Long getVolume() {
            return volume;
        }
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

        public String getSymbol() {
            return symbol;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public BigDecimal getChange() {
            return change;
        }

        public BigDecimal getChangePercent() {
            return changePercent;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    //Constructor
    @Autowired
    public RealTimeStockDataService(StockService stockService, StockPriceRepository stockPriceRepository, SimpMessagingTemplate messagingTemplate, RestTemplate restTemplate) {
        this.stockService = stockService;
        this.stockPriceRepository = stockPriceRepository;
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
    }


    // Get current stock price with caching (30 second TTL)
    @Cacheable(value = "stock-prices", key = "#symbol")
    public StockPrice getCurrentStockPrice(String symbol) {
        try {
            // First check cache/recent data
            Optional<StockPrice> recentPrice = stockPriceRepository.findLatestBySymbol(symbol);
            if (recentPrice.isPresent() &&
                    recentPrice.get().getTimestamp().isAfter(LocalDateTime.now().minusSeconds(30))) {
                return recentPrice.get();
            }

            // Fetch from external API
            return fetchAndUpdateStockPrice(symbol);

        } catch (Exception e) {
            logger.error("Failed to get current stock price for {}", symbol, e);
            throw new StockDataException("Unable to fetch current price for " + symbol);
        }
    }

    private StockPrice fetchAndUpdateStockPrice(String symbol) {
        try {
            String url = String.format("%/quote?symbol=%s&token=%s", apiUrl, symbol, apiKey);

            StockApiResponse response = restTemplate.getForObject(url, StockApiResponse.class);

            if(response == null || response.getCurrentPrice() == null) {
                throw new StockDataException("Invalid response from stock API for " + symbol);
            }

            // Create new StockPrice record
            StockPrice stockPrice = new StockPrice(
                    symbol,
                    response.getCurrentPrice(),
                    response.getChange(),
                    response.getChangePercent(),
                    response.getVolume(),
                    "FINNHUB"
            );

            // Save price history
            stockPriceRepository.save(stockPrice);

            // Update price history
            Stock stock = stockService.getStock(symbol);
            stock.setCurrentPrice(response.getCurrentPrice());
            stock.setLastUpdated(LocalDateTime.now());
            stockService.updateStock(stock);

            //broadcastPriceUpdate

            logger.debug("Updated price for {}: ${}", symbol, response.getCurrentPrice());
            return stockPrice;

        } catch (Exception e) {
            logger.error("Failed to fetch stock price for {}", symbol, e);
            throw new StockDataException("Failed to fetch price for " + symbol + ": " + e.getMessage());
        }
    }

    // Add symbol to active tracking
    public void trackSymbol(String symbol) {
        activeSymbols.add(symbol.toUpperCase());
        logger.info("Now tracking symbol: {}", symbol);
    }

    // Remove symbol from active tracking
    public void stopTrackingSymbol(String symbol) {
        activeSymbols.remove(symbol.toUpperCase());
        logger.info("Stopped tracking symbol: {}", symbol);
    }

    // Get all actively tracked symbols
    public Set<String> getActiveSymbols() {
        return Set.copyOf(activeSymbols);
    }

    // Scheduled update for all active symbols (every 30 seconds during market hours)
    @Scheduled(fixedRate = 30000) // 30 seconds
    @Async
    public void updateActiveStockPrices() {
        if (activeSymbols.isEmpty()) {
            return;
        }

        logger.info("Updating prices for {} active symbols", activeSymbols.size());

        activeSymbols.parallelStream().forEach(symbol -> {
            try {
                fetchAndUpdateStockPrice(symbol);
            } catch (Exception e) {
                logger.warn("Failed to update price for symbol: {}", symbol, e);
            }
        });
    }

}

    // Fetch from external API and update both Stock and StockPrice entities

