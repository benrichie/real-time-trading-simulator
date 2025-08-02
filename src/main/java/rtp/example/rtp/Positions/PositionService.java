package rtp.example.rtp.Positions;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }

    public Position getPosition(Long id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Position not found: " + id));
    }

    public List<Position> getPositionsByPortfolio(Long portfolioId) {
        return positionRepository.findByPortfolioId(portfolioId);
    }

    public Optional<Position> getPositionByPortfolioAndStock(Long portfolioId, String stockSymbol) {
        return positionRepository.findByPortfolioIdAndStockSymbol(portfolioId, stockSymbol);
    }

    public Position createPosition(Position position) {
        return positionRepository.save(position);
    }

    public Position updatePosition(Position position) {
        return positionRepository.save(position);
    }

    public void deletePosition(Long id) {
        positionRepository.deleteById(id);
    }
}