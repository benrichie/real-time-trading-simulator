package rtp.example.rtp.Portfolio;

import org.springframework.stereotype.Service;
import rtp.example.rtp.User.User;
import rtp.example.rtp.User.UserRepository;

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

    // public List<Portfolio> getAllPortfolios() {
    //    return portfolioRepository.findAll();
    //}

    public Portfolio getPortfolio(Long id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portfolio not found: " + id));
    }

    public Portfolio getPortfolioByUserId(Long userId) {
        return portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Portfolio not found for user: " + userId));
    }

    public Portfolio createPortfolio(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Portfolio portfolio = new Portfolio(userId, user.getInitialBalance());
        return portfolioRepository.save(portfolio);
    }

    public Portfolio updatePortfolio(Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }

    public void updateCashBalance(Long portfolioId, BigDecimal newBalance) {
        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.setCashBalance(newBalance);
        updatePortfolio(portfolio);
    }

    public void updateTotalValue(Long portfolioId, BigDecimal newTotalValue) {
        Portfolio portfolio = getPortfolio(portfolioId);
        portfolio.setTotalValue(newTotalValue);
        updatePortfolio(portfolio);
    }

    public void deletePortfolio(Long id) {
        portfolioRepository.deleteById(id);
    }
}