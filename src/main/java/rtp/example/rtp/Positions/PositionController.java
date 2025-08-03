package rtp.example.rtp.Positions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rtp.example.rtp.PortfolioCalculationService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/positions")
public class PositionController {

    private final PositionService positionService;
    private final PortfolioCalculationService portfolioCalculationService;

    public PositionController(PositionService positionService,
                              PortfolioCalculationService portfolioCalculationService) {
        this.positionService = positionService;
        this.portfolioCalculationService = portfolioCalculationService;
    }

    @GetMapping
    public List<Position> getAllPositions() {
        return positionService.getAllPositions();
    }

    @GetMapping("/{id}")
    public Position getPosition(@PathVariable Long id) {
        return positionService.getPosition(id);
    }

    @GetMapping("/portfolio/{portfolioId}")
    public List<Position> getPositionsByPortfolio(@PathVariable Long portfolioId) {
        return positionService.getPositionsByPortfolio(portfolioId);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<PortfolioCalculationService.PositionSummary> getPositionSummary(@PathVariable Long id) {
        try {
            PortfolioCalculationService.PositionSummary summary = portfolioCalculationService.getPositionSummary(id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/portfolio/{portfolioId}/summaries")
    public ResponseEntity<List<PortfolioCalculationService.PositionSummary>> getPortfolioPositionSummaries(@PathVariable Long portfolioId) {
        try {
            List<Position> positions = positionService.getPositionsByPortfolio(portfolioId);
            List<PortfolioCalculationService.PositionSummary> summaries = positions.stream()
                    .map(position -> portfolioCalculationService.getPositionSummary(position.getId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/portfolio/{portfolioId}/top-performers")
    public ResponseEntity<List<PortfolioCalculationService.PositionSummary>> getTopPerformers(@PathVariable Long portfolioId,
                                                                                              @RequestParam(defaultValue = "5") int limit) {
        try {
            List<Position> positions = positionService.getPositionsByPortfolio(portfolioId);
            List<PortfolioCalculationService.PositionSummary> summaries = positions.stream()
                    .map(position -> portfolioCalculationService.getPositionSummary(position.getId()))
                    .sorted((s1, s2) -> s2.getPercentageReturn().compareTo(s1.getPercentageReturn()))
                    .limit(limit)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/portfolio/{portfolioId}/worst-performers")
    public ResponseEntity<List<PortfolioCalculationService.PositionSummary>> getWorstPerformers(@PathVariable Long portfolioId,
                                                                                                @RequestParam(defaultValue = "5") int limit) {
        try {
            List<Position> positions = positionService.getPositionsByPortfolio(portfolioId);
            List<PortfolioCalculationService.PositionSummary> summaries = positions.stream()
                    .map(position -> portfolioCalculationService.getPositionSummary(position.getId()))
                    .sorted((s1, s2) -> s1.getPercentageReturn().compareTo(s2.getPercentageReturn()))
                    .limit(limit)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/portfolio/{portfolioId}/allocation")
    public ResponseEntity<List<PositionAllocation>> getPositionAllocation(@PathVariable Long portfolioId) {
        try {
            List<Position> positions = positionService.getPositionsByPortfolio(portfolioId);
            java.math.BigDecimal totalPortfolioValue = positions.stream()
                    .map(position -> portfolioCalculationService.getPositionSummary(position.getId()))
                    .map(PortfolioCalculationService.PositionSummary::getCurrentMarketValue)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            List<PositionAllocation> allocations = positions.stream()
                    .map(position -> {
                        PortfolioCalculationService.PositionSummary summary = portfolioCalculationService.getPositionSummary(position.getId());
                        java.math.BigDecimal percentage = totalPortfolioValue.compareTo(java.math.BigDecimal.ZERO) == 0
                                ? java.math.BigDecimal.ZERO
                                : summary.getCurrentMarketValue()
                                .divide(totalPortfolioValue, 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new java.math.BigDecimal("100"));

                        return new PositionAllocation(
                                summary.getStockSymbol(),
                                summary.getCurrentMarketValue(),
                                percentage
                        );
                    })
                    .sorted((a1, a2) -> a2.getPercentage().compareTo(a1.getPercentage()))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(allocations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public Position createPosition(@RequestBody Position position) {
        return positionService.createPosition(position);
    }

    @PutMapping("/{id}")
    public Position updatePosition(@PathVariable Long id, @RequestBody Position position) {
        position.setId(id);
        return positionService.updatePosition(position);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePosition(@PathVariable Long id) {
        try {
            positionService.deletePosition(id);
            return ResponseEntity.ok("Position deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting position: " + e.getMessage());
        }
    }

    // Allocation DTO
    public static class PositionAllocation {
        private final String stockSymbol;
        private final java.math.BigDecimal value;
        private final java.math.BigDecimal percentage;

        public PositionAllocation(String stockSymbol, java.math.BigDecimal value, java.math.BigDecimal percentage) {
            this.stockSymbol = stockSymbol;
            this.value = value;
            this.percentage = percentage;
        }

        public String getStockSymbol() { return stockSymbol; }
        public java.math.BigDecimal getValue() { return value; }
        public java.math.BigDecimal getPercentage() { return percentage; }
    }
}