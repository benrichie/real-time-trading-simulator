package rtp.example.rtp.Stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

    @Repository
    public interface StockRepository extends JpaRepository<Stock, String>{
        // String because symbol (our @Id) is String, not Long
    }
