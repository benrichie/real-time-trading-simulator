package rtp.example.rtp.Stock;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    public Stock getStock(String symbol) {
        return stockRepository.findById(symbol)
                .orElseThrow(() -> new RuntimeException("Stock not found: "+ symbol));
    }

    public Stock saveStock(Stock stock) {
        return stockRepository.save(stock);
    }

    public void deleteStock(String symbol) { stockRepository.deleteById(symbol); }

    public void updateStock(Stock stock){
        if(!stockRepository.existsById(stock.getSymbol())){
            throw new RuntimeException("Stock not found: " + stock.getSymbol());
        }
        stockRepository.save(stock);
    }


    public boolean existsBySymbol(String normalized) {
        return stockRepository.existsById(normalized);
    }
}
