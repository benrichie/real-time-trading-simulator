package rtp.example.rtp.positions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByPortfolioId(Long portfolioId);
    Optional<Position> findByPortfolioIdAndStockSymbol(Long portfolioId, String stockSymbol);
    List<Position> findByPortfolioIdIn(List<Long> portfolioIds);
}
