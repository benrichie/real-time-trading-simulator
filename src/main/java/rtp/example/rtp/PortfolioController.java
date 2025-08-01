package rtp.example.rtp;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<Portfolio> getAllPortfolios(){
        return portfolioService.getAllPortfolios();
    }

    @GetMapping("/{id}")
    public Portfolio getPortfolio(@PathVariable Long id){
        return portfolioService.getPortfolio(id);
    }

    @GetMapping("/user/{userId}")
    public Portfolio getPortfolioByUserId(@PathVariable Long userId){
        return portfolioService.getPortfolioByUserId(userId);
    }

    @PostMapping("/user/{userId}")
    public Portfolio createPortfolioForUser(@PathVariable Long userId){
        return portfolioService.createPortfolio(userId);
    }

    @DeleteMapping("/{id}")
    public void deletePortfolio(@PathVariable Long id) {
        portfolioService.deletePortfolio(id);
    }




}
