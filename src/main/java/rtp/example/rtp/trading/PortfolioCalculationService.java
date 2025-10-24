package rtp.example.rtp.trading;

import org.springframework.stereotype.Service;
import rtp.example.rtp.portfolio.Portfolio;
import rtp.example.rtp.portfolio.PortfolioService;
import rtp.example.rtp.positions.Position;
import rtp.example.rtp.positions.PositionService;
import rtp.example.rtp.stock.Stock;
import rtp.example.rtp.stock.StockService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PortfolioCalculationService {

    private final PortfolioService portfolioService;
    private final PositionService positionService;
    private final StockService stockService;

    public PortfolioCalculationService(PortfolioService portfolioService,
                                       PositionService positionService,
                                       StockService stockService) {
        this.portfolioService = portfolioService;
        this.positionService = positionService;
        this.stockService = stockService;
    }


    public void recalculatePortfolio(Long portfolioId) {
        // getPortfolio already verifies ownership
        Portfolio portfolio = portfolioService.getPortfolio(portfolioId);

        // getPositionsByPortfolio already verifies ownership through portfolioService
        List<Position> positions = positionService.getPositionsByPortfolio(portfolioId);

        // Calculate total market value of all positions
        BigDecimal totalPositionsValue = calculateTotalPositionsValue(positions);

        // Update portfolio total value (cash + positions)
        BigDecimal totalPortfolioValue = portfolio.getCashBalance().add(totalPositionsValue);
        portfolio.setTotalValue(totalPortfolioValue);

        // updatePortfolio already verifies ownership
        portfolioService.updatePortfolio(portfolio);

        // Update individual position current values
        updatePositionCurrentValues(positions);
    }

    public PortfolioSummary getPortfolioSummary(Long portfolioId) {
        // Ownership verification happens in getPortfolio
        Portfolio portfolio = portfolioService.getPortfolio(portfolioId);
        List<Position> positions = positionService.getPositionsByPortfolio(portfolioId);

        BigDecimal totalPositionsValue = calculateTotalPositionsValue(positions);
        BigDecimal totalCostBasis = calculateTotalCostBasis(positions);
        BigDecimal unrealizedPnL = totalPositionsValue.subtract(totalCostBasis);
        BigDecimal totalPortfolioValue = portfolio.getCashBalance().add(totalPositionsValue);

        return new PortfolioSummary(
                portfolioId,
                portfolio.getCashBalance(),
                totalPositionsValue,
                totalPortfolioValue,
                totalCostBasis,
                unrealizedPnL,
                calculatePortfolioPercentageReturn(totalPortfolioValue, getTotalInvestedAmount(portfolio, totalCostBasis))
        );
    }

    public PositionSummary getPositionSummary(Long positionId) {
        // Ownership verification happens in getPosition
        Position position = positionService.getPosition(positionId);
        Stock stock = stockService.getStock(position.getStockSymbol());

        BigDecimal currentMarketValue = stock.getCurrentPrice().multiply(new BigDecimal(position.getQuantity()));
        BigDecimal costBasis = position.getAveragePrice().multiply(new BigDecimal(position.getQuantity()));
        BigDecimal unrealizedPnL = currentMarketValue.subtract(costBasis);
        BigDecimal percentageReturn = calculatePercentageReturn(currentMarketValue, costBasis);

        return new PositionSummary(
                position.getId(),
                position.getStockSymbol(),
                position.getQuantity(),
                position.getAveragePrice(),
                stock.getCurrentPrice(),
                costBasis,
                currentMarketValue,
                unrealizedPnL,
                percentageReturn
        );
    }

    private BigDecimal calculateTotalPositionsValue(List<Position> positions) {
        BigDecimal total = BigDecimal.ZERO;

        for (Position position : positions) {
            try {
                Stock stock = stockService.getStock(position.getStockSymbol());
                BigDecimal positionValue = stock.getCurrentPrice().multiply(new BigDecimal(position.getQuantity()));
                total = total.add(positionValue);
            } catch (Exception e) {
                // If stock not found, use the stored current value
                total = total.add(position.getCurrentValue());
            }
        }

        return total;
    }

    private BigDecimal calculateTotalCostBasis(List<Position> positions) {
        return positions.stream()
                .map(position -> position.getAveragePrice().multiply(new BigDecimal(position.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updatePositionCurrentValues(List<Position> positions) {
        for (Position position : positions) {
            try {
                Stock stock = stockService.getStock(position.getStockSymbol());
                BigDecimal currentValue = stock.getCurrentPrice().multiply(new BigDecimal(position.getQuantity()));
                position.setCurrentValue(currentValue);
                // updatePosition already verifies ownership
                positionService.updatePosition(position);
            } catch (Exception e) {
                // If stock price cannot be fetched, keep existing current value
                System.err.println("Could not update position value for " + position.getStockSymbol());
            }
        }
    }

    private BigDecimal calculatePercentageReturn(BigDecimal currentValue, BigDecimal costBasis) {
        if (costBasis.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentValue.subtract(costBasis)
                .divide(costBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculatePortfolioPercentageReturn(BigDecimal currentValue, BigDecimal initialValue) {
        if (initialValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentValue.subtract(initialValue)
                .divide(initialValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal getTotalInvestedAmount(Portfolio portfolio, BigDecimal totalCostBasis) {
        BigDecimal initialCapital = portfolio.getInitialCapital();
        if(initialCapital == null) {
            initialCapital = BigDecimal.ZERO;
        }
        return initialCapital.add(totalCostBasis);
    }

    // Summary DTOs
    public static class PortfolioSummary {
        private final Long portfolioId;
        private final BigDecimal cashBalance;
        private final BigDecimal totalPositionsValue;
        private final BigDecimal totalPortfolioValue;
        private final BigDecimal totalCostBasis;
        private final BigDecimal unrealizedPnL;
        private final BigDecimal percentageReturn;

        public PortfolioSummary(Long portfolioId, BigDecimal cashBalance, BigDecimal totalPositionsValue,
                                BigDecimal totalPortfolioValue, BigDecimal totalCostBasis,
                                BigDecimal unrealizedPnL, BigDecimal percentageReturn) {
            this.portfolioId = portfolioId;
            this.cashBalance = cashBalance;
            this.totalPositionsValue = totalPositionsValue;
            this.totalPortfolioValue = totalPortfolioValue;
            this.totalCostBasis = totalCostBasis;
            this.unrealizedPnL = unrealizedPnL;
            this.percentageReturn = percentageReturn;
        }

        // Getters
        public Long getPortfolioId() { return portfolioId; }
        public BigDecimal getCashBalance() { return cashBalance; }
        public BigDecimal getTotalPositionsValue() { return totalPositionsValue; }
        public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
        public BigDecimal getTotalCostBasis() { return totalCostBasis; }
        public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public BigDecimal getPercentageReturn() { return percentageReturn; }
    }

    public static class PositionSummary {
        private final Long positionId;
        private final String stockSymbol;
        private final Integer quantity;
        private final BigDecimal averagePrice;
        private final BigDecimal currentPrice;
        private final BigDecimal costBasis;
        private final BigDecimal currentMarketValue;
        private final BigDecimal unrealizedPnL;
        private final BigDecimal percentageReturn;

        public PositionSummary(Long positionId, String stockSymbol, Integer quantity,
                               BigDecimal averagePrice, BigDecimal currentPrice, BigDecimal costBasis,
                               BigDecimal currentMarketValue, BigDecimal unrealizedPnL, BigDecimal percentageReturn) {
            this.positionId = positionId;
            this.stockSymbol = stockSymbol;
            this.quantity = quantity;
            this.averagePrice = averagePrice;
            this.currentPrice = currentPrice;
            this.costBasis = costBasis;
            this.currentMarketValue = currentMarketValue;
            this.unrealizedPnL = unrealizedPnL;
            this.percentageReturn = percentageReturn;
        }

        // Getters
        public Long getPositionId() { return positionId; }
        public String getStockSymbol() { return stockSymbol; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getAveragePrice() { return averagePrice; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public BigDecimal getCostBasis() { return costBasis; }
        public BigDecimal getCurrentMarketValue() { return currentMarketValue; }
        public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public BigDecimal getPercentageReturn() { return percentageReturn; }
    }
}