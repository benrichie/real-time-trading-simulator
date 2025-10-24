package rtp.example.rtp.positions;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rtp.example.rtp.portfolio.PortfolioService;
import rtp.example.rtp.user.User;
import rtp.example.rtp.user.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PositionService {

    private final PositionRepository positionRepository;
    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    public PositionService(PositionRepository positionRepository,
                           PortfolioService portfolioService,
                           UserRepository userRepository) {
        this.positionRepository = positionRepository;
        this.portfolioService = portfolioService;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user.getId();
    }

    private void verifyPositionOwnership(Long positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new EntityNotFoundException("Position not found with ID: " + positionId));

        // Verify the user owns the portfolio that contains this position
        portfolioService.getPortfolio(position.getPortfolioId());
    }

    // Admin only - restrict in controller
    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }

    public Position getPosition(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Position ID cannot be null");
        }

        verifyPositionOwnership(id);

        return positionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Position not found: " + id));
    }

    public List<Position> getPositionsByPortfolio(Long portfolioId) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }

        // This will verify ownership
        portfolioService.getPortfolio(portfolioId);

        return positionRepository.findByPortfolioId(portfolioId);
    }

    public Optional<Position> getPositionByPortfolioAndStock(Long portfolioId, String stockSymbol) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }

        // This will verify ownership
        portfolioService.getPortfolio(portfolioId);

        return positionRepository.findByPortfolioIdAndStockSymbol(portfolioId, stockSymbol);
    }

    public Position createPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (position.getPortfolioId() == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }

        // Verify user owns the portfolio
        portfolioService.getPortfolio(position.getPortfolioId());

        return positionRepository.save(position);
    }

    public Position updatePosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (position.getId() == null) {
            throw new IllegalArgumentException("Position ID cannot be null for updates");
        }

        verifyPositionOwnership(position.getId());

        return positionRepository.save(position);
    }

    public void deletePosition(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Position ID cannot be null");
        }

        verifyPositionOwnership(id);

        positionRepository.deleteById(id);
    }
}