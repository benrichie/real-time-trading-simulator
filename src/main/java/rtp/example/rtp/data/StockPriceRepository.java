package rtp.example.rtp.data;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

    @Query(value = "Select * FROM stock_prices WHERE symbol = ?1 ORDER BY timestamp DESC LIMIT 1", nativeQuery = true)
    Optional<StockPrice> findLatestBySymbol(String symbol);

    @Query("SELECT DISTINCT sp.symbol FROM StockPrice sp")
    List<String> findAllUniqueSymbols();

    List<StockPrice> findBySymbolAndTimestampAfterOrderByTimestampDesc(String symbol, LocalDateTime after);

    void deleteBySymbolAndTimestampBefore(String symbol, LocalDateTime before);
}
