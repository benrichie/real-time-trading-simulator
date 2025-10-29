package rtp.example.rtp.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
    public interface StockRepository extends JpaRepository<Stock, String>{
        Optional<Stock> findBySymbol(String upperCase);
        // String because symbol (our @Id) is String, not Long
    }
