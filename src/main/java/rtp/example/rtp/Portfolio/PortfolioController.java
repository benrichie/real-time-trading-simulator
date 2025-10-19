package rtp.example.rtp.Portfolio;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rtp.example.rtp.PortfolioCalculationService;
import rtp.example.rtp.User.User;

import java.util.List;

@RestController
@RequestMapping("api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioCalculationService portfolioCalculationService;

    public PortfolioController(PortfolioService portfolioService,
                               PortfolioCalculationService portfolioCalculationService) {
        this.portfolioService = portfolioService;
        this.portfolioCalculationService = portfolioCalculationService;
    }

    @GetMapping
    public List<Portfolio> getAllPortfolios(@AuthenticationPrincipal User user) {
        // Now returns only current user's portfolios
        return portfolioService.getAllPortfolios();
    }

    @GetMapping("/{id}")
    public Portfolio getPortfolio(@PathVariable Long id) {
        // Service layer checks ownership
        return portfolioService.getPortfolio(id);
    }

    @GetMapping("/me")
    public Portfolio getMyPortfolio(@AuthenticationPrincipal User user) {
        // New endpoint - get current user's portfolio
        return portfolioService.getPortfolioByUserId(user.getId());
    }

    @PostMapping("/me")
    public Portfolio createMyPortfolio(@AuthenticationPrincipal User user) {
        // Create portfolio for current user only
        return portfolioService.createPortfolio(user.getId());
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<PortfolioCalculationService.PortfolioSummary> getPortfolioSummary(@PathVariable Long id) {
        // Service layer checks ownership
        portfolioService.getPortfolio(id); // Verify access first
        PortfolioCalculationService.PortfolioSummary summary = portfolioCalculationService.getPortfolioSummary(id);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/{id}/recalculate")
    public ResponseEntity<String> recalculatePortfolio(@PathVariable Long id) {
        portfolioService.getPortfolio(id); // Verify access first
        portfolioCalculationService.recalculatePortfolio(id);
        return ResponseEntity.ok("Portfolio recalculated successfully");
    }

    @GetMapping("/{id}/performance")
    public ResponseEntity<PortfolioPerformance> getPortfolioPerformance(@PathVariable Long id) {
        portfolioService.getPortfolio(id); // Verify access first
        PortfolioCalculationService.PortfolioSummary summary = portfolioCalculationService.getPortfolioSummary(id);
        Portfolio portfolio = portfolioService.getPortfolio(id);

        PortfolioPerformance performance = new PortfolioPerformance(
                summary.getPortfolioId(),
                summary.getTotalPortfolioValue(),
                summary.getUnrealizedPnL(),
                summary.getPercentageReturn(),
                summary.getCashBalance(),
                summary.getTotalPositionsValue(),
                summary.getTotalCostBasis(),
                calculateCashPercentage(summary.getCashBalance(), summary.getTotalPortfolioValue()),
                calculatePositionsPercentage(summary.getTotalPositionsValue(), summary.getTotalPortfolioValue())
        );

        return ResponseEntity.ok(performance);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.ok("Portfolio deleted successfully");
    }

    private java.math.BigDecimal calculateCashPercentage(java.math.BigDecimal cashBalance, java.math.BigDecimal totalValue) {
        if (totalValue.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return java.math.BigDecimal.ZERO;
        }
        return cashBalance.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"));
    }

    private java.math.BigDecimal calculatePositionsPercentage(java.math.BigDecimal positionsValue, java.math.BigDecimal totalValue) {
        if (totalValue.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return java.math.BigDecimal.ZERO;
        }
        return positionsValue.divide(totalValue, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new java.math.BigDecimal("100"));
    }

    public static class PortfolioPerformance {
        private final Long portfolioId;
        private final java.math.BigDecimal totalValue;
        private final java.math.BigDecimal totalPnL;
        private final java.math.BigDecimal percentageReturn;
        private final java.math.BigDecimal cashBalance;
        private final java.math.BigDecimal positionsValue;
        private final java.math.BigDecimal totalCostBasis;
        private final java.math.BigDecimal cashPercentage;
        private final java.math.BigDecimal positionsPercentage;

        public PortfolioPerformance(Long portfolioId, java.math.BigDecimal totalValue,
                                    java.math.BigDecimal totalPnL, java.math.BigDecimal percentageReturn,
                                    java.math.BigDecimal cashBalance, java.math.BigDecimal positionsValue,
                                    java.math.BigDecimal totalCostBasis, java.math.BigDecimal cashPercentage,
                                    java.math.BigDecimal positionsPercentage) {
            this.portfolioId = portfolioId;
            this.totalValue = totalValue;
            this.totalPnL = totalPnL;
            this.percentageReturn = percentageReturn;
            this.cashBalance = cashBalance;
            this.positionsValue = positionsValue;
            this.totalCostBasis = totalCostBasis;
            this.cashPercentage = cashPercentage;
            this.positionsPercentage = positionsPercentage;
        }

        // Getters
        public Long getPortfolioId() { return portfolioId; }
        public java.math.BigDecimal getTotalValue() { return totalValue; }
        public java.math.BigDecimal getTotalPnL() { return totalPnL; }
        public java.math.BigDecimal getPercentageReturn() { return percentageReturn; }
        public java.math.BigDecimal getCashBalance() { return cashBalance; }
        public java.math.BigDecimal getPositionsValue() { return positionsValue; }
        public java.math.BigDecimal getTotalCostBasis() { return totalCostBasis; }
        public java.math.BigDecimal getCashPercentage() { return cashPercentage; }
        public java.math.BigDecimal getPositionsPercentage() { return positionsPercentage; }
    }
}