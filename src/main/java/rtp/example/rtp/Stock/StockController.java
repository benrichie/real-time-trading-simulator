package rtp.example.rtp.Stock;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService){
        this.stockService = stockService;
    }

    @GetMapping
    public List<Stock> getAllStocks(){
        return stockService.getAllStocks();
    }

    @GetMapping("/{symbol}")
    public Stock getStock(@PathVariable String symbol){
        return stockService.getStock(symbol);
    }

    @PostMapping
    public Stock createStock(@RequestBody Stock stock) {
        return stockService.saveStock(stock);
    }

    @PutMapping("/{symbol}")
    public Stock updateStock(@PathVariable String symbol, @RequestBody Stock stock){
        stock.setSymbol(symbol);
        return stockService.saveStock(stock);
    }

    @DeleteMapping("/{symbol}")
    public void deleteStock(@PathVariable String symbol){
        stockService.deleteStock(symbol);
    }


}
