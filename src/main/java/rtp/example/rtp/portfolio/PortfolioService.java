package rtp.example.rtp.portfolio;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import rtp.example.rtp.user.User;
import rtp.example.rtp.user.UserRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    public PortfolioService(PortfolioRepository portfolioRepository, UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user.getId();
    }

    private void verifyPortfolioOwnership(Long portfolioId) {
        Portfolio portfolio = getPortfolio(portfolioId);
        Long currentUserId = getCurrentUserId();
        if (!portfolio.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("Access denied: You don't own this portfolio");
        }
    }

    public List<Portfolio> getAllPortfolios() {
        // Only return portfolios for current user
        Long currentUserId = getCurrentUserId();
        return portfolioRepository.findByUserId(currentUserId).stream().toList();
    }

    public Portfolio getPortfolio(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found with ID: " + id));

        // Verify ownership
        Long currentUserId = getCurrentUserId();
        if (!portfolio.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("Access denied: You don't own this portfolio");
        }

        return portfolio;
    }

    public Portfolio getPortfolioByUserId(Long userId) {
        // Users can only get their own portfolio
        Long currentUserId = getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            throw new IllegalArgumentException("Access denied: You can only access your own portfolio");
        }

        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Portfolio not found for user ID: " + userId));
    }

    public Portfolio createPortfolio(Long userId) {
        // Users can only create portfolio for themselves
        Long currentUserId = getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            throw new IllegalArgumentException("Access denied: You can only create a portfolio for yourself");
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        if (portfolioRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("Portfolio already exists for user ID: " + userId);
        }

        Portfolio portfolio = new Portfolio(userId, user.getInitialBalance());
        return portfolioRepository.save(portfolio);
    }

    public Portfolio updatePortfolio(Portfolio portfolio) {
        if (portfolio == null) {
            throw new IllegalArgumentException("Portfolio cannot be null");
        }
        if (portfolio.getId() == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null for updates");
        }

        verifyPortfolioOwnership(portfolio.getId());

        if (!portfolioRepository.existsById(portfolio.getId())) {
            throw new EntityNotFoundException("Portfolio not found with ID: " + portfolio.getId());
        }

        return portfolioRepository.save(portfolio);
    }

    public void updateCashBalance(Long portfolioId, BigDecimal newBalance) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        if (newBalance == null) {
            throw new IllegalArgumentException("New balance cannot be null");
        }
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cash balance cannot be negative");
        }

        verifyPortfolioOwnership(portfolioId);

        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.setCashBalance(newBalance);
        updatePortfolio(portfolio);
    }

    public void updateTotalValue(Long portfolioId, BigDecimal newTotalValue) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }
        if (newTotalValue == null) {
            throw new IllegalArgumentException("New total value cannot be null");
        }

        verifyPortfolioOwnership(portfolioId);

        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.setTotalValue(newTotalValue);
        updatePortfolio(portfolio);
    }

    public void deletePortfolio(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Portfolio ID cannot be null");
        }

        verifyPortfolioOwnership(id);

        if (!portfolioRepository.existsById(id)) {
            throw new EntityNotFoundException("Portfolio not found with ID: " + id);
        }
        portfolioRepository.deleteById(id);
    }
}