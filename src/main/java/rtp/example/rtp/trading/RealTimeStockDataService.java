// Fixed and cleaned RealTimeStockDataService:
// - removed duplicate/malformed code (plan step 1)
// - validated API response and defaulted nullable numeric fields before saving (plan step 3)
// - preserved caching, tracking, scheduled updates and cleanup (steps 2,4,5)
package rtp.example.rtp.trading;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import rtp.example.rtp.stock.Stock;
import rtp.example.rtp.stock.StockService;
import rtp.example.rtp.common.exception.StockDataException;
import rtp.example.rtp.data.StockPrice;
import rtp.example.rtp.data.StockPriceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
//implement global exception handler
@Service
public class RealTimeStockDataService {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeStockDataService.class);

    @Value("${stock.api.key}")
    private String apiKey;

    @Value("${stock.api.url}")
    private String apiUrl;

    private final StockService stockService;
    private final StockPriceRepository stockPriceRepository;
    private final RestTemplate restTemplate;

    // Track active symbols that need real-time update
    private final Set<String> activeSymbols = ConcurrentHashMap.newKeySet();

    // API response DTO mapping
    private static class StockApiResponse {
        @JsonProperty("c")
        private BigDecimal currentPrice;

        @JsonProperty("d")
        private BigDecimal change;

        @JsonProperty("dp")
        private BigDecimal changePercent;

        @JsonProperty("v")
        private Long volume;

        public BigDecimal getCurrentPrice() { return currentPrice; }
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

    // Constructor (step 1)
    @Autowired
    public RealTimeStockDataService(StockService stockService,
                                    StockPriceRepository stockPriceRepository,
                                    RestTemplate restTemplate) {
        this.stockService = stockService;
        this.stockPriceRepository = stockPriceRepository;
        this.restTemplate = restTemplate;
    }

    // Get current stock price with caching (30 second TTL) - step 2
    @Cacheable(value = "stock-prices", key = "#symbol")
    public StockPrice getCurrentStockPrice(String symbol) {
        try {
            Optional<StockPrice> recentPrice = stockPriceRepository.findLatestBySymbol(symbol);
            if (recentPrice.isPresent() &&
                    recentPrice.get().getTimestamp().isAfter(LocalDateTime.now().minusSeconds(30))) {
                logger.debug("Returning cached price for symbol {}", symbol);
                return recentPrice.get();
            } else {
                logger.warn("Cached price for symbol {} is stale or missing, fetching fresh data", symbol);
                return fetchAndUpdateStockPrice(symbol);
            }
        } catch (Exception e) {
            logger.error("Failed to get current stock price for {}", symbol, e);
            throw new StockDataException("Unable to fetch current price for " + symbol, e);
        }
    }


    // Fetch from external API, persist, update Stock entity (steps 3-4)
    private StockPrice fetchAndUpdateStockPrice(String symbol) {
        try {
            String url = String.format("%s/quote?symbol=%s&token=%s", apiUrl, symbol, apiKey);
            StockApiResponse response = restTemplate.getForObject(url, StockApiResponse.class);

            if (response == null || response.getCurrentPrice() == null) {
                throw new StockDataException("Invalid response from stock API for " + symbol);
            }

            // Default nullable numeric fields to safe values (plan step 3)
            BigDecimal change = response.getChange() != null ? response.getChange() : BigDecimal.ZERO;
            BigDecimal changePercent = response.getChangePercent() != null ? response.getChangePercent() : BigDecimal.ZERO;
            Long volume = response.getVolume() != null ? response.getVolume() : 0L;

            // Create new StockPrice record
            StockPrice stockPrice = new StockPrice(
                    symbol,
                    response.getCurrentPrice(),
                    change,
                    changePercent,
                    volume,
                    "FINNHUB"
            );

            // Save price history
            stockPriceRepository.save(stockPrice);

            // Update Stock entity with latest price (if present in DB)
            try {
                Stock stock = stockService.getStock(symbol);
                stock.setCurrentPrice(response.getCurrentPrice());
                stock.setLastUpdated(LocalDateTime.now());
                stockService.updateStock(stock);
            } catch (RuntimeException e) {
                // Stock might not exist yet; that's fine — skip update
                logger.debug("Stock {} not found in DB to update current price; will skip update.", symbol);
            }

            logger.debug("Updated price for {}: {}", symbol, response.getCurrentPrice());
            return stockPrice;

        } catch (StockDataException e) {
            logger.error("StockDataException while fetching price for {}", symbol, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to fetch stock price for {}", symbol, e);
            throw new StockDataException("Failed to fetch price for " + symbol + ": " + e.getMessage(), e);
        }
    }

    // Add symbol to active tracking
    public void trackSymbol(String symbol) {
        if (symbol != null) {
            activeSymbols.add(symbol.toUpperCase());
            logger.info("Now tracking symbol: {}", symbol.toUpperCase());
        }
    }

    // Remove symbol from active tracking
    public void stopTrackingSymbol(String symbol) {
        if (symbol != null) {
            activeSymbols.remove(symbol.toUpperCase());
            logger.info("Stopped tracking symbol: {}", symbol.toUpperCase());
        }
    }

    // Get all actively tracked symbols
    public Set<String> getActiveSymbols() {
        return Set.copyOf(activeSymbols);
    }

    // Scheduled update for all active symbols (every 30 seconds) - step 5
    @Scheduled(fixedRate = 30000)
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

    // Update multiple stocks and start tracking them
    public void updateMultipleStockPrices(Set<String> symbols) {
        if (symbols == null || symbols.isEmpty()) return;
        symbols.forEach(this::trackSymbol);
        symbols.parallelStream().forEach(symbol -> {
            try {
                fetchAndUpdateStockPrice(symbol);
            } catch (Exception e) {
                logger.error("Failed to update price for {}", symbol, e);
            }
        });
    }

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void cleanupOnStartUp() {
        cleanupOldPriceData();
    }

    @Transactional
    public void cleanupOldPriceData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(14);
        List<String> symbols = stockPriceRepository.findAllUniqueSymbols();
        for (String symbol : symbols) {
            stockPriceRepository.deleteBySymbolAndTimestampBefore(symbol, cutoff);
        }
        logger.info("Cleaned up stock price data older than {}", cutoff);
    }
}
