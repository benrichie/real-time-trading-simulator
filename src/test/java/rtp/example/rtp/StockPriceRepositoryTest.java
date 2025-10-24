package rtp.example.rtp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import rtp.example.rtp.data.StockPrice;
import rtp.example.rtp.data.StockPriceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StockPriceRepositoryTest {

    @Autowired
    private StockPriceRepository repository;

    @Test
    void testSaveAndFindLatestBySymbol() {
        // Arrange
        StockPrice price1 = new StockPrice();
        price1.setSymbol("AAPL");
        price1.setPrice(BigDecimal.valueOf(150.25));
        price1.setTimestamp(LocalDateTime.of(2025, 8, 13, 10, 0));
        repository.save(price1);

        StockPrice price2 = new StockPrice();
        price2.setSymbol("AAPL");
        price2.setPrice(BigDecimal.valueOf(151.75));
        price2.setTimestamp(LocalDateTime.of(2025, 8, 13, 11, 0));
        repository.save(price2);

        // Act
        Optional<StockPrice> latest = repository.findLatestBySymbol("AAPL");

        // Assert
        assertThat(latest).isPresent();
        assertThat(latest.get().getPrice()).isEqualTo(151.75);
    }

    @Test
    void testFindAllUniqueSymbols() {
        StockPrice price1 = new StockPrice();
        price1.setSymbol("AAPL");
        price1.setPrice(BigDecimal.valueOf(150));
        price1.setTimestamp(LocalDateTime.now());
        price1.setVolume(100L);
        price1.setSource("TEST");
        repository.save(price1);

        StockPrice price2 = new StockPrice();
        price2.setSymbol("GOOG");
        price2.setPrice(BigDecimal.valueOf(2800));
        price2.setTimestamp(LocalDateTime.now());
        price2.setVolume(50L);
        price2.setSource("TEST");
        repository.save(price2);

        StockPrice price3 = new StockPrice();
        price3.setSymbol("AAPL");
        price3.setPrice(BigDecimal.valueOf(152));
        price3.setTimestamp(LocalDateTime.now());
        price3.setVolume(200L);
        price3.setSource("TEST");
        repository.save(price3);

        var symbols = repository.findAllUniqueSymbols();
        assertThat(symbols).containsExactlyInAnyOrder("AAPL", "GOOG");
    }
}
